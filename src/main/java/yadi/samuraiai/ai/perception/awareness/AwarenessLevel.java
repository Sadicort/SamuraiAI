package yadi.samuraiai.ai.perception.awareness;

/** How switched-on the NPC is, from oblivious to locked on a target. Ordered: later values are more alert. */
public enum AwarenessLevel {
    UNAWARE, AWARE, ALERT, SEARCHING, TRACKING, FOCUSED;

    public boolean atLeast(AwarenessLevel other) { return compareTo(other) >= 0; }
    /** Levels at which the NPC is actively hunting for or following something. */
    public boolean engaged() { return this == SEARCHING || this == TRACKING || this == FOCUSED; }
}
