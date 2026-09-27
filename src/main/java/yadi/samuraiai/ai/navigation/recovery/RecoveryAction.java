package yadi.samuraiai.ai.navigation.recovery;

/** One rung of the recovery ladder, from gentlest to most drastic. */
public enum RecoveryAction {
    /** Jump in place / toward the goal to clear a lip or shake off a corner. */
    JUMP_NUDGE,
    /** Same goal, new search from where the walker actually is. */
    RECALCULATE,
    /** New search that refuses the nodes that just failed. */
    ALTERNATIVE_ROUTE,
    /** Retrace the last nodes and try the approach again. */
    BACKTRACK,
    /** Stand still briefly; the obstacle may leave. */
    WAIT,
    /** Relocate to a validated node ahead on the path. Only when explicitly allowed. */
    SAFE_TELEPORT,
    /** Give up and let the behavior choose something else. */
    CANCEL
}
