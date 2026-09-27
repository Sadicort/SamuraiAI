package yadi.samuraiai.living.calendar.events;

/** The phase of the day changed (dawn, morning, noon...). */
public record DayPhaseChangedEvent(long minute, yadi.samuraiai.living.core.DayPhase from, yadi.samuraiai.living.core.DayPhase to) implements CalendarEngineEvent { }
