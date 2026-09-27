package yadi.samuraiai.living.quest.events;

/** A quest changed: accepted, a path chosen, an objective done, a twist, a merge, progress. */
public record QuestUpdatedEvent(long minute, java.util.UUID questId, String change, String detail) implements QuestEngineEvent { }
