package yadi.samuraiai.living.family.events;

/** A clan has a new leading family. */
public record ClanLeaderChangedEvent(long minute, java.util.UUID clanId, java.util.UUID previous, java.util.UUID leaderFamily) implements FamilyEngineEvent { }
