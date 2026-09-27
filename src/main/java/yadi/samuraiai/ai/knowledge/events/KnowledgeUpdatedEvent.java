package yadi.samuraiai.ai.knowledge.events;

import java.util.UUID;

public record KnowledgeUpdatedEvent(UUID ownerId, UUID traceId, UUID knowledgeId, String reason, double oldConfidence, double newConfidence) implements KnowledgeEvent {
    @Override public String getName() { return "KnowledgeUpdatedEvent"; }
}
