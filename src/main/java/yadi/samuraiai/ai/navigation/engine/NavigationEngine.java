package yadi.samuraiai.ai.navigation.engine;

import java.util.*;
import java.util.function.Supplier;
import yadi.samuraiai.ai.navigation.cache.PathCache;
import yadi.samuraiai.ai.navigation.chunks.*;
import yadi.samuraiai.ai.navigation.events.*;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.ai.navigation.graph.*;
import yadi.samuraiai.ai.navigation.metrics.NavigationMetrics;
import yadi.samuraiai.ai.navigation.movement.MovementReport;
import yadi.samuraiai.ai.navigation.obstacles.*;
import yadi.samuraiai.ai.navigation.pathfinding.*;
import yadi.samuraiai.ai.navigation.planner.*;
import yadi.samuraiai.ai.navigation.recovery.*;
import yadi.samuraiai.ai.navigation.terrain.TerrainCostTable;
import yadi.samuraiai.ai.navigation.zones.DangerMap;
import yadi.samuraiai.logging.SamuraiLogger;

/**
 * Navigation core for one dimension: validates destinations, plans and replans paths within a node budget,
 * verifies them against a changing world, decides recoveries, and publishes every state change. It never
 * moves an entity and never chooses a destination; those belong to the movement controller and the behaviors.
 */
public final class NavigationEngine {
    public enum VerifyOutcome { OK, HOLD, REPLANNING, BLOCKED }

    private final NavigationGraph graph;
    private final DangerMap dangers;
    private final PathCache cache;
    private final TerrainCostTable costTable;
    private final Supplier<NavigationSettings> settings;
    private final NavigationMetrics metrics;
    private final EventSink events;
    private final DestinationValidator validator;
    private final PathfindingEngine pathfinding;
    private final ObstacleScanner scanner;
    private final ChunkLoadGuard chunkGuard;
    private final ChunkRoutePlanner routePlanner;
    private final ChunkPathIndex chunkIndex = new ChunkPathIndex();

    public NavigationEngine(NavigationGraph graph, DangerMap dangers, PathCache cache, TerrainCostTable costTable,
                            Supplier<NavigationSettings> settings, NavigationMetrics metrics, EventSink events,
                            EntityObstacleSource entities) {
        this.graph = graph; this.dangers = dangers; this.cache = cache; this.costTable = costTable;
        this.settings = settings; this.metrics = metrics; this.events = events;
        this.validator = new DestinationValidator(graph, dangers);
        this.pathfinding = new PathfindingEngine(graph);
        this.scanner = new ObstacleScanner(graph, entities);
        this.chunkGuard = new ChunkLoadGuard(graph.view());
        this.routePlanner = new ChunkRoutePlanner(graph.view());
    }

    public NavigationGraph graph() { return graph; }
    public DangerMap dangers() { return dangers; }
    public PathCache cache() { return cache; }
    public ChunkPathIndex chunkIndex() { return chunkIndex; }
    public ChunkLoadGuard chunkGuard() { return chunkGuard; }
    public ChunkRoutePlanner routePlanner() { return routePlanner; }
    public ObstacleScanner scanner() { return scanner; }
    public NavigationMetrics metrics() { return metrics; }
    public NavigationSettings config() { return settings.get(); }
    public TerrainCostTable costTable() { return costTable; }

    // ------------------------------------------------------------------ lifecycle

    public NavigationSession open(PathRequest request, long tick) {
        NavigationSettings cfg = settings.get();
        NavigationSession session = new NavigationSession(request, tick, new StuckDetector(cfg.stuckTicks(), cfg.stuckMinProgress()));
        session.deadlineTick = tick + request.timeoutTicks();
        metrics.requested();
        events.publish(new PathRequestedEvent(request.npcId(), session.id, request.start(), request.goal(), request.behavior()));
        return session;
    }

    /** Spends up to {@code budget} search nodes on this session. Returns the nodes actually expanded. */
    public int plan(NavigationSession s, int budget, long tick) {
        if (s.state.terminal()) return 0;
        try {
            if (s.state == PathState.REQUESTED && !prepare(s, tick)) return 0;
            if ((s.state == PathState.BUILDING || s.state == PathState.RECALCULATING) && s.search != null)
                return advanceSearch(s, budget, tick);
            return 0;
        } catch (RuntimeException error) {
            SamuraiLogger.CORE.error("Navigation planning failed npc={}", s.request.npcId(), error);
            fail(s, NavigationFailure.INTERNAL_ERROR, error.toString(), tick);
            return 0;
        }
    }

