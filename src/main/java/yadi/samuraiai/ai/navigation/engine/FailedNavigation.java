package yadi.samuraiai.ai.navigation.engine;

import java.util.UUID;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.pathfinding.PathState;

/** Handle for a request that could not even start (no body, unknown dimension). Already FAILED, nothing to cancel. */
public record FailedNavigation(UUID npcId, NavigationFailure failure, String failureDetail, NavPos destination) implements NavigationHandle {
    private static final UUID ID = new UUID(0L, 0L);
    @Override public UUID id() { return ID; }
    @Override public PathState state() { return PathState.FAILED; }
    @Override public double progress() { return 0.0D; }
    @Override public void cancel(String reason) { }
}
