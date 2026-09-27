package yadi.samuraiai.living.world.events_api;

/** A road was laid between two points of the road graph. */
public record RoadCreatedEvent(long minute, java.util.UUID edgeId, java.util.UUID fromNode, java.util.UUID toNode, double length) implements WorldEngineEvent { }
