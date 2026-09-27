package yadi.samuraiai.living.village.events;

/** The village market opened. */
public record MarketOpenedEvent(long minute, java.util.UUID villageId, int stalls) implements VillageEngineEvent { }
