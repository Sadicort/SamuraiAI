package yadi.samuraiai.ai.scheduler.world;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Collection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import yadi.samuraiai.ai.navigation.movement.MovementBody;
import yadi.samuraiai.ai.navigation.world.MobMovementBody;
import yadi.samuraiai.ai.navigation.world.NavigationService;
import yadi.samuraiai.ai.perception.engine.PerceptionSnapshot;
import yadi.samuraiai.ai.perception.engine.SenseProfile;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.world.PerceptionService;
import yadi.samuraiai.ai.scheduler.emotion.EmotionInput;
import yadi.samuraiai.ai.scheduler.engine.BehaviorScheduler;
import yadi.samuraiai.ai.scheduler.engine.InputSource;
import yadi.samuraiai.ai.scheduler.engine.Investigation;
import yadi.samuraiai.ai.scheduler.engine.Light;
import yadi.samuraiai.ai.scheduler.engine.Perceived;
import yadi.samuraiai.ai.scheduler.engine.SchedulerAdvice;
import yadi.samuraiai.ai.scheduler.engine.SchedulerInput;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.personality.Temperament;
import yadi.samuraiai.ai.scheduler.stack.BackgroundBehavior;
import yadi.samuraiai.ai.scheduler.zone.Place;
import yadi.samuraiai.emotion.Emotion;
import yadi.samuraiai.emotion.EmotionState;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.npc.NPCCapability;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.runtime.ServerScheduler;
import yadi.samuraiai.world.ServerWorlds;
import yadi.samuraiai.world.SpawnLocation;

/**
 * Minecraft-facing front of the behavior scheduler. Once per server tick it hands the scheduler the world clock and a view of
 * the living NPCs (where they are, how far the nearest player is, what they perceive and feel) and afterwards passes each
 * NPC's personality on to perception and navigation as scale factors. The Brain reads the resulting {@link SchedulerAdvice}
 * through the world context. Nothing here moves an NPC or starts a behavior. Server-thread only.
 */
public final class SchedulerService {
    private static final SchedulerService INSTANCE = new SchedulerService();
    public static SchedulerService getInstance() { return INSTANCE; }

    /** What was last handed to the other engines for an NPC, so that nothing is re-applied when it has not changed. */
    private record Applied(Temperament temperament, boolean alert) { }

    private record Pos(String dimension, double x, double y, double z) { }

    private final BehaviorScheduler scheduler = new BehaviorScheduler(SchedulerSettings::current, EventSink.eventBus());
    private final Set<UUID> debugViewers = new HashSet<>();
    private final Map<UUID, Applied> applied = new HashMap<>();
    private final Map<UUID, Integer> lastThreat = new HashMap<>();
    private final Set<UUID> hadInvestigation = new HashSet<>();
    private final Map<UUID, Place> homes = new HashMap<>();
    private Map<UUID, NPCRuntime> npcs = Map.of();
    private List<ServerPlayer> players = List.of();
    private long tick;

    private SchedulerService() { }

    public boolean enabled() { return SchedulerConfig.enabled() && ServerScheduler.getInstance().isRunning(); }
    public BehaviorScheduler scheduler() { return scheduler; }
    public long currentTick() { return tick; }
    public void useEventSink(EventSink sink) { scheduler.useEventSink(sink); }

    /** The advice for an NPC, or empty when the scheduler is off or has not looked at the NPC yet. */
    public Optional<SchedulerAdvice> adviceFor(UUID npcId) {
        return enabled() ? scheduler.advice(npcId).filter(SchedulerAdvice::active) : Optional.empty();
    }

    // ------------------------------------------------------------------ tick

    public void tick() {
        if (!enabled()) return;
        Optional<ServerLevel> overworld = ServerWorlds.level("minecraft:overworld");
        if (overworld.isEmpty()) return;
        tick++;
        Map<UUID, NPCRuntime> living = new HashMap<>();
        for (NPCRuntime runtime : NPCManager.getInstance().getActive()) if (runtime.isActive()) living.put(runtime.getId(), runtime);
        npcs = living;
        players = ServerWorlds.onlinePlayers();
        watchPerception();
        scheduler.tick(tick, overworld.get().getDayTime(), source);
        applyTemperaments();
        if (!debugViewers.isEmpty() && tick % 10 == 0) SchedulerDebugRenderer.render(this, debugViewers);
    }

    /** An NPC that just became worried, or was just told of something to check, is looked at on the next tick whatever its bucket. */
    private void watchPerception() {
        for (UUID id : npcs.keySet()) {
            Optional<PerceptionSnapshot> snapshot = PerceptionService.getInstance().snapshot(id);
            if (snapshot.isEmpty()) continue;
            int level = snapshot.get().threatLevel().ordinal();
            Integer before = lastThreat.put(id, level);
            boolean investigating = snapshot.get().investigationTarget().isPresent();
            boolean newlyInvestigating = investigating && hadInvestigation.add(id);
            if (!investigating) hadInvestigation.remove(id);
            if ((before != null && level > before) || newlyInvestigating) scheduler.disturb(id);
        }
    }

