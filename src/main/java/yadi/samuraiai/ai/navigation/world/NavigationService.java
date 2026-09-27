package yadi.samuraiai.ai.navigation.world;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import yadi.samuraiai.ai.navigation.cache.PathCache;
import yadi.samuraiai.ai.navigation.engine.*;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.graph.NavigationGraph;
import yadi.samuraiai.ai.navigation.metrics.NavigationMetrics;
import yadi.samuraiai.ai.navigation.movement.MovementBody;
import yadi.samuraiai.ai.navigation.planner.NavigationProfiles;
import yadi.samuraiai.ai.navigation.planner.PathPreferences;
import yadi.samuraiai.ai.navigation.planner.PathRequest;
import yadi.samuraiai.ai.navigation.terrain.TerrainCostTable;
import yadi.samuraiai.ai.navigation.terrain.TerrainScanner;
import yadi.samuraiai.ai.navigation.zones.DangerMap;
import yadi.samuraiai.ai.navigation.zones.DangerSource;
import yadi.samuraiai.ai.navigation.zones.DangerZone;
import yadi.samuraiai.ai.navigation.zones.HazardType;
import yadi.samuraiai.logging.SamuraiLogger;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.runtime.ServerScheduler;
import yadi.samuraiai.world.ServerWorlds;
import yadi.samuraiai.world.SpawnLocation;

/**
 * Minecraft-facing front of the navigation engine. Owns one engine + runtime per dimension (created on first
 * use), resolves NPC bodies through their controller, drives every runtime once per server tick and turns
 * world events (block changes, unloading chunks, explosions) into cache invalidations. Behaviors talk to this
 * class and nothing else. Server-thread only.
 */
public final class NavigationService {
    private static final NavigationService INSTANCE = new NavigationService();
    public static NavigationService getInstance() { return INSTANCE; }

    /** Everything that exists once per dimension. */
    public record Dimension(String key, ServerLevel level, MinecraftNavWorldView view, NavigationGraph graph, DangerMap dangers,
                            PathCache cache, NavigationEngine engine, NavigationRuntime runtime, TerrainScanner scanner) { }

    private static final int DEBUG_INTERVAL_TICKS = 10;
    private final Map<String, Dimension> dimensions = new HashMap<>();
    private final NavigationMetrics metrics = new NavigationMetrics();
    private final TerrainCostTable costs = new TerrainCostTable();
    private final Set<UUID> debugViewers = new HashSet<>();
    private final Map<String, DangerSource> globalDangerSources = new java.util.LinkedHashMap<>();
    private EventSink events = EventSink.eventBus();
    private final java.util.Map<UUID, java.util.function.UnaryOperator<PathPreferences>> adjusters = new java.util.HashMap<>();
    private long tick;

    private NavigationService() { }

    public boolean enabled() { return NavigationConfig.enabled() && ServerScheduler.getInstance().isRunning(); }
    public NavigationMetrics metrics() { return metrics; }
    public long currentTick() { return tick; }
    public Collection<Dimension> dimensions() { return List.copyOf(dimensions.values()); }
    /** Replaces where navigation events go; tests redirect them, production keeps the EventBus. */
    public void useEventSink(EventSink sink) { events = Objects.requireNonNull(sink); }

    /** Lets other engines (perception threats, world events) contribute hazards to every dimension's danger map. */
    public void registerDangerSource(String id, DangerSource source) {
        globalDangerSources.put(Objects.requireNonNull(id), Objects.requireNonNull(source));
        dimensions.values().forEach(d -> d.dangers().registerSource(id, source));
    }

    // ------------------------------------------------------------------ requests

