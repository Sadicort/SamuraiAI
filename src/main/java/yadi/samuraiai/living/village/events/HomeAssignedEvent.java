package yadi.samuraiai.living.village.events;

/** A citizen was given a home. */
public record HomeAssignedEvent(long minute, java.util.UUID villageId, java.util.UUID npcId, java.util.UUID buildingId, String room) implements VillageEngineEvent { }
