package yadi.samuraiai.living.family.events;

/** A family was founded (or recognised for an NPC who arrived without one). */
public record FamilyCreatedEvent(long minute, java.util.UUID familyId, String name, java.util.UUID village, int generations) implements FamilyEngineEvent { }
