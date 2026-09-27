package yadi.samuraiai.ai.navigation.engine;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.metrics.SessionMetrics;
import yadi.samuraiai.ai.navigation.movement.MovementState;
import yadi.samuraiai.ai.navigation.obstacles.TemporaryObstacleTracker;
import yadi.samuraiai.ai.navigation.pathfinding.*;
import yadi.samuraiai.ai.navigation.planner.PathRequest;
import yadi.samuraiai.ai.navigation.recovery.StuckDetector;

/**
 * The full state of one NPC's journey: request, current path, progress, search in flight, recovery counters
 * and metrics. Owned by the runtime and mutated only by the engine on the server thread; behaviors get the
 * read-only {@link NavigationHandle} view.
 */
public final class NavigationSession implements NavigationHandle {
    final UUID id = UUID.randomUUID();
    PathRequest request;
    NavPos resolvedGoal;
    PathState state = PathState.REQUESTED;
    NavigationPath path;
    PathSearch search;
    int detourResumeIndex = -1;
    NavigationPath detourBase;
    String recalcReason = "";
    int recalculations, recoveryAttempts, startRetries;
    final Set<NavPos> blocked = new HashSet<>();
    NavigationFailure failure = NavigationFailure.NONE;
    String failureDetail = "";
    long createdTick, deadlineTick, finishedTick = -1, blockedSince = -1, waitUntil = -1, lastVerifyTick = -1;
    String blockReason = "";
    NavPos lastPartialEnd;
    long lastChunk = Long.MIN_VALUE;
    boolean holding;
    boolean cacheHit;
    String lastEvent = "requested";
    final MovementState movement = new MovementState();
    StuckDetector stuck;
    final TemporaryObstacleTracker temporary = new TemporaryObstacleTracker();
    final SessionMetrics metrics = new SessionMetrics();
    Consumer<String> canceller = reason -> { };
    /** Update-frequency bucket assigned by the runtime, and a phase so buckets do not all fire on the same tick. */
    int interval = 1, phase;

    NavigationSession(PathRequest request, long tick, StuckDetector stuck) {
        this.request = request; this.createdTick = tick; this.stuck = stuck;
    }

    @Override public UUID id() { return id; }
    @Override public UUID npcId() { return request.npcId(); }
    @Override public PathState state() { return state; }
    @Override public NavigationFailure failure() { return failure; }
    @Override public String failureDetail() { return failureDetail; }
    @Override public NavPos destination() { return resolvedGoal != null ? resolvedGoal : request.goal(); }
    @Override public double progress() {
        if (path == null || path.size() < 2) return state == PathState.COMPLETED ? 1.0D : 0.0D;
        return Math.min(1.0D, (double) movement.index() / (path.size() - 1));
    }
    @Override public void cancel(String reason) { canceller.accept(reason); }

    public PathRequest request() { return request; }
    public NavigationPath path() { return path; }
    public int index() { return movement.index(); }
    public SessionMetrics metrics() { return metrics; }
    public String lastEvent() { return lastEvent; }
    public int recalculations() { return recalculations; }
    public int recoveryAttempts() { return recoveryAttempts; }
    public long createdTick() { return createdTick; }
    public long deadlineTick() { return deadlineTick; }
    public boolean cacheHit() { return cacheHit; }
    public String blockReason() { return blockReason; }
    public boolean holding() { return holding; }
    public MovementState movement() { return movement; }
    public PathSearch search() { return search; }
    public Set<NavPos> blockedNodes() { return Set.copyOf(blocked); }
}
