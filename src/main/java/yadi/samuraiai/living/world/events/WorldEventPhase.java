package yadi.samuraiai.living.world.events;

/** The life of a world event: PREPARATION → START → DEVELOPMENT → END → CONSEQUENCES, then CLOSED (archived). */
public enum WorldEventPhase {
    PREPARATION, START, DEVELOPMENT, END, CONSEQUENCES, CLOSED;

    public boolean running() { return this == START || this == DEVELOPMENT; }
    public boolean open() { return this != CLOSED; }
}
