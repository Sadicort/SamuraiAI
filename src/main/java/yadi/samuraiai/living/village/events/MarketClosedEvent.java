package yadi.samuraiai.living.village.events;

/** The village market closed. */
public record MarketClosedEvent(long minute, java.util.UUID villageId, String reason) implements VillageEngineEvent { }
