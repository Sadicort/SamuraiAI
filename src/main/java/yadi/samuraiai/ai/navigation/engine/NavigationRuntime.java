package yadi.samuraiai.ai.navigation.engine;

import java.util.*;
import java.util.function.Supplier;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.metrics.NavigationMetrics;
import yadi.samuraiai.ai.navigation.movement.*;
import yadi.samuraiai.ai.navigation.pathfinding.PathState;
import yadi.samuraiai.ai.navigation.planner.PathRequest;
import yadi.samuraiai.ai.navigation.recovery.RecoveryAction;
import yadi.samuraiai.logging.SamuraiLogger;

/**
 * Supervises every active journey of one dimension, once per game tick: spends the shared search budget,
 * runs the movement controller, feeds its reports back to the engine, detects stuck walkers and applies the
 * physical part of recovery. Sessions are visited round-robin and far-away walkers on a reduced frequency so
 * hundreds of NPCs cost a bounded amount per tick. Server-thread only.
 */
public final class NavigationRuntime {
    /** Supplies the physical body behind an NPC id, or null when it has none (chat-only NPC, removed entity). */
    @FunctionalInterface public interface BodyProvider { MovementBody body(UUID npcId); }
    /** Distance from an NPC to the nearest player, used to pick its update frequency. */
    @FunctionalInterface public interface ActivityProbe {
        ActivityProbe ALWAYS_NEAR = npcId -> 0.0D;
        double nearestPlayerDistance(UUID npcId);
    }

    private static final int TIER_REFRESH_TICKS = 40;
    private final NavigationEngine engine;
    private final MovementController movement = new MovementController();
    private final BodyProvider bodies;
    private final ActivityProbe probe;
    private final Supplier<NavigationSettings> settings;
    private final NavigationMetrics metrics;
    private final Map<UUID, NavigationSession> active = new LinkedHashMap<>();
    private final Map<UUID, NavigationSession> lastFinished = new HashMap<>();
    private long tick;

    public NavigationRuntime(NavigationEngine engine, BodyProvider bodies, ActivityProbe probe, Supplier<NavigationSettings> settings) {
        this.engine = engine; this.bodies = bodies; this.probe = probe == null ? ActivityProbe.ALWAYS_NEAR : probe;
        this.settings = settings; this.metrics = engine.metrics();
    }

    public NavigationEngine engine() { return engine; }
    public long currentTick() { return tick; }
    public Collection<NavigationSession> activeSessions() { return List.copyOf(active.values()); }
    public Optional<NavigationSession> session(UUID npcId) {
        NavigationSession live = active.get(npcId);
        return Optional.ofNullable(live != null ? live : lastFinished.get(npcId));
    }

    /** One active journey per NPC: a new request replaces the previous one. */
    public NavigationSession request(PathRequest request, long now) {
        cancel(request.npcId(), "replaced by new request");
        NavigationSession session = engine.open(request, now);
        session.phase = Math.floorMod(session.id.hashCode(), 64);
        session.canceller = reason -> cancel(request.npcId(), reason);
        active.put(request.npcId(), session);
        lastFinished.remove(request.npcId());
        metrics.activeSessions(active.size());
        return session;
    }

    public void cancel(UUID npcId, String reason) {
        NavigationSession session = active.get(npcId);
        if (session == null) return;
        engine.cancel(session, reason, tick);
        finish(session, bodies.body(npcId));
    }

    public void cancelAll(String reason) { for (UUID id : List.copyOf(active.keySet())) cancel(id, reason); }

    public void forget(UUID npcId) {
        cancel(npcId, "npc removed");
        lastFinished.remove(npcId);
    }

    /** A block changed: cached nodes and paths are dropped and walkers through that chunk re-verify immediately. */
    public void worldChanged(NavPos pos) {
        engine.graph().invalidate(pos);
        engine.cache().invalidate(pos);
        for (long chunk : new long[]{pos.chunkKey()})
            for (UUID sessionId : engine.sessionsInChunk(chunk))
                for (NavigationSession s : active.values()) if (s.id.equals(sessionId)) s.lastVerifyTick = -1;
    }

