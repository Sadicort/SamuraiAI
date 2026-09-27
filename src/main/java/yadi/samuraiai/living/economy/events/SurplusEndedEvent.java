package yadi.samuraiai.living.economy.events;

/** A surplus ended. */
public record SurplusEndedEvent(long minute, java.util.UUID settlementId, String resource, double coverDays) implements EconomyEngineEvent { }
