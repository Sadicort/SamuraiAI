package yadi.samuraiai.living.family.events;

/** A clan was founded by a family. */
public record ClanCreatedEvent(long minute, java.util.UUID clanId, String name, java.util.UUID founderFamily) implements FamilyEngineEvent { }