    public void chunkChanged(int chunkX, int chunkZ) {
        engine.graph().invalidateChunk(chunkX, chunkZ);
        engine.cache().invalidateChunk(chunkX, chunkZ);
        for (UUID sessionId : engine.sessionsInChunk(NavPos.chunkKey(chunkX, chunkZ)))
            for (NavigationSession s : active.values()) if (s.id.equals(sessionId)) s.lastVerifyTick = -1;
    }

    public void tick(long now) {
        long started = System.nanoTime();
        tick = now;
        NavigationSettings cfg = settings.get();
        int nodeBudget = cfg.searchNodesPerTick();
        int processed = 0;
        List<NavigationSession> order = new ArrayList<>(active.values());
        int offset = order.isEmpty() ? 0 : (int) (now % order.size());
        for (int n = 0; n < order.size(); n++) {
            NavigationSession s = order.get((n + offset) % order.size());
            if (processed >= cfg.maxSessionsPerTick()) break;
            if (s.state.terminal()) { finish(s, bodies.body(s.request.npcId())); continue; }
            if (now >= s.deadlineTick) {
                engine.fail(s, NavigationFailure.TIMEOUT, "deadline after " + (now - s.createdTick) + " ticks", now);
                finish(s, bodies.body(s.request.npcId()));
                continue;
            }
            if (now % TIER_REFRESH_TICKS == Math.floorMod(s.phase, TIER_REFRESH_TICKS)) s.interval = tier(s, cfg);
            if (s.state == PathState.RUNNING && s.interval > 1 && (now + s.phase) % s.interval != 0) {
                // Not this session's turn to think, but its body must keep being driven every tick.
                MovementBody driven = bodies.body(s.request.npcId());
                if (driven != null && !s.holding) movement.reapply(s.movement, driven);
                continue;
            }
            processed++;
            try { nodeBudget -= step(s, nodeBudget, now, cfg); }
            catch (RuntimeException error) {
                SamuraiLogger.CORE.error("Navigation step failed npc={}", s.request.npcId(), error);
                engine.fail(s, NavigationFailure.INTERNAL_ERROR, error.toString(), now);
            }
            if (s.state.terminal()) finish(s, bodies.body(s.request.npcId()));
        }
        metrics.activeSessions(active.size());
        metrics.tick(System.nanoTime() - started);
    }

    private int tier(NavigationSession s, NavigationSettings cfg) {
        double distance = probe.nearestPlayerDistance(s.request.npcId());
        if (distance <= cfg.nearPlayerDistance()) return 1;
        if (distance <= cfg.farPlayerDistance()) return 2;
        return cfg.farUpdateInterval();
    }

    /** Returns the search nodes spent, so the shared per-tick budget can be split fairly. */
    private int step(NavigationSession s, int nodeBudget, long now, NavigationSettings cfg) {
        MovementBody body = bodies.body(s.request.npcId());
        if (body == null) { engine.fail(s, NavigationFailure.NO_BODY, "NPC has no physical body", now); return 0; }
        if (!body.alive()) { engine.fail(s, NavigationFailure.BODY_LOST, "body not alive", now); return 0; }
        NavPos current = body.block();
        int used = 0;
        switch (s.state) {
            case REQUESTED, BUILDING, RECALCULATING -> {
                if (s.state == PathState.REQUESTED) s.request = s.request.withStart(current);
                used = engine.plan(s, Math.max(0, nodeBudget), now);
                if (s.state != PathState.READY && s.state != PathState.RUNNING) movement.hold(body);
                if (s.state == PathState.READY) engine.started(s, now);
                if (s.state == PathState.RUNNING) running(s, body, current, now, cfg);
            }
            case READY -> { engine.started(s, now); running(s, body, current, now, cfg); }
            case RUNNING -> running(s, body, current, now, cfg);
            case BLOCKED -> blocked(s, body, now, cfg);
            default -> { }
        }
        return used;
    }

