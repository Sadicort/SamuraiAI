package yadi.samuraiai.living.village.events;

/** A temple ritual began or ended. */
public record TempleRitualEvent(long minute, java.util.UUID villageId, boolean started, String ritual) implements VillageEngineEvent { }
