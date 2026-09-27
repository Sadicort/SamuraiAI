package yadi.samuraiai.living.family.events;

/** A clan has no member families left; it is remembered, not deleted. */
public record ClanDisbandedEvent(long minute, java.util.UUID clanId, String name) implements FamilyEngineEvent { }
