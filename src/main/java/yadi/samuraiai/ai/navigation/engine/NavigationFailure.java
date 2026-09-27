package yadi.samuraiai.ai.navigation.engine;

/** Explicit reason a session ended in FAILED. Behaviors read this to choose their own fallback. */
public enum NavigationFailure {
    NONE, DESTINATION_INVALID, DESTINATION_UNREACHABLE, START_INVALID, CHUNK_UNLOADED, SEARCH_LIMIT, NO_BODY, BODY_LOST,
    TIMEOUT, STUCK, BLOCKED, DANGER, DIMENSION_CHANGED, TOO_MANY_RECALCULATIONS, DOOR_LOCKED, INTERNAL_ERROR
}
