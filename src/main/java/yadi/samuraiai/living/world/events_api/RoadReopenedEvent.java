package yadi.samuraiai.living.world.events_api;

/** A blocked road is open again. */
public record RoadReopenedEvent(long minute, java.util.UUID edgeId) implements WorldEngineEvent { }
