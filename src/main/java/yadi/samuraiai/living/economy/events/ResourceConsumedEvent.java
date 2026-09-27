package yadi.samuraiai.living.economy.events;

/** A settlement consumed goods during a step; {@code shortfall} is what it needed and did not have. */
public record ResourceConsumedEvent(long minute, java.util.UUID settlementId, java.util.Map<String, Double> consumed, java.util.Map<String, Double> shortfall, double foodCoverDays) implements EconomyEngineEvent { }
