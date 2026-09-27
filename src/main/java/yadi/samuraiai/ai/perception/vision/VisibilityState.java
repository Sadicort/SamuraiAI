package yadi.samuraiai.ai.perception.vision;

/** How well a target is seen. Deliberately not binary: partial cover and lost-but-remembered targets matter. */
public enum VisibilityState {
    VISIBLE, PARTIAL, OBSTRUCTED, LOST, MEMORY_ONLY;

    /** True while there is actual sight of the target right now. */
    public boolean seen() { return this == VISIBLE || this == PARTIAL; }
}
