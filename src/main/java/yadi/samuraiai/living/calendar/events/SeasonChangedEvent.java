package yadi.samuraiai.living.calendar.events;

/** The season changed; agriculture, prices and routines recalculate on it instead of polling. */
public record SeasonChangedEvent(long minute, yadi.samuraiai.living.core.Season from, yadi.samuraiai.living.core.Season to, int year) implements CalendarEngineEvent { }
