package yadi.samuraiai.living.village.events;

/** A village's security changed (PEACE, ALERT, DANGER, ATTACK, RECOVERY). */
public record VillageSecurityChangedEvent(long minute, java.util.UUID villageId, String from, String to, String reason, double threat) implements VillageEngineEvent { }
