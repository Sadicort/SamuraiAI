package yadi.samuraiai.living.quest.events;

/** A campaign was completed or failed. */
public record CampaignEndedEvent(long minute, java.util.UUID campaignId, String state) implements QuestEngineEvent { }
