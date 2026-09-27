package yadi.samuraiai.living.family.events;

/** A family has a new head (after the succession pipeline). */
public record FamilyHeadChangedEvent(long minute, java.util.UUID familyId, java.util.UUID previous, java.util.UUID head, String reason) implements FamilyEngineEvent { }
