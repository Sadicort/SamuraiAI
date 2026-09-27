package yadi.samuraiai.living.economy.events;

/** Taxes collected in a settlement over a day. */
public record TaxCollectedEvent(long minute, java.util.UUID settlementId, double market, double tithe, double levy) implements EconomyEngineEvent { }
