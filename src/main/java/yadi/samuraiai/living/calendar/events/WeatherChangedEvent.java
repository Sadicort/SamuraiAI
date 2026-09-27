package yadi.samuraiai.living.calendar.events;

/** The weather over a cell (a region or {@code world}) changed. */
public record WeatherChangedEvent(long minute, String cell, yadi.samuraiai.living.core.WeatherKind from, yadi.samuraiai.living.core.WeatherKind to, double intensity) implements CalendarEngineEvent { }
