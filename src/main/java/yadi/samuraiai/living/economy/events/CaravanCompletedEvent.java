package yadi.samuraiai.living.economy.events;

/** A caravan came home; its merchant is free again. */
public record CaravanCompletedEvent(long minute, java.util.UUID caravanId, java.util.UUID merchant, double profit) implements EconomyEngineEvent { }
