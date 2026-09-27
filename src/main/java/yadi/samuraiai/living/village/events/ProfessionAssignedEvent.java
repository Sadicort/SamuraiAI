package yadi.samuraiai.living.village.events;

/** A citizen took up a profession (by type, by the village's needs, by choice). */
public record ProfessionAssignedEvent(long minute, java.util.UUID villageId, java.util.UUID npcId, String profession, String reason) implements VillageEngineEvent { }
