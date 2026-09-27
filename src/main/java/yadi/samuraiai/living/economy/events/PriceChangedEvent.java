package yadi.samuraiai.living.economy.events;

/** A market price moved by more than the configured threshold. */
public record PriceChangedEvent(long minute, java.util.UUID settlementId, String resource, double before, double after, String mainFactor) implements EconomyEngineEvent { }
