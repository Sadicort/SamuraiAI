package yadi.samuraiai.living.economy.events;

/** A sale in a market (or to a player). */
public record TradeCompletedEvent(long minute, java.util.UUID settlementId, java.util.UUID seller, java.util.UUID buyer, String resource, double quantity, double unitPrice) implements EconomyEngineEvent { }