    /** Starts (or replaces) the journey of an NPC. Never throws: an impossible request returns a FAILED handle. */
    public NavigationHandle navigate(NPCRuntime npc, SpawnLocation goal, NavigationOptions options) {
        ServerScheduler.getInstance().requireServerThread();
        NavPos destination = NavPos.ofBlock(goal.x(), goal.y(), goal.z());
        Optional<MovementBody> body = npc.getController().movementBody(npc.getInstance());
        if (body.isEmpty()) return new FailedNavigation(npc.getId(), NavigationFailure.NO_BODY, "controller has no movement body", destination);
        Optional<ServerLevel> level = ServerWorlds.level(goal.dimensionKey());
        if (level.isEmpty()) return new FailedNavigation(npc.getId(), NavigationFailure.DESTINATION_INVALID, "dimension " + goal.dimensionKey() + " not loaded", destination);
        Dimension dimension = dimension(level.get());
        MovementBody actuator = body.get();
        if (actuator instanceof MobMovementBody mob && !mob.dimension().equals(dimension.key()))
            return new FailedNavigation(npc.getId(), NavigationFailure.DIMENSION_CHANGED, "npc is in " + mob.dimension(), destination);
        PathPreferences preferences = NavigationProfiles.forType(npc.getInstance().getIdentity().type().value()).withMode(options.mode())
                .withBodyRadius(actuator.width() / 2.0D + 0.02D);
        var adjuster = adjusters.get(npc.getId());
        if (adjuster != null) preferences = adjuster.apply(preferences);
        PathRequest request = new PathRequest(npc.getId(), dimension.key(), actuator.block(), destination, preferences,
                options.behavior(), options.goal(), options.arrivalRadius(), options.timeoutTicks(), options.allowPartial());
        return dimension.runtime().request(request, tick);
    }

    public void cancel(UUID npcId, String reason) { dimensions.values().forEach(d -> d.runtime().cancel(npcId, reason)); }
    public void forget(UUID npcId) { adjusters.remove(npcId); dimensions.values().forEach(d -> d.runtime().forget(npcId)); }

    /** Lets another engine (the scheduler's personality) tune this NPC's routing preferences; null removes the adjustment. */
    public void setPreferenceAdjuster(UUID npcId, java.util.function.UnaryOperator<PathPreferences> adjuster) {
        if (adjuster == null) adjusters.remove(npcId); else adjusters.put(npcId, adjuster);
    }