    public void started(NavigationSession s, long tick) {
        if (!transition(s, PathState.RUNNING)) return;
        s.stuck.reset();
        s.lastEvent = "started";
        events.publish(new PathStartedEvent(s.request.npcId(), s.id));
    }

    public void complete(NavigationSession s, long tick) {
        if (!transition(s, PathState.COMPLETED)) return;
        s.finishedTick = tick;
        s.lastEvent = "completed";
        chunkIndex.unregister(s.id);
        metrics.finished(s.metrics, true, false, NavigationFailure.NONE);
        events.publish(new PathCompletedEvent(s.request.npcId(), s.id, tick - s.createdTick, s.metrics.distance));
    }

    public void cancel(NavigationSession s, String reason, long tick) {
        if (!transition(s, PathState.CANCELLED)) return;
        s.finishedTick = tick;
        s.failureDetail = reason == null ? "" : reason;
        s.lastEvent = "cancelled: " + s.failureDetail;
        chunkIndex.unregister(s.id);
        metrics.finished(s.metrics, false, true, NavigationFailure.NONE);
        events.publish(new PathCancelledEvent(s.request.npcId(), s.id, s.failureDetail));
    }

    public void fail(NavigationSession s, NavigationFailure reason, String detail, long tick) {
        if (!transition(s, PathState.FAILED)) return;
        s.finishedTick = tick;
        s.failure = reason;
        s.failureDetail = detail == null ? "" : detail;
        s.lastEvent = "failed: " + reason;
        chunkIndex.unregister(s.id);
        metrics.finished(s.metrics, false, false, reason);
        events.publish(new PathFailedEvent(s.request.npcId(), s.id, reason, s.failureDetail));
        if (settings.get().debugLogging())
            SamuraiLogger.CORE.info("navigation failed npc={} reason={} detail={}", s.request.npcId(), reason, detail);
    }

    // ------------------------------------------------------------------ planning

    private boolean prepare(NavigationSession s, long tick) {
        NavigationSettings cfg = settings.get();
        PathPreferences prefs = s.request.prefs();
        DestinationResult destination = validator.validate(s.request.goal(), prefs, tick);
        if (!destination.usable()) {
            NavigationFailure reason = switch (destination.status()) {
                case CHUNK_UNLOADED -> NavigationFailure.CHUNK_UNLOADED;
                case DANGEROUS, LAVA, VOID -> NavigationFailure.DANGER;
                default -> NavigationFailure.DESTINATION_INVALID;
            };
            fail(s, reason, destination.status() + ": " + destination.detail(), tick);
            return false;
        }
        s.resolvedGoal = destination.resolved();
        Optional<NavPos> start = resolveStart(s.request.start(), prefs);
        if (start.isEmpty()) {
            if (s.startRetries++ < cfg.startRetryTicks()) return false;
            fail(s, NavigationFailure.START_INVALID, "no standable position near " + s.request.start(), tick);
            return false;
        }
        s.request = s.request.withStart(start.get());
        String dimension = graph.view().dimension();
        Optional<NavigationPath> cached = cache.get(dimension, start.get(), s.resolvedGoal, prefs.cacheSignature(), tick);
        if (cached.isPresent() && cachedPathStillValid(cached.get(), prefs, tick, s)) {
            s.cacheHit = true; s.metrics.cacheHit = true;
            adopt(s, cached.get(), true, tick);
            return true;
        }
        beginSearch(s, start.get(), s.resolvedGoal, cfg.maxSearchNodes(), cfg.maxSearchRadius(), tick, false);
        return true;
    }

    private boolean cachedPathStillValid(NavigationPath path, PathPreferences prefs, long tick, NavigationSession s) {
        int check = Math.min(path.size() - 1, 24);
        for (int i = 0; i <= check; i++) graph.refresh(path.get(i).pos());
        PathCostModel model = model(s, prefs, path.start(), tick);
        return pathfinding.validator().validate(path, 0, check, prefs, model).valid();
    }

