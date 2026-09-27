package yadi.samuraiai.living.family.events;

/** A family joined a clan. */
public record ClanJoinedEvent(long minute, java.util.UUID clanId, java.util.UUID familyId) implements FamilyEngineEvent { }
