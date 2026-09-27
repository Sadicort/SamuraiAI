package yadi.samuraiai.living.village.events;

/** A person became a citizen of a village. */
public record CitizenJoinedEvent(long minute, java.util.UUID villageId, java.util.UUID npcId, String name, String profession, boolean embodied) implements VillageEngineEvent { }
