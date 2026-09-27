package yadi.samuraiai.living.family.events;

/** Someone became part of a family (born, adopted, married in). */
public record FamilyMemberJoinedEvent(long minute, java.util.UUID familyId, java.util.UUID person, String name, String relation) implements FamilyEngineEvent { }
