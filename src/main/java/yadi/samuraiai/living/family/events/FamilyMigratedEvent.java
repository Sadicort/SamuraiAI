package yadi.samuraiai.living.family.events;

/** A family moved to another village. */
public record FamilyMigratedEvent(long minute, java.util.UUID familyId, java.util.UUID from, java.util.UUID to) implements FamilyEngineEvent { }
