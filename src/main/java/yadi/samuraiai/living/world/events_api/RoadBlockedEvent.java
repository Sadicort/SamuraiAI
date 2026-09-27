package yadi.samuraiai.living.world.events_api;

/** A road was blocked; trade routes through it are invalidated by this event, not by polling. */
public record RoadBlockedEvent(long minute, java.util.UUID edgeId, String reason) implements WorldEngineEvent { }
