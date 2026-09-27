package yadi.samuraiai.living.quest.events;

/** A quest was completed; rewards and consequences follow. */
public record QuestCompletedEvent(long minute, java.util.UUID questId, String title, String path, java.util.Set<java.util.UUID> players) implements QuestEngineEvent { }
