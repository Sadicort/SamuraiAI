package yadi.samuraiai.living.calendar.events;

/** A festival began today. */
public record FestivalStartedEvent(long minute, String festivalId, String name, int days, int year) implements CalendarEngineEvent { }
