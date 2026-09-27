package yadi.samuraiai.living.quest.events;

/** A multi-quest campaign began. */
public record CampaignStartedEvent(long minute, java.util.UUID campaignId, String type, String title) implements QuestEngineEvent { }
