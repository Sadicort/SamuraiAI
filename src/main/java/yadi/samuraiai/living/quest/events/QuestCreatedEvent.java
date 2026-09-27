package yadi.samuraiai.living.quest.events;

/** A quest was generated from a world condition (it is being offered). */
public record QuestCreatedEvent(long minute, java.util.UUID questId, String template, String title, String condition, java.util.UUID settlementId, java.util.UUID giver) implements QuestEngineEvent { }
