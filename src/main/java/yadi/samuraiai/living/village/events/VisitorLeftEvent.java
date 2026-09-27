package yadi.samuraiai.living.village.events;

/** A visitor left. */
public record VisitorLeftEvent(long minute, java.util.UUID villageId, java.util.UUID visitorId, String kind, String name) implements VillageEngineEvent { }
