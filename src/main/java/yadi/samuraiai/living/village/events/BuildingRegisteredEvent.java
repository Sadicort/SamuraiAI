package yadi.samuraiai.living.village.events;

/** A building joined the village. */
public record BuildingRegisteredEvent(long minute, java.util.UUID villageId, java.util.UUID buildingId, String kind, String name, String state) implements VillageEngineEvent { }
