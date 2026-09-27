package yadi.samuraiai.living.economy.events;

/** A caravan is waiting in front of a blocked road. */
public record CaravanDelayedEvent(long minute, java.util.UUID caravanId, java.util.UUID edge, String reason) implements EconomyEngineEvent { }
