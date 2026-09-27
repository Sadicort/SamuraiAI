package yadi.samuraiai.living.quest.events;

/** A consequence of a quest was applied to the world. */
public record QuestConsequenceAppliedEvent(long minute, java.util.UUID questId, String kind, String target, String detail) implements QuestEngineEvent { }
