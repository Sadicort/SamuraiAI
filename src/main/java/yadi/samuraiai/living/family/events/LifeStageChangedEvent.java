package yadi.samuraiai.living.family.events;

/** A person entered a new stage of life (from the calendar, not from ticks). */
public record LifeStageChangedEvent(long minute, java.util.UUID person, String from, String to) implements FamilyEngineEvent { }
