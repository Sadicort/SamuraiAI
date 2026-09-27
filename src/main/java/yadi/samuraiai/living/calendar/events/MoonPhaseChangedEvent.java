package yadi.samuraiai.living.calendar.events;

/** The moon entered a new phase. */
public record MoonPhaseChangedEvent(long minute, yadi.samuraiai.living.core.MoonPhase from, yadi.samuraiai.living.core.MoonPhase to) implements CalendarEngineEvent { }
