package yadi.samuraiai.living.world.events_api;

/** The world's population total changed (as reported by the villages). */
public record WorldPopulationChangedEvent(long minute, int total, int regions, int settlements) implements WorldEngineEvent { }
