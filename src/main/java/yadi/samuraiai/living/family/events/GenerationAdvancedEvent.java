package yadi.samuraiai.living.family.events;

/** A family's first member of a new generation was born. */
public record GenerationAdvancedEvent(long minute, java.util.UUID familyId, int generation) implements FamilyEngineEvent { }
