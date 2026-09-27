package yadi.samuraiai.living.world.events_api;

/** A region that woke up was brought up to date in a bounded number of coarse steps. */
public record CatchUpCompletedEvent(long minute, java.util.UUID regionId, long elapsedMinutes, int steps) implements WorldEngineEvent { }
