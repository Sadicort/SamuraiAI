package yadi.samuraiai.living.calendar.events;

/** A festival ended. */
public record FestivalEndedEvent(long minute, String festivalId, String name, int year) implements CalendarEngineEvent { }
