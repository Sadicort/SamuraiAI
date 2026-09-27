package yadi.samuraiai.living.calendar.events;

/** The world clock went backwards (a command, a restored backup); the calendar refused to rewind. */
public record TimeRewindRejectedEvent(long minute, long attempts, String detail) implements CalendarEngineEvent { }
