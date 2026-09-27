package yadi.samuraiai.ai.perception.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.zones.DangerZone;
import yadi.samuraiai.ai.navigation.zones.HazardType;
import yadi.samuraiai.ai.navigation.movement.MovementBody;
import yadi.samuraiai.ai.perception.engine.PerceptionEngine;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;
import yadi.samuraiai.ai.perception.engine.PerceptionSnapshot;
import yadi.samuraiai.ai.perception.engine.PerceptionState;
import yadi.samuraiai.ai.perception.engine.Perceiver;
import yadi.samuraiai.ai.perception.engine.Reports;
import yadi.samuraiai.ai.perception.engine.SenseProfile;
import yadi.samuraiai.ai.perception.hearing.SoundCategory;
import yadi.samuraiai.ai.perception.hearing.SoundEvent;
import yadi.samuraiai.ai.perception.hearing.SoundLog;
import yadi.samuraiai.ai.perception.metrics.PerceptionMetrics;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.vision.RayBudget;
import yadi.samuraiai.emotion.Emotion;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.event.NPCEventBus;
import yadi.samuraiai.event.PlayerMessageEvent;
import yadi.samuraiai.logging.SamuraiLogger;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.npc.relationship.RelationshipService;
import yadi.samuraiai.runtime.ServerScheduler;
import yadi.samuraiai.world.ServerWorlds;

/**
 * Minecraft-facing front of the perception engine. Once per server tick it runs each active NPC's pass when due (near NPCs
 * often, far ones rarely, alert ones faster), under shared budgets for NPCs and raycasts, and publishes the resulting
 * events. It turns world events (sounds, damage, chat, block changes) into inputs for the NPCs they concern, and exposes each
 * NPC's latest {@link PerceptionSnapshot}. It never moves an NPC or starts a behavior. Server-thread only.
 */
public final class PerceptionService {
    private static final PerceptionService INSTANCE = new PerceptionService();
    public static PerceptionService getInstance() { return INSTANCE; }

    /** Per-dimension world reader and sound log. */
    public record Dimension(String key, ServerLevel level, MinecraftPerceptionWorld world, SoundLog sounds) { }

    private static final int TIER_REFRESH_TICKS = 40;
    private final Map<UUID, PerceptionState> states = new HashMap<>();
    private final Map<UUID, int[]> tiers = new HashMap<>();
    private final Map<String, Dimension> dimensions = new HashMap<>();
    private final PerceptionMetrics metrics = new PerceptionMetrics();
    private final PerceptionEngine engine = new PerceptionEngine(PerceptionSettings::current, metrics);
    private final Set<UUID> debugViewers = new HashSet<>();
    private final Map<UUID, SenseProfile> senseProfiles = new HashMap<>();
    private EventSink events = EventSink.eventBus();
    private Set<UUID> npcEntityCache = Set.of();
    private long npcEntityCacheTick = -1;
    private long tick;
    private int cursor;

    private PerceptionService() { }

    public boolean enabled() { return PerceptionConfig.enabled() && ServerScheduler.getInstance().isRunning(); }
    public PerceptionMetrics metrics() { return metrics; }
    public PerceptionEngine engine() { return engine; }
    public long currentTick() { return tick; }
    public void useEventSink(EventSink sink) { events = Objects.requireNonNull(sink); }
    public List<Dimension> dimensions() { return List.copyOf(dimensions.values()); }
    public List<UUID> trackedIds() { return List.copyOf(states.keySet()); }
    public Optional<PerceptionState> state(UUID npcId) { return Optional.ofNullable(states.get(npcId)); }
    public Optional<PerceptionSnapshot> snapshot(UUID npcId) { return Optional.ofNullable(states.get(npcId)).map(s -> s.snapshot); }

    /** Wires the service to the internal event bus and to navigation; called once when a server session starts. */
    public void install() {
        NPCEventBus.getInstance().subscribe(PlayerMessageEvent.class, event -> {
            var player = ServerWorlds.playerById(event.playerId());
            if (player.isEmpty()) return;
            conversation(event.npcId(), event.playerId(), event.playerName(), player.get().getX(), player.get().getY(), player.get().getZ(), event.message().length());
        });
        yadi.samuraiai.ai.navigation.world.NavigationService.getInstance().registerDangerSource("perception", this::collectThreats);
    }

    // ------------------------------------------------------------------ per-tick driving

