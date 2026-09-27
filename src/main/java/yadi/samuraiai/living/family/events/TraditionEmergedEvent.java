package yadi.samuraiai.living.family.events;

/** Repeated history became a family tradition. */
public record TraditionEmergedEvent(long minute, java.util.UUID familyId, String tradition) implements FamilyEngineEvent { }
