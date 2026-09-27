package yadi.samuraiai.living.family.events;

/** A family split into a new branch. */
public record FamilyBranchCreatedEvent(long minute, java.util.UUID parentFamily, java.util.UUID branch, java.util.UUID founder, String reason) implements FamilyEngineEvent { }