    public void tick() {
        if (!ServerScheduler.getInstance().isRunning()) return;
        long started = System.nanoTime();
        tick++;
        PerceptionSettings s = PerceptionSettings.current();
        List<NPCRuntime> active = new ArrayList<>(NPCManager.getInstance().getActive());
        int n = active.size(), processed = 0, deferred = 0;
        RayBudget rays = new RayBudget(s.maxRaycastsPerTick());
        int offset = n == 0 ? 0 : cursor % n;
        for (int i = 0; i < n; i++) {
            NPCRuntime npc = active.get((offset + i) % n);
            if (!npc.isActive()) continue;
            try {
                if (!due(npc, s)) continue;
                if (processed >= s.maxNpcsPerTick() || (rays.remaining() <= 0 && !forced(npc))) { deferred++; continue; }
                if (pass(npc, s, rays)) processed++;
            } catch (RuntimeException error) {
                SamuraiLogger.PERCEPTION.error("Perception failed for npc {}", npc.getId(), error);
            }
        }
        cursor = n == 0 ? 0 : (offset + Math.max(1, processed)) % n;
        if (tick % 20 == 0) dimensions.values().forEach(d -> d.sounds().prune(tick, s.soundLogTicks()));
        if (tick % 100 == 0) {
            Set<UUID> alive = new HashSet<>();
            active.forEach(npc -> alive.add(npc.getId()));
            states.keySet().removeIf(id -> !alive.contains(id));
            tiers.keySet().removeIf(id -> !alive.contains(id));
        }
        metrics.deferred(deferred);
        metrics.trackedNpcs(states.size());
        metrics.tick(System.nanoTime() - started);
        if (!debugViewers.isEmpty() && tick % 10 == 0) PerceptionDebugRenderer.render(this, debugViewers);
    }

    private boolean forced(NPCRuntime npc) {
        PerceptionState st = states.get(npc.getId());
        if (st == null) return true;
        if (!st.pendingDamage.isEmpty() || !st.pendingConversation.isEmpty() || !st.pendingVoice.isEmpty()) return true;
        Dimension d = dimensions.get(st.dimension);
        return d != null && d.sounds().latestTick() > st.lastSoundTick;
    }

    private boolean due(NPCRuntime npc, PerceptionSettings s) {
        PerceptionState st = states.get(npc.getId());
        if (st == null || st.lastPassTick < 0 || forced(npc)) return true;
        int interval = Math.max(1, Math.min(s.visionInterval(), s.hearingInterval()));
        interval = Math.max(1, interval * tierMultiplier(npc, st, s));
        if (st.awareness.level().atLeast(yadi.samuraiai.ai.perception.awareness.AwarenessLevel.ALERT)) interval = Math.max(1, interval / s.alertIntervalDivisor());
        return tick - st.lastPassTick >= interval;
    }

    private boolean pass(NPCRuntime npc, PerceptionSettings s, RayBudget rays) {
        Perceiver perceiver = perceiverOf(npc);
        if (perceiver == null) return false;
        String key = npc.getInstance().getLocation() == null ? "" : npc.getInstance().getLocation().dimensionKey();
        Dimension d = dimension(key);
        if (d == null) return false;
        PerceptionState st = states.computeIfAbsent(npc.getId(), PerceptionState::new);
        st.lastX = perceiver.x(); st.lastY = perceiver.y(); st.lastZ = perceiver.z(); st.lastYaw = perceiver.yaw(); st.dimension = d.key();
        var result = engine.process(perceiver, d.world(), d.sounds(), st, tick, tierMultiplier(npc, st, s), rays);
        for (var event : result.events()) events.publish(event);
        return true;
    }

    private int tierMultiplier(NPCRuntime npc, PerceptionState st, PerceptionSettings s) {
        int[] cache = tiers.computeIfAbsent(npc.getId(), id -> new int[]{1, (int) (Math.floorMod(id.hashCode(), TIER_REFRESH_TICKS))});
        if (tick % TIER_REFRESH_TICKS == cache[1]) {
            double distance = nearestPlayerDistance(st);
            cache[0] = distance <= s.tierNearDistance() ? 1 : distance <= s.tierFarDistance() ? s.midIntervalMultiplier() : s.farIntervalMultiplier();
        }
        return cache[0];
    }

