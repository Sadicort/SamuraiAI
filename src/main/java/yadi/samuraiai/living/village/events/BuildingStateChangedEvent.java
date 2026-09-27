package yadi.samuraiai.living.village.events;

/** A building was built, damaged, destroyed or abandoned. */
public record BuildingStateChangedEvent(long minute, java.util.UUID villageId, java.util.UUID buildingId, String from, String to, String reason) implements VillageEngineEvent { }
