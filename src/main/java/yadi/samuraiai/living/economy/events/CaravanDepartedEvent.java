package yadi.samuraiai.living.economy.events;

/** A caravan left. */
public record CaravanDepartedEvent(long minute, java.util.UUID caravanId, java.util.UUID origin, java.util.UUID destination, double distance) implements EconomyEngineEvent { }