    private double nearestPlayerDistance(PerceptionState st) {
        Dimension d = dimensions.get(st.dimension);
        if (d == null) return Double.MAX_VALUE;
        Player nearest = d.level().getNearestPlayer(st.lastX, st.lastY, st.lastZ, -1.0D, false);
        return nearest == null ? Double.MAX_VALUE : Math.sqrt(nearest.distanceToSqr(st.lastX, st.lastY, st.lastZ));
    }

    // ------------------------------------------------------------------ building the observer

    private Perceiver perceiverOf(NPCRuntime npc) {
        double x, y, z;
        float yaw;
        UUID entityId = npc.getId();
        Optional<MovementBody> body = npc.getController().movementBody(npc.getInstance());
        if (body.isPresent()) {
            MovementBody b = body.get();
            x = b.x(); y = b.y(); z = b.z(); yaw = b.yaw();
            if (b.entityId() != null) entityId = b.entityId();
        } else {
            var location = npc.getInstance().getLocation();
            if (location == null) return null;
            x = location.x(); y = location.y(); z = location.z(); yaw = location.yaw();
        }
        var emotions = npc.getEmotionState();
        UUID npcId = npc.getId();
        return new Perceiver(npcId, entityId, npc.getName(), x, y, z, yaw, 0.0F, 1.62D, emotions.get(Emotion.FEAR), emotions.get(Emotion.ANGER), emotions.get(Emotion.CALM),
                npc.getCurrentGoal() == null ? "IDLE" : npc.getCurrentGoal().getType().name(), senseProfiles.getOrDefault(npcId, SenseProfile.NEUTRAL), target -> relation(npcId, target));
    }

    /** -100..100 from the NPC's relationship with the target, or NaN when it has none (the NPC does not know them). */
    private static double relation(UUID npc, UUID target) {
        return RelationshipService.getInstance().find(npc, target).map(r ->
                Math.max(-100.0D, Math.min(100.0D, (r.getTrust() + r.getRespect() + r.getGratitude() + r.getLoyalty()) / 4.0D - r.getHostility()))).orElse(Double.NaN);
    }

    // ------------------------------------------------------------------ inputs from the world

    public void emitSound(ServerLevel level, SoundEvent event) {
        Dimension d = dimension(level);
        if (d != null) d.sounds().add(event);
    }

    /** Records a sound now, at a position. Loudness 0..1. */
    public void sound(ServerLevel level, SoundCategory category, UUID source, double x, double y, double z, double loudness) {
        emitSound(level, new SoundEvent(UUID.randomUUID(), category, source, x, y, z, loudness, tick, 40, category.name()));
    }

    public void damage(UUID npcId, UUID source, String sourceName, double x, double y, double z, double amount) {
        PerceptionState st = states.computeIfAbsent(npcId, PerceptionState::new);
        st.pendingDamage.add(new Reports.DamageReport(source, sourceName == null ? "unknown" : sourceName, x, y, z, amount, tick));
    }

    public void conversation(UUID npcId, UUID speaker, String name, double x, double y, double z, int length) {
        PerceptionState st = states.computeIfAbsent(npcId, PerceptionState::new);
        if (st.pendingConversation.size() < 16) st.pendingConversation.add(new Reports.ConversationReport(speaker, name, x, y, z, length, tick));
    }

    /** A player spoke aloud (server-side voice input): every NPC within earshot gets a voice report. */
    public void voice(ServerLevel level, UUID speaker, String name, double x, double y, double z, double loudness) {
        double radius = PerceptionSettings.current().voiceRadius() * (0.4D + 0.6D * loudness);
        for (var entry : states.entrySet()) {
            PerceptionState st = entry.getValue();
            if (!st.dimension.equals(level.dimension().location().toString())) continue;
            double dx = st.lastX - x, dy = st.lastY - y, dz = st.lastZ - z;
            if (dx * dx + dy * dy + dz * dz <= radius * radius && st.pendingVoice.size() < 16) st.pendingVoice.add(new Reports.VoiceReport(speaker, name, x, y, z, loudness, tick));
        }
    }

