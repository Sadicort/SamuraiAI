package yadi.samuraiai.living.world.events_api;

/** No player is near a region any more: it degrades into abstract simulation. */
public record RegionSleepingEvent(long minute, java.util.UUID regionId, String name, String from, String to) implements WorldEngineEvent { }
