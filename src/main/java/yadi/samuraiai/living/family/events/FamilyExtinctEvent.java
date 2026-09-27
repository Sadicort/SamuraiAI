package yadi.samuraiai.living.family.events;

/** A family has no living members: its history remains. */
public record FamilyExtinctEvent(long minute, java.util.UUID familyId, String name) implements FamilyEngineEvent { }
