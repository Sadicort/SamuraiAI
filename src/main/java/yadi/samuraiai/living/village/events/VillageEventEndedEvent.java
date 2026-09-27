package yadi.samuraiai.living.village.events;

/** A village event ended. */
public record VillageEventEndedEvent(long minute, java.util.UUID villageId, java.util.UUID eventId, String kind, String title) implements VillageEngineEvent { }
