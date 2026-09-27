package yadi.samuraiai.living.family.events;

/** A birth was registered. */
public record BirthRegisteredEvent(long minute, java.util.UUID person, String name, java.util.UUID familyId, java.util.UUID parentA, java.util.UUID parentB, int generation) implements FamilyEngineEvent { }
