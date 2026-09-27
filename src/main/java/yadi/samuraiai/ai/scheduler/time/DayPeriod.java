package yadi.samuraiai.ai.scheduler.time;

/** The six parts of the scheduling day. Where each begins is configuration, not code. */
public enum DayPeriod {
    MORNING(false), AFTERNOON(false), EVENING(false), NIGHT(true), LATE_NIGHT(true), DAWN(true);

    private final boolean dark;
    DayPeriod(boolean dark) { this.dark = dark; }
    /** Whether ordinary people are expected to be indoors. */
    public boolean dark() { return dark; }
}
