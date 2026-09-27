package yadi.samuraiai.living.calendar.events;

/** A holiday began today. */
public record HolidayEvent(long minute, String holidayId, String name, String kind, java.util.List<String> tags) implements CalendarEngineEvent { }
