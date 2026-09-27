package yadi.samuraiai.living.family.events;

/** A family left a clan (by choice, or because it has no living members left to represent it). */
public record ClanLeftEvent(long minute, java.util.UUID clanId, java.util.UUID familyId, String reason) implements FamilyEngineEvent { }
