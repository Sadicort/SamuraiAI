package yadi.samuraiai.living.village.events;

/** The guard changed shift. */
public record GuardShiftChangedEvent(long minute, java.util.UUID villageId, boolean night, int onDuty) implements VillageEngineEvent { }
