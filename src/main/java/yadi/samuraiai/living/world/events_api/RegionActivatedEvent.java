package yadi.samuraiai.living.world.events_api;

/** A region came near a player: it is now simulated in detail (and was caught up if it had slept). */
public record RegionActivatedEvent(long minute, java.util.UUID regionId, String name, String from, String to) implements WorldEngineEvent { }
