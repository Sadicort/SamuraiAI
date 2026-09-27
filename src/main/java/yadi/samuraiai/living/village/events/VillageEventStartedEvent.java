package yadi.samuraiai.living.village.events;

/** A village event began (market, rain, attack, celebration, procession...). */
public record VillageEventStartedEvent(long minute, java.util.UUID villageId, java.util.UUID eventId, String kind, String title, String source) implements VillageEngineEvent { }
