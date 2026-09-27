package yadi.samuraiai.living.family.events;

/** A person's legacy was established (with its causes). */
public record LegacyCreatedEvent(long minute, java.util.UUID person, double total, int causes) implements FamilyEngineEvent { }
