package yadi.samuraiai.living.calendar.events;

/** Today is the anniversary of a registered date; {@code years} since it happened. */
public record AnniversaryEvent(long minute, java.util.UUID anniversaryId, String kind, String subject, String title, int years) implements CalendarEngineEvent { }