    /** A block changed near NPCs: the ones close enough recheck it on their next block scan. */
    public void blockChanged(ServerLevel level, BlockPos pos) {
        String key = level.dimension().location().toString();
        double range = PerceptionSettings.current().blockScanRadius() + 2.0D;
        for (PerceptionState st : states.values()) {
            if (!st.dimension.equals(key)) continue;
            if (Math.abs(st.lastX - pos.getX()) <= range && Math.abs(st.lastZ - pos.getZ()) <= range && Math.abs(st.lastY - pos.getY()) <= range + 2.0D)
                st.markBlockDirty(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    // ------------------------------------------------------------------ hazards for navigation

    /** Threats the NPCs currently perceive, as danger zones for the navigation engine (contract: DangerSource). */
    private void collectThreats(String dimension, long navTick, java.util.function.Consumer<DangerZone> out) {
        Map<String, DangerZone> merged = new HashMap<>();
        for (PerceptionState st : states.values()) {
            if (!st.dimension.equals(dimension) || st.snapshot == null) continue;
            for (var threat : st.snapshot.threats()) {
                if (threat.score() < PerceptionSettings.current().threatWarning()) continue;
                double radius = switch (threat.category()) { case EXPLOSION -> 9.0D; case HOSTILE -> 6.0D; case DAMAGE_TAKEN -> 5.0D; case FIRE, LAVA -> 3.0D; default -> 4.0D; };
                HazardType type = switch (threat.category()) { case EXPLOSION -> HazardType.EXPLOSION; case HOSTILE -> HazardType.HOSTILE; case FIRE -> HazardType.FIRE;
                    case LAVA -> HazardType.LAVA; default -> HazardType.CUSTOM; };
                NavPos at = NavPos.ofBlock(threat.x(), threat.y(), threat.z());
                String cell = threat.category() + ":" + at.x() / 3 + ":" + at.y() / 3 + ":" + at.z() / 3;
                DangerZone zone = new DangerZone(dimension, at, radius, Math.min(100.0D, threat.score() * 0.9D), navTick + 45, type, "perception");
                DangerZone known = merged.get(cell);
                if (known == null || known.score() < zone.score()) merged.put(cell, zone);
            }
        }
        merged.values().forEach(out);
    }

    // ------------------------------------------------------------------ housekeeping

    /** Drops an NPC's beliefs, and with them the hazards it reported: navigation is refreshed at once so no stale danger zone lingers. */
    /** How this NPC's character scales its senses (set by the scheduler's personality engine). */
    public void setSenseProfile(UUID npcId, SenseProfile profile) { if (profile == null) senseProfiles.remove(npcId); else senseProfiles.put(npcId, profile); }

    public void forget(UUID npcId) {
        senseProfiles.remove(npcId);
        boolean had = states.remove(npcId) != null;
        tiers.remove(npcId);
        if (had) yadi.samuraiai.ai.navigation.world.NavigationService.getInstance().refreshDanger();
    }
    public boolean toggleDebug(UUID player) { return debugViewers.add(player) || !debugViewers.remove(player); }

    /** Called when the server session ends so no world object or belief survives into the next one. */
    public void reset() {
        states.clear(); tiers.clear(); dimensions.clear(); debugViewers.clear(); senseProfiles.clear();
        metrics.reset();
        tick = 0; cursor = 0; npcEntityCacheTick = -1;
    }

    private Dimension dimension(String key) {
        if (key == null || key.isEmpty()) return null;
        var level = ServerWorlds.level(key);
        return level.map(this::dimension).orElse(null);
    }

    private Dimension dimension(ServerLevel level) {
        String key = level.dimension().location().toString();
        Dimension known = dimensions.get(key);
        if (known != null && known.level() == level) return known;
        Dimension created = new Dimension(key, level, new MinecraftPerceptionWorld(level, this::npcEntityIds), known != null ? known.sounds() : new SoundLog());
        dimensions.put(key, created);
        SamuraiLogger.PERCEPTION.info("Perception world created for dimension {}", key);
        return created;
    }

    private Set<UUID> npcEntityIds() {
        if (npcEntityCacheTick == tick) return npcEntityCache;
        Set<UUID> ids = new HashSet<>();
        for (NPCRuntime npc : NPCManager.getInstance().getActive()) ids.addAll(npc.getController().ownedEntityIds());
        npcEntityCache = ids;
        npcEntityCacheTick = tick;
        return ids;
    }

    /** Debug: the snapshot for a category of evidence, used by the inspector. */
    public boolean hasThreat(UUID npcId, StimulusCategory category) {
        return snapshot(npcId).map(s -> s.threats().stream().anyMatch(t -> t.category() == category)).orElse(false);
    }
}
