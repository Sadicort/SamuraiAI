package yadi.samuraiai.living.economy.events;

/** A merchant bought goods and formed a caravan. */
public record CaravanCreatedEvent(long minute, java.util.UUID caravanId, java.util.UUID origin, java.util.UUID destination, String type, java.util.Map<String, Double> cargo, double purchaseCost) implements EconomyEngineEvent { }
