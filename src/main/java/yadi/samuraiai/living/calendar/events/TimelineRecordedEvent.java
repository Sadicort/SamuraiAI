package yadi.samuraiai.living.calendar.events;

/** A fact entered the world timeline. */
public record TimelineRecordedEvent(long minute, java.util.UUID entryId, String category, String title, double significance, java.util.Set<String> scopes) implements CalendarEngineEvent { }
