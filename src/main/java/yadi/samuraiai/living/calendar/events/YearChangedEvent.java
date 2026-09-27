package yadi.samuraiai.living.calendar.events;

/** A new year began (New Year's day). */
public record YearChangedEvent(long minute, int year) implements CalendarEngineEvent { }
