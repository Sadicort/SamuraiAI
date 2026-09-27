package yadi.samuraiai.living.family.events;

/** A person went missing, moved away, died or became a historical record. */
public record LifeStateChangedEvent(long minute, java.util.UUID person, String from, String to, String cause) implements FamilyEngineEvent { }
