package yadi.samuraiai.living.world.events_api;

/** A world event entered a new phase (PREPARATION, START, DEVELOPMENT, END, CONSEQUENCES, CLOSED). */
public record WorldEventPhaseEvent(long minute, java.util.UUID eventId, String type, String phase, java.util.UUID regionId, java.util.UUID settlementId, double severity, String title) implements WorldEngineEvent { }
