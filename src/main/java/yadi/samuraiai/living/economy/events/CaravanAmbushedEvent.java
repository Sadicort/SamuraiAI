package yadi.samuraiai.living.economy.events;

/** A caravan was ambushed on a dangerous road. */
public record CaravanAmbushedEvent(long minute, java.util.UUID caravanId, java.util.UUID edge, double lossFraction, boolean lost) implements EconomyEngineEvent { }
