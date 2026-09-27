package yadi.samuraiai.living.village.events;

/** A visitor arrived. */
public record VisitorArrivedEvent(long minute, java.util.UUID villageId, java.util.UUID visitorId, String kind, String name, String purpose) implements VillageEngineEvent { }