    private void beginSearch(NavigationSession s, NavPos start, NavPos goal, int maxNodes, int radius, long tick, boolean recalculation) {
        PathPreferences prefs = s.request.prefs();
        // Someone already standing in a hazard (a stale or nearby danger zone) must be able to walk out of it: raise the tolerance
        // to the danger of the starting cell so the search can leave, while the danger weight still makes it leave the quickest way.
        double startDanger = model(s, prefs, start, tick).danger(graph.node(start));
        PathPreferences searchPrefs = startDanger > prefs.maxDanger() ? prefs.withDanger(prefs.dangerWeight(), startDanger + 5.0D) : prefs;
        PathCostModel model = model(s, searchPrefs, start, tick);
        s.search = pathfinding.begin(model, searchPrefs, start, goal, maxNodes, radius, s.blocked);
        if (!recalculation) transition(s, PathState.BUILDING);
    }

    private PathCostModel model(NavigationSession s, PathPreferences prefs, NavPos at, long tick) {
        return new PathCostModel(costTable, dangers, graph.view().dimension(), prefs, graph.view().environment(at), tick);
    }

    private int advanceSearch(NavigationSession s, int budget, long tick) {
        PathSearch search = s.search;
        int before = search.expanded();
        long nanosBefore = search.spentNanos();
        PathSearch.Status status = search.advance(budget);
        int used = search.expanded() - before;
        s.metrics.nodesExpanded += used;
        s.metrics.searchNanos += search.spentNanos() - nanosBefore;
        if (status == PathSearch.Status.IN_PROGRESS) { metrics.budgetExhausted(); return used; }
        finishSearch(s, status == PathSearch.Status.LIMIT, tick);
        return used;
    }

    private void finishSearch(NavigationSession s, boolean hitLimit, long tick) {
        NavigationSettings cfg = settings.get();
        PathSearch search = s.search;
        s.search = null;
        Optional<NavigationPath> result = pathfinding.finish(search, s.request.prefs(), tick, cfg.partialMinGain());
        boolean detour = s.detourResumeIndex >= 0;
        if (result.isEmpty() || (result.get().partial() && (!s.request.allowPartial() || detour))) {
            if (detour) {
                // A local detour found nothing: fall back to replanning the whole remaining route.
                NavPos from = search.start();
                s.detourResumeIndex = -1; s.detourBase = null;
                beginRecalculation(s, "detour-failed", from, Set.of(), -1, tick);
                return;
            }
            boolean chunkProblem = !routePlanner.route(s.request.start(), s.resolvedGoal).fullyLoaded();
            fail(s, chunkProblem ? NavigationFailure.CHUNK_UNLOADED : hitLimit ? NavigationFailure.SEARCH_LIMIT : NavigationFailure.DESTINATION_UNREACHABLE,
                    "search " + (hitLimit ? "limit" : "exhausted") + " expanded=" + search.expanded(), tick);
            return;
        }
        NavigationPath found = result.get();
        if (detour) found = splice(s.detourBase, found, s.detourResumeIndex, tick);
        s.detourResumeIndex = -1; s.detourBase = null;
        adopt(s, found, false, tick);
    }

    private NavigationPath splice(NavigationPath base, NavigationPath detour, int resumeIndex, long tick) {
        List<PathNode> nodes = new ArrayList<>(detour.nodes());
        for (int i = resumeIndex + 1; i < base.size(); i++) nodes.add(base.get(i));
        return new NavigationPath(nodes, detour.cost() + base.cost() * (base.size() - resumeIndex) / Math.max(1, base.size()), base.partial(), tick, detour.expanded());
    }

