package yadi.samuraiai.living.quest.events;

/** A campaign moved to its next stage. */
public record CampaignAdvancedEvent(long minute, java.util.UUID campaignId, int stage, String template) implements QuestEngineEvent { }
