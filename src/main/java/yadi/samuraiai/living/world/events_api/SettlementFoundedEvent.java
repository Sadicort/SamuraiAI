package yadi.samuraiai.living.world.events_api;

/** A settlement was founded. */
public record SettlementFoundedEvent(long minute, java.util.UUID settlementId, java.util.UUID regionId, String name, String type) implements WorldEngineEvent { }
