package yadi.samuraiai.living.economy.events;

/** A scarcity ended. */
public record ScarcityEndedEvent(long minute, java.util.UUID settlementId, String resource, double coverDays) implements EconomyEngineEvent { }
