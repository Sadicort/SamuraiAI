package yadi.samuraiai.living.world.events_api;

/** A settlement was abandoned, destroyed, ruined or restored. */
public record SettlementStatusChangedEvent(long minute, java.util.UUID settlementId, String from, String to) implements WorldEngineEvent { }