    private void adopt(NavigationSession s, NavigationPath path, boolean fromCache, long tick) {
        boolean firstPath = s.path == null;
        s.path = path;
        trace(s, "path adopted nodes={} cost={} partial={} cached={} first={} last={}", path.size(), String.format("%.1f", path.cost()), path.partial(), fromCache, path.start(), path.end());
        if (settings.get().debugLogging() && path.size() <= 16) {
            StringBuilder nodes = new StringBuilder();
            path.nodes().forEach(n -> nodes.append(n.pos()).append(':').append(n.via()).append(' '));
            trace(s, "path nodes {}", nodes.toString().trim());
        }
        s.movement.forceRebind();
        chunkIndex.register(s.id, path);
        s.metrics.cost += path.cost();
        metrics.pathCreated(fromCache);
        if (!fromCache && !path.partial() && firstPath)
            cache.put(graph.view().dimension(), path.start(), path.end(), s.request.prefs().cacheSignature(), path, tick);
        if (s.state == PathState.REQUESTED || s.state == PathState.BUILDING) {
            transition(s, PathState.READY);
            s.lastEvent = fromCache ? "path from cache" : "path ready";
            events.publish(new PathCreatedEvent(s.request.npcId(), s.id, path.size(), path.cost(), fromCache, path.partial()));
        } else {
            transition(s, PathState.RUNNING);
            s.stuck.reset();
            s.lastEvent = "recalculated (" + s.recalcReason + ")";
            events.publish(new PathRecalculatedEvent(s.request.npcId(), s.id, s.recalculations, s.recalcReason));
        }
    }

    /** Starts a new search from where the walker really is. Local detours splice back into the old path. */
    public boolean beginRecalculation(NavigationSession s, String reason, NavPos current, Set<NavPos> extraBlocked,
                                      int resumeIndex, long tick) {
        NavigationSettings cfg = settings.get();
        if (s.state.terminal()) return false;
        if (s.recalculations >= cfg.maxRecalculations()) {
            fail(s, NavigationFailure.TOO_MANY_RECALCULATIONS, "recalculations=" + s.recalculations + " last=" + reason, tick);
            return false;
        }
        Optional<NavPos> start = resolveStart(current, s.request.prefs());
        if (start.isEmpty()) { fail(s, NavigationFailure.START_INVALID, "no standable position near " + current, tick); return false; }
        s.recalculations++;
        s.metrics.recalculations++;
        s.recalcReason = reason;
        metrics.recalculated();
        s.blocked.addAll(extraBlocked);
        s.blocked.remove(s.resolvedGoal);
        if (s.state != PathState.RECALCULATING && !transition(s, PathState.RECALCULATING)) return false;
        NavPos goal = s.resolvedGoal;
        int nodes = cfg.maxSearchNodes(), radius = cfg.maxSearchRadius();
        if (resumeIndex >= 0 && s.path != null && resumeIndex < s.path.size()) {
            goal = s.path.get(resumeIndex).pos();
            s.detourResumeIndex = resumeIndex; s.detourBase = s.path;
            nodes = Math.min(nodes, 1500); radius = Math.min(radius, 16);
        } else { s.detourResumeIndex = -1; s.detourBase = null; }
        beginSearch(s, start.get(), goal, nodes, radius, tick, true);
        s.lastEvent = "recalculating: " + reason;
        trace(s, "recalculating reason={} from={} goal={} detourResume={}", reason, start.get(), goal, resumeIndex);
        return true;
    }

    // ------------------------------------------------------------------ verification while walking