    private void running(NavigationSession s, MovementBody body, NavPos current, long now, NavigationSettings cfg) {
        long began = System.nanoTime();
        if (now - s.lastVerifyTick >= cfg.verifyIntervalTicks()) {
            s.lastVerifyTick = now;
            NavigationEngine.VerifyOutcome outcome = engine.verify(s, current, body.entityId(), now);
            if (s.state.terminal()) return;
            if (outcome != NavigationEngine.VerifyOutcome.OK) { movement.hold(body); return; }
        }
        if (s.holding) { movement.hold(body); return; }
        MovementReport report = movement.tick(s.movement, body, s.path, s.request.prefs().mode(), s.request.arrivalRadius(), cfg);
        engine.onMovement(s, report, current, now);
        if (s.state != PathState.RUNNING) { movement.hold(body); return; }
        int stuckTicks = s.stuck.update(body.x(), body.y(), body.z(), now, report.status() == MovementReport.Status.MOVING);
        if (stuckTicks > 0) recover(s, body, current, stuckTicks, now);
        s.metrics.movementNanos += System.nanoTime() - began;
    }

    private void recover(NavigationSession s, MovementBody body, NavPos current, int stuckTicks, long now) {
        engine.trace(s, "body x={} y={} z={} onGround={} yaw={} door={}", String.format("%.2f", body.x()), String.format("%.2f", body.y()),
                String.format("%.2f", body.z()), body.onGround(), String.format("%.0f", body.yaw()), s.movement.door().phase() + " " + body.diagnostics());
        RecoveryAction action = engine.stuck(s, current, stuckTicks, now);
        switch (action) {
            case JUMP_NUDGE -> body.jump();
            case BACKTRACK -> s.movement.follower().backtrack(2);
            case SAFE_TELEPORT -> {
                Optional<NavPos> target = engine.safeTeleportTarget(s, current);
                if (target.isEmpty() || !body.teleportSafe(target.get())) action = RecoveryAction.CANCEL;
                else s.movement.resetTracking();
            }
            default -> { }
        }
        engine.applyRecovery(s, action, current, now);
        if (s.state == PathState.BLOCKED || s.state == PathState.RECALCULATING) movement.hold(body);
    }

    private void blocked(NavigationSession s, MovementBody body, long now, NavigationSettings cfg) {
        movement.hold(body);
        switch (s.blockReason) {
            case "chunk-unloaded" -> {
                if (engine.chunksReady(s)) engine.unblock(s);
                else if (now - s.blockedSince >= cfg.chunkWaitTicks())
                    engine.fail(s, NavigationFailure.CHUNK_UNLOADED, "ground ahead stayed unloaded for " + (now - s.blockedSince) + " ticks", now);
            }
            case "recovery-wait" -> { if (now >= s.waitUntil) engine.unblock(s); }
            default -> engine.unblock(s);
        }
    }

    /** Wraps up a finished session: releases the body, closes doors it opened, and moves it to the finished map. */
    private void finish(NavigationSession s, MovementBody body) {
        if (active.get(s.request.npcId()) == s) active.remove(s.request.npcId());
        lastFinished.put(s.request.npcId(), s);
        if (lastFinished.size() > 512) lastFinished.remove(lastFinished.keySet().iterator().next());
        if (body != null) {
            try {
                movement.hold(body);
                NavPos closed = s.movement.door().abandon(body, settings.get().closeDoorsBehind());
                if (closed != null) engine.doorClosed(s, closed);
            } catch (RuntimeException error) { SamuraiLogger.CORE.warn("Navigation cleanup failed npc={}", s.request.npcId(), error); }
        }
        metrics.activeSessions(active.size());
    }

    public void reset() {
        for (NavigationSession s : List.copyOf(active.values())) { engine.cancel(s, "runtime reset", tick); finish(s, null); }
        active.clear(); lastFinished.clear();
        engine.chunkIndex().clear();
    }
}
