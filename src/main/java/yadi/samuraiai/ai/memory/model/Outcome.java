package yadi.samuraiai.ai.memory.model;

/** How an experience turned out for the NPC. */
public enum Outcome { POSITIVE, NEGATIVE, NEUTRAL, MIXED;
    public double sign() { return this == POSITIVE ? 1.0 : this == NEGATIVE ? -1.0 : 0.0; }
}
