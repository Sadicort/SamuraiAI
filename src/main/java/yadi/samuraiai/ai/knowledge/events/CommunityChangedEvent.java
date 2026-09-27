package yadi.samuraiai.ai.knowledge.events;

import java.util.UUID;

public record CommunityChangedEvent(UUID ownerId, UUID traceId, String community, UUID member, String change) implements KnowledgeEvent {
    @Override public String getName() { return "CommunityChangedEvent"; }
}
