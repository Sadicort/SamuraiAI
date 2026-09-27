package yadi.samuraiai.living.economy.events;

/** Goods were produced in a settlement during a simulation step (what, how much, by how many workers). */
public record ResourceProducedEvent(long minute, java.util.UUID settlementId, java.util.Map<String, Double> produced, int workers) implements EconomyEngineEvent { }
