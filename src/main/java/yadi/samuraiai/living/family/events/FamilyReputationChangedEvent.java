package yadi.samuraiai.living.family.events;

/** A family's reputation or honour changed, with its cause. */
public record FamilyReputationChangedEvent(long minute, java.util.UUID familyId, double before, double after, String cause) implements FamilyEngineEvent { }