    /** Re-checks the next nodes against the live world and watches for entities in the way. */
    public VerifyOutcome verify(NavigationSession s, NavPos current, UUID selfEntity, long tick) {
        NavigationSettings cfg = settings.get();
        NavigationPath path = s.path;
        if (path == null) return VerifyOutcome.OK;
        PathPreferences prefs = s.request.prefs();
        int index = s.movement.index();
        int look = Math.min(cfg.lookaheadNodes(), 5);
        int last = Math.min(path.size() - 1, index + look);
        for (int i = index; i <= last; i++) graph.refresh(path.get(i).pos());
        PathCostModel model = model(s, prefs, current, tick);
        PathValidation validation = pathfinding.validator().validate(path, index, cfg.lookaheadNodes(), prefs, model);
        if (!validation.valid()) {
            s.metrics.obstacles++;
            NavPos at = path.get(validation.invalidIndex()).pos();
            trace(s, "path invalid index={}/{} pos={} reason={} from={}", validation.invalidIndex(), path.size(), at, validation.reason(), current);
            if (validation.reason() == PathValidation.Reason.CHUNK_UNLOADED) { block(s, "chunk-unloaded", at, tick); return VerifyOutcome.BLOCKED; }
            for (Obstacle obstacle : scanner.staticAhead(path, index, cfg.lookaheadNodes(), prefs))
                s.lastEvent = "obstacle " + obstacle.type() + " at " + obstacle.pos();
            int resume = firstValidAfter(s, validation.invalidIndex(), prefs, model);
            events.publish(new PathBlockedEvent(s.request.npcId(), s.id, validation.reason().name(), at));
            metrics.blocked(); s.metrics.blocks++;
            beginRecalculation(s, "world-changed:" + validation.reason(), current, Set.of(), resume, tick);
            return VerifyOutcome.REPLANNING;
        }
        List<Obstacle> moving = scanner.temporaryAhead(path, index, current.centerX(), current.centerZ(), cfg.entityLookaheadBlocks(), selfEntity);
        ObstacleDecision decision = s.temporary.observe(moving, tick, cfg.tempObstacleWaitTicks());
        if (decision == ObstacleDecision.WAIT) { s.holding = true; s.metrics.obstacles++; s.lastEvent = "waiting for " + moving.get(0).type(); return VerifyOutcome.HOLD; }
        s.holding = false;
        if (decision == ObstacleDecision.DETOUR) {
            Set<NavPos> cells = new HashSet<>();
            for (Obstacle o : moving) cells.add(o.pos());
            int resume = Math.min(path.size() - 1, moving.get(moving.size() - 1).pathIndex() + 2);
            s.temporary.reset();
            metrics.blocked(); s.metrics.blocks++;
            events.publish(new PathBlockedEvent(s.request.npcId(), s.id, "entity-in-way", moving.get(0).pos()));
            beginRecalculation(s, "detour-around-" + moving.get(0).type(), current, cells, resume, tick);
            return VerifyOutcome.REPLANNING;
        }
        return VerifyOutcome.OK;
    }

