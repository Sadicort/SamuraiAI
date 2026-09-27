package yadi.samuraiai.ai.knowledge.events;

import java.util.UUID;

public record DiscoveryEvent(UUID ownerId, UUID traceId, UUID knowledgeId, String category, String name, double importance) implements KnowledgeEvent {
    @Override public String getName() { return "DiscoveryEvent"; }
}