    public Optional<NavigationSession> session(UUID npcId) {
        for (Dimension d : dimensions.values()) {
            Optional<NavigationSession> found = d.runtime().session(npcId);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------ per-tick driving

    public void tick() {
        if (!ServerScheduler.getInstance().isRunning()) return;
        tick++;
        for (Dimension d : dimensions.values()) {
            try {
                d.runtime().tick(tick);
                if (tick % 100 == 0) { d.cache().purgeExpired(tick); d.dangers().prune(tick); }
                if (tick % 40 == 0) d.dangers().refresh(d.key(), tick);
            } catch (RuntimeException error) {
                SamuraiLogger.CORE.error("Navigation tick failed in {}", d.key(), error);
            }
        }
        if (!debugViewers.isEmpty() && tick % DEBUG_INTERVAL_TICKS == 0) NavigationDebugRenderer.render(this, debugViewers);
    }

    /** Re-collects every danger source now instead of waiting for the periodic refresh (e.g. after a source NPC was removed). */
    public void refreshDanger() {
        for (Dimension d : dimensions.values()) d.dangers().refresh(d.key(), tick);
    }

    // ------------------------------------------------------------------ world events

    public void worldChanged(ServerLevel level, BlockPos pos) {
        Dimension d = dimensions.get(level.dimension().location().toString());
        if (d != null) d.runtime().worldChanged(new NavPos(pos.getX(), pos.getY(), pos.getZ()));
    }

    public void chunkChanged(ServerLevel level, int chunkX, int chunkZ) {
        Dimension d = dimensions.get(level.dimension().location().toString());
        if (d != null) d.runtime().chunkChanged(chunkX, chunkZ);
    }

    public void explosion(ServerLevel level, double x, double y, double z, List<BlockPos> affected) {
        Dimension d = dimensions.get(level.dimension().location().toString());
        if (d == null) return;
        d.dangers().add(new DangerZone(d.key(), NavPos.ofBlock(x, y, z), 7.0D, 70.0D, tick + 100, HazardType.EXPLOSION, "explosion"));
        for (BlockPos pos : affected) d.runtime().worldChanged(new NavPos(pos.getX(), pos.getY(), pos.getZ()));
    }

    /** Configuration was reloaded: rebuild cost tables and drop paths planned under the old costs. */
    public void configReloaded() {
        costs.reset().applyOverrides(NavigationSettings.current().costOverrides());
        for (Dimension d : dimensions.values()) { d.cache().clear(); d.scanner().invalidate(); }
    }

    // ------------------------------------------------------------------ debug

    public boolean toggleDebug(UUID player) { return debugViewers.add(player) || !debugViewers.remove(player); }
    public boolean debugging(UUID player) { return debugViewers.contains(player); }

    // ------------------------------------------------------------------ lifecycle

    /** Called when the server session ends so no world object or path survives into the next one. */
    public void reset() {
        for (Dimension d : dimensions.values()) d.runtime().reset();
        dimensions.clear();
        adjusters.clear();
        debugViewers.clear();
        metrics.reset();
        tick = 0;
    }

    private Dimension dimension(ServerLevel level) {
        String key = level.dimension().location().toString();
        Dimension known = dimensions.get(key);
        if (known != null && known.level() == level) return known;
        if (dimensions.isEmpty()) costs.reset().applyOverrides(NavigationSettings.current().costOverrides());
        NavigationSettings cfg = NavigationSettings.current();
        MinecraftNavWorldView view = new MinecraftNavWorldView(level);
        NavigationGraph graph = new NavigationGraph(view, cfg.dangerCap(), cfg.nodeCacheMax(), null);
        DangerMap dangers = new DangerMap();
        globalDangerSources.forEach(dangers::registerSource);
        PathCache cache = new PathCache(cfg.cacheCapacity(), cfg.cacheTtlTicks());
        MinecraftEntityObstacleSource entities = new MinecraftEntityObstacleSource(level, this::npcEntityIds);
        NavigationEngine engine = new NavigationEngine(graph, dangers, cache, costs, NavigationSettings::current, metrics, events, entities);
        NavigationRuntime runtime = new NavigationRuntime(engine, id -> bodyFor(id, key), id -> nearestPlayerDistance(level, id), NavigationSettings::current);
        Dimension created = new Dimension(key, level, view, graph, dangers, cache, engine, runtime, new TerrainScanner(graph, 100));
        dimensions.put(key, created);
        SamuraiLogger.CORE.info("Navigation engine created for dimension {}", key);
        return created;
    }

    private Set<UUID> npcEntityIds() {
        Set<UUID> ids = new HashSet<>();
        for (NPCRuntime npc : NPCManager.getInstance().getActive()) ids.addAll(npc.getController().ownedEntityIds());
        return ids;
    }

    private MovementBody bodyFor(UUID npcId, String dimensionKey) {
        return NPCManager.getInstance().find(npcId).filter(NPCRuntime::isActive)
                .flatMap(npc -> npc.getController().movementBody(npc.getInstance()))
                .filter(body -> !(body instanceof MobMovementBody mob) || mob.dimension().equals(dimensionKey))
                .orElse(null);
    }

    private double nearestPlayerDistance(ServerLevel level, UUID npcId) {
        MovementBody body = bodyFor(npcId, level.dimension().location().toString());
        if (body == null) return 0.0D;
        Player nearest = level.getNearestPlayer(body.x(), body.y(), body.z(), -1.0D, false);
        return nearest == null ? Double.MAX_VALUE : Math.sqrt(nearest.distanceToSqr(body.x(), body.y(), body.z()));
    }

    /** Debug/command helper: the viewer's player entity, if online. */
    static Optional<ServerPlayer> player(UUID id) { return ServerWorlds.playerById(id); }
}
