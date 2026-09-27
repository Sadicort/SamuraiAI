package yadi.samuraiai.living.economy.events;

/** A resource became scarce in a settlement (quests and trade react to it). */
public record ScarcityStartedEvent(long minute, java.util.UUID settlementId, String resource, double coverDays) implements EconomyEngineEvent { }
