package yadi.samuraiai.ai.knowledge.events;

import java.util.UUID;

public record KnowledgeCreatedEvent(UUID ownerId, UUID traceId, UUID knowledgeId, String type, String predicate, String subject, String state, double confidence) implements KnowledgeEvent {
    @Override public String getName() { return "KnowledgeCreatedEvent"; }
}
