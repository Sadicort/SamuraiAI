package yadi.samuraiai.living.quest.events;

/** A reward was given to a player. */
public record QuestRewardGivenEvent(long minute, java.util.UUID questId, java.util.UUID player, String kind, String detail) implements QuestEngineEvent { }
