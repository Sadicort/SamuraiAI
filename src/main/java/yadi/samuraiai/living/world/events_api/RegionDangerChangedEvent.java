package yadi.samuraiai.living.world.events_api;

/** A region's danger changed noticeably (wildlife, bandits, war). */
public record RegionDangerChangedEvent(long minute, java.util.UUID regionId, double before, double after) implements WorldEngineEvent { }
