package yadi.samuraiai.living.economy.events;

/** A caravan reached its destination and sold its goods. */
public record CaravanArrivedEvent(long minute, java.util.UUID caravanId, java.util.UUID destination, java.util.Map<String, Double> delivered, double saleValue) implements EconomyEngineEvent { }
