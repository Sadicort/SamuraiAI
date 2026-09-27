package yadi.samuraiai.living.world.events_api;

/** A region of the world map was created (first settlement, first NPC or first visit there). */
public record RegionCreatedEvent(long minute, java.util.UUID regionId, String key, String name, String type) implements WorldEngineEvent { }
