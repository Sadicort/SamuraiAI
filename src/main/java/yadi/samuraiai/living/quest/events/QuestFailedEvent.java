package yadi.samuraiai.living.quest.events;

/** A quest failed, expired, was abandoned or was resolved by the world. */
public record QuestFailedEvent(long minute, java.util.UUID questId, String title, String state, String reason) implements QuestEngineEvent { }
