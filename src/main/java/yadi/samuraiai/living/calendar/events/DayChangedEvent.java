package yadi.samuraiai.living.calendar.events;

/** A new Deiliora day began. {@code skippedDays} > 0 when a long jump was compressed (only the last days were processed one by one). */
public record DayChangedEvent(long minute, long dayIndex, int year, int month, int day, String weekday, long skippedDays) implements CalendarEngineEvent { }
