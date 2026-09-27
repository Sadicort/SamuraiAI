package yadi.samuraiai.ai.scheduler.interrupt;

/** What happens to a routine that something more important interrupts. */
public enum InterruptPolicy {
    /** Dropped for good; the routine planner decides again afterwards. */
    CANCEL,
    /** Progress is kept and resumed once the interruption is over, for a limited time. */
    PAUSE,
    /** Like PAUSE, but frozen: it only resumes if the interruption was short, and lapses quickly otherwise. */
    SUSPEND,
    /** Continues exactly where it left off, however long the interruption lasted. */
    RESUME,
    /** Starts over from the beginning once the interruption is over. */
    RESTART
}
