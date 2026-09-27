package yadi.samuraiai.living.calendar.events;

/** A new month began. */
public record MonthChangedEvent(long minute, int year, int month, String monthName) implements CalendarEngineEvent { }
