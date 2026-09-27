package yadi.samuraiai.living.economy.events;

/** A caravan was lost (quests can come from it). */
public record CaravanLostEvent(long minute, java.util.UUID caravanId, java.util.UUID origin, java.util.UUID destination, String reason) implements EconomyEngineEvent { }
