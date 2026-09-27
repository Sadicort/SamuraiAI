package yadi.samuraiai.living.economy.events;

/** Goods were lost from a store (fire, looting). */
public record WarehouseLossEvent(long minute, java.util.UUID settlementId, String cause, java.util.Map<String, Double> lost) implements EconomyEngineEvent { }
