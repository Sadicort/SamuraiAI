package yadi.samuraiai.living.economy.events;

/** A settlement has more of a resource than it needs: it can export. */
public record SurplusStartedEvent(long minute, java.util.UUID settlementId, String resource, double coverDays) implements EconomyEngineEvent { }