    /** The first node after {@code invalid} that is still valid: where a local detour should rejoin the old path. */
    private int firstValidAfter(NavigationSession s, int invalid, PathPreferences prefs, PathCostModel model) {
        NavigationPath path = s.path;
        for (int i = invalid + 1; i < path.size() && i <= invalid + 8; i++) {
            graph.refresh(path.get(i).pos());
            if (graph.standable(path.get(i).pos(), prefs) && !Double.isInfinite(model.edgeCost(graph.node(path.get(i).pos()), path.get(i).via(), 1.0D)))
                return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ movement feedback and recovery

    public void onMovement(NavigationSession s, MovementReport report, NavPos current, long tick) {
        s.metrics.ticks++;
        s.metrics.distance += report.moved();
        if (report.jumped()) s.metrics.jumps++;
        if (report.doorOpened() != null || report.doorClosed() != null || report.status() == MovementReport.Status.DOOR_FAILED)
            trace(s, "door opened={} closed={} status={}", report.doorOpened(), report.doorClosed(), report.status());
        if (report.doorOpened() != null) { s.metrics.doorsOpened++; events.publish(new DoorOpenedEvent(s.request.npcId(), report.doorOpened())); }
        if (report.doorClosed() != null) { s.metrics.doorsClosed++; events.publish(new DoorClosedEvent(s.request.npcId(), report.doorClosed())); }
        long chunk = current.chunkKey();
        if (chunk != s.lastChunk) { if (s.lastChunk != Long.MIN_VALUE) s.metrics.chunksEntered++; s.lastChunk = chunk; }
        switch (report.status()) {
            case ARRIVED -> {
                if (s.path.partial()) partialEnd(s, current, tick);
                else complete(s, tick);
            }
            case OFF_PATH -> beginRecalculation(s, "off-path", current, Set.of(), -1, tick);
            case DOOR_FAILED -> {
                NavPos door = s.path.get(Math.min(report.index(), s.path.size() - 1)).pos();
                metrics.blocked(); s.metrics.blocks++;
                events.publish(new PathBlockedEvent(s.request.npcId(), s.id, "door-locked", door));
                s.blocked.add(door);
                if (s.recalculations >= settings.get().maxRecalculations()) fail(s, NavigationFailure.DOOR_LOCKED, "door at " + door + " will not open", tick);
                else beginRecalculation(s, "door-locked", current, Set.of(door), -1, tick);
            }
            case BODY_LOST -> fail(s, NavigationFailure.BODY_LOST, "body no longer alive or present", tick);
            case MOVING, WAITING_DOOR -> { }
        }
    }

    /** A partial path ended: keep going toward the real goal, unless the last attempt made no progress. */
    private void partialEnd(NavigationSession s, NavPos current, long tick) {
        NavigationSettings cfg = settings.get();
        NavPos end = s.path.end();
        boolean stalled = s.lastPartialEnd != null && s.lastPartialEnd.horizontalDistance(end) < cfg.partialMinGain();
        s.lastPartialEnd = end;
        if (stalled) {
            boolean chunkProblem = !routePlanner.route(current, s.resolvedGoal).fullyLoaded();
            fail(s, chunkProblem ? NavigationFailure.CHUNK_UNLOADED : NavigationFailure.DESTINATION_UNREACHABLE, "no progress past " + end, tick);
            return;
        }
        beginRecalculation(s, "partial-end", current, Set.of(), -1, tick);
    }

    public void block(NavigationSession s, String reason, NavPos at, long tick) {
        if (s.state == PathState.RUNNING && transition(s, PathState.BLOCKED)) {
            s.blockedSince = tick; s.blockReason = reason; s.lastEvent = "blocked: " + reason;
            metrics.blocked(); s.metrics.blocks++;
            events.publish(new PathBlockedEvent(s.request.npcId(), s.id, reason, at));
        }
    }

    public void unblock(NavigationSession s) {
        if (s.state == PathState.BLOCKED && transition(s, PathState.RUNNING)) {
            s.blockedSince = -1; s.blockReason = ""; s.waitUntil = -1; s.stuck.reset(); s.lastEvent = "resumed";
        }
    }

    /** Reports a stuck walker and picks the next recovery rung. The runtime performs the parts that touch the body. */
    public RecoveryAction stuck(NavigationSession s, NavPos at, int stuckTicks, long tick) {
        NavigationSettings cfg = settings.get();
        metrics.stuck(); metrics.recovery(); s.metrics.stuckEvents++; s.metrics.recoveries++;
        events.publish(new NPCStuckEvent(s.request.npcId(), s.id, at, stuckTicks));
        RecoveryAction action = new RecoveryEngine(cfg.maxRecoveryAttempts(), cfg.allowTeleportRecovery()).next(s.recoveryAttempts++);
        s.lastEvent = "stuck -> " + action;
        trace(s, "stuck at={} ticks={} index={}/{} target={} action={}", at, stuckTicks, s.movement.index(), s.path == null ? 0 : s.path.size(),
                s.path == null ? "-" : s.path.get(Math.min(s.movement.index(), s.path.size() - 1)), action);
        return action;
    }

    /** Applies the engine-side effect of a recovery step. Returns true when the session keeps going. */
    public boolean applyRecovery(NavigationSession s, RecoveryAction action, NavPos current, long tick) {
        switch (action) {
            case JUMP_NUDGE, BACKTRACK, SAFE_TELEPORT -> { return true; }
            case RECALCULATE -> { return beginRecalculation(s, "stuck", current, Set.of(), -1, tick); }
            case ALTERNATIVE_ROUTE -> {
                return beginRecalculation(s, "alternative-route", current, cellsAhead(s, current, 2), -1, tick);
            }
            case WAIT -> {
                block(s, "recovery-wait", current, tick);
                s.waitUntil = tick + 20;
                return true;
            }
            case CANCEL -> { fail(s, NavigationFailure.STUCK, "recovery exhausted after " + s.recoveryAttempts + " attempts", tick); return false; }
        }
        return false;
    }

    /** Cells on the route within a few blocks ahead of the walker (excluding its own cell and the goal). */
    private Set<NavPos> cellsAhead(NavigationSession s, NavPos current, int count) {
        Set<NavPos> cells = new LinkedHashSet<>();
        NavigationPath path = s.path;
        if (path == null) return cells;
        double cx = current.centerX(), cz = current.centerZ();
        for (int i = s.movement.index(); i < path.size() && cells.size() < count; i++) {
            NavPos node = path.get(i).pos();
            double tx = node.centerX(), tz = node.centerZ(), length = Math.hypot(tx - cx, tz - cz);
            for (double d = 1.0D; d <= length && cells.size() < count; d += 1.0D) {
                NavPos cell = new NavPos((int) Math.floor(cx + (tx - cx) * d / length), node.y(), (int) Math.floor(cz + (tz - cz) * d / length));
                if (!cell.equals(current) && !cell.equals(s.resolvedGoal)) cells.add(cell);
            }
            cx = tx; cz = tz;
        }
        return cells;
    }

    /** A validated standing cell a few blocks ahead along the route, for the optional safe-teleport recovery. */
    public Optional<NavPos> safeTeleportTarget(NavigationSession s, NavPos current) {
        if (s.path == null) return Optional.empty();
        PathPreferences prefs = s.request.prefs();
        double cx = current.centerX(), cz = current.centerZ(), remaining = 4.0D;
        NavPos best = null;
        for (int i = s.movement.index(); i < s.path.size() && remaining > 0; i++) {
            NavPos node = s.path.get(i).pos();
            double tx = node.centerX(), tz = node.centerZ(), length = Math.hypot(tx - cx, tz - cz);
            double used = Math.min(length, remaining);
            NavPos cell = length < 1.0E-6D ? node : new NavPos((int) Math.floor(cx + (tx - cx) * used / length), node.y(), (int) Math.floor(cz + (tz - cz) * used / length));
            graph.refresh(cell);
            if (!cell.equals(current) && graph.standable(cell, prefs) && graph.node(cell).staticDanger() < prefs.maxDanger() / 2) best = cell;
            remaining -= used;
            cx = tx; cz = tz;
        }
        return Optional.ofNullable(best);
    }

    /** True when the ground ahead is loaded again, ending a chunk wait. */
    public boolean chunksReady(NavigationSession s) {
        return s.path == null || chunkGuard.unloadedAhead(s.path, s.movement.index(), settings.get().lookaheadNodes()).isEmpty();
    }

    /** Announces a door the runtime closed while wrapping up a session. */
    public void doorClosed(NavigationSession s, NavPos door) {
        s.metrics.doorsClosed++;
        events.publish(new DoorClosedEvent(s.request.npcId(), door));
    }

    /** Sessions whose path crosses this chunk; used when a chunk unloads or a block changes. */
    public Set<UUID> sessionsInChunk(long chunkKey) { return chunkIndex.sessionsIn(chunkKey); }

    // ------------------------------------------------------------------ helpers

    public Optional<NavPos> resolveStart(NavPos p, PathPreferences prefs) {
        List<NavPos> candidates = List.of(p, p.offset(0, -1, 0), p.offset(0, -2, 0), p.offset(0, 1, 0),
                p.offset(1, 0, 0), p.offset(-1, 0, 0), p.offset(0, 0, 1), p.offset(0, 0, -1),
                p.offset(1, -1, 0), p.offset(-1, -1, 0), p.offset(0, -1, 1), p.offset(0, -1, -1),
                p.offset(1, 1, 0), p.offset(-1, 1, 0), p.offset(0, 1, 1), p.offset(0, 1, -1));
        for (NavPos candidate : candidates) {
            if (!graph.view().isLoaded(candidate.chunkX(), candidate.chunkZ())) continue;
            graph.refresh(candidate);
            if (graph.standable(candidate, prefs)) return Optional.of(candidate);
        }
        return Optional.empty();
    }

    /** Structured debug line, only when {@code debugLogging} is on, so normal servers stay quiet. */
    void trace(NavigationSession s, String message, Object... args) {
        if (!settings.get().debugLogging()) return;
        SamuraiLogger.NAVIGATION.info("npc=" + s.request.npcId() + " session=" + s.id.toString().substring(0, 8) + " state=" + s.state + " " + message, args);
    }

    private boolean transition(NavigationSession s, PathState next) {
        if (!s.state.canMoveTo(next)) {
            SamuraiLogger.CORE.debug("navigation: rejected state change {} -> {} npc={}", s.state, next, s.request.npcId());
            return false;
        }
        s.state = next;
        return true;
    }
}
