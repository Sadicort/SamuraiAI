package yadi.samuraiai.living.village.events;

/** A citizen left the village (moved away, went missing, died). */
public record CitizenLeftEvent(long minute, java.util.UUID villageId, java.util.UUID npcId, String name, String status, String reason) implements VillageEngineEvent { }
