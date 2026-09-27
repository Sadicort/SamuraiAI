package yadi.samuraiai.ai.navigation.engine;

import java.util.UUID;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.pathfinding.PathState;

/**
 * What a behavior sees of a navigation request: state, outcome and a way to cancel. Nothing here lets a
 * behavior touch the path, the world or the body.
 */
public interface NavigationHandle {
    UUID id();
    UUID npcId();
    PathState state();
    NavigationFailure failure();
    String failureDetail();
    NavPos destination();
    /** 0..1 fraction of the current path already covered. */
    double progress();
    void cancel(String reason);

    default boolean done() { return state().terminal(); }
    default boolean succeeded() { return state() == PathState.COMPLETED; }
}