    private void applyTemperaments() {
        for (UUID id : npcs.keySet()) {
            SchedulerAdvice advice = scheduler.advice(id).orElse(null);
            if (advice == null) continue;
            boolean alert = advice.background().contains(BackgroundBehavior.STAY_ALERT) || advice.background().contains(BackgroundBehavior.SCAN_SURROUNDINGS);
            Applied now = new Applied(advice.temperament(), alert);
            if (now.equals(applied.get(id))) continue;
            applied.put(id, now);
            Temperament t = advice.temperament();
            PerceptionService.getInstance().setSenseProfile(id, new SenseProfile(t.visionScale() * (alert ? 1.1D : 1.0D), t.hearingScale() * (alert ? 1.15D : 1.0D),
                    t.curiosity(), t.suspicionGain(), t.fearfulness(), t.attentionSpan() * (alert ? 1.2D : 1.0D)));
            NavigationService.getInstance().setPreferenceAdjuster(id, prefs -> prefs.withDanger(prefs.dangerWeight() * t.dangerAversion(), prefs.maxDanger()));
        }
    }

    // ------------------------------------------------------------------ what the Brain reports back

    /** A routine's place could not be reached. */
    public void unreachable(UUID npcId) { scheduler.unreachable(npcId); }

    /** The NPC left the world: forget everything about it. */
    /** The NPC's home changed (the living world found its real bed): the next evaluation pins the new one. */
    public void rehome(UUID npcId) { homes.remove(npcId); }

    public void forget(UUID npcId) {
        scheduler.forget(npcId);
        applied.remove(npcId); lastThreat.remove(npcId); hadInvestigation.remove(npcId); homes.remove(npcId);
    }

    public void reset() {
        scheduler.reset();
        applied.clear(); lastThreat.clear(); hadInvestigation.clear(); homes.clear(); debugViewers.clear();
        npcs = Map.of(); players = List.of(); tick = 0;
    }

    public boolean toggleDebug(UUID player) { return debugViewers.add(player) || !debugViewers.remove(player); }
    public Collection<NPCRuntime> livingNpcs() { return npcs.values(); }

    // ------------------------------------------------------------------ the world, as the scheduler sees it

    private Pos position(NPCRuntime runtime) {
        Optional<MovementBody> body = runtime.getController().movementBody(runtime.getInstance());
        SpawnLocation location = runtime.getInstance().getLocation();
        if (body.isPresent()) {
            MovementBody b = body.get();
            String dimension = b instanceof MobMovementBody mob ? mob.dimension() : location == null ? null : location.dimensionKey();
            if (dimension != null) return new Pos(dimension, b.x(), b.y(), b.z());
        }
        return location == null ? null : new Pos(location.dimensionKey(), location.x(), location.y(), location.z());
    }

    private double nearestPlayer(Pos pos) {
        double best = Double.MAX_VALUE;
        for (ServerPlayer player : players) {
            if (!player.level.dimension().location().toString().equals(pos.dimension())) continue;
            best = Math.min(best, Math.sqrt(player.distanceToSqr(pos.x(), pos.y(), pos.z())));
        }
        return best;
    }

    private Perceived perceived(UUID id) {
        Optional<PerceptionSnapshot> found = PerceptionService.getInstance().snapshot(id);
        if (found.isEmpty()) return Perceived.CALM;
        PerceptionSnapshot s = found.get();
        var top = s.strongestThreat().orElse(null);
        boolean damaged = s.threats().stream().anyMatch(t -> t.category() == StimulusCategory.DAMAGE_TAKEN);
        Investigation investigation = s.investigationTarget().map(i -> new Investigation(i.x(), i.y(), i.z(), i.uncertainty(), i.urgency())).orElse(null);
        return new Perceived(s.threatLevel().ordinal(), s.threatScore(), s.awareness().ordinal(), s.suspicion(), s.suspicious(), investigation, damaged,
                top == null ? Double.NaN : top.x(), top == null ? Double.NaN : top.y(), top == null ? Double.NaN : top.z());
    }

    private static EmotionInput emotions(EmotionState e) {
        return new EmotionInput(e.get(Emotion.FEAR), e.get(Emotion.ANGER), e.get(Emotion.SADNESS), e.get(Emotion.HAPPINESS), e.get(Emotion.CALM), e.get(Emotion.ANXIETY), e.get(Emotion.TRUST));
    }

    private final InputSource source = new InputSource() {
        @Override public Collection<UUID> npcs() { return npcs.keySet(); }

        @Override public Optional<Light> light(UUID id) {
            NPCRuntime runtime = npcs.get(id);
            if (runtime == null) return Optional.empty();
            Pos pos = position(runtime);
            if (pos == null) return Optional.empty();
            return Optional.of(new Light(runtime.getInstance().getIdentity().type().value(), pos.dimension(), pos.x(), pos.y(), pos.z(), nearestPlayer(pos)));
        }

        @Override public Optional<SchedulerInput> input(UUID id, long now, long worldTime) {
            NPCRuntime runtime = npcs.get(id);
            if (runtime == null) return Optional.empty();
            Pos pos = position(runtime);
            if (pos == null) return Optional.empty();
            Place homePlace = homes.computeIfAbsent(id, key -> {
                // The NPC's home is where it first stood: pinned, so that routines keep their places while it walks about.
                SpawnLocation home = runtime.getInstance().getHome();
                if (home != null) runtime.getInstance().setHome(home);
                return home == null ? null : new Place(home.dimensionKey(), home.x(), home.y(), home.z(), 3.0D, null);
            });
            return Optional.of(new SchedulerInput(id, runtime.getInstance().getIdentity().type().value(), pos.dimension(), pos.x(), pos.y(), pos.z(), worldTime, now,
                    emotions(runtime.getEmotionState()), perceived(id), nearestPlayer(pos), runtime.hasCapability(NPCCapability.CAN_FIGHT), homePlace));
        }
    };
}
