package yadi.samuraiai.living.family.events;

/** An estate was passed on. */
public record InheritanceCompletedEvent(long minute, java.util.UUID inheritance, java.util.UUID owner, String status, int transfers) implements FamilyEngineEvent { }
