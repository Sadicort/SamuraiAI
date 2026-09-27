package yadi.samuraiai.living.family.events;

/** A school or tradition has a new leader. */
public record LineageLeaderChangedEvent(long minute, java.util.UUID lineage, java.util.UUID previous, java.util.UUID leader, String how) implements FamilyEngineEvent { }
