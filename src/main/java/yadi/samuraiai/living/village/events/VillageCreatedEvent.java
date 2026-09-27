package yadi.samuraiai.living.village.events;

/** A village was created (linked to its world settlement and knowledge community). */
public record VillageCreatedEvent(long minute, java.util.UUID villageId, String name, java.util.UUID regionId, String culture, boolean planned) implements VillageEngineEvent { }
