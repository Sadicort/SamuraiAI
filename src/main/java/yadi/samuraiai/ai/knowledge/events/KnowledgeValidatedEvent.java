package yadi.samuraiai.ai.knowledge.events;

import java.util.UUID;

public record KnowledgeValidatedEvent(UUID ownerId, UUID traceId, UUID knowledgeId, String oldState, String newState, String reason) implements KnowledgeEvent {
    @Override public String getName() { return "KnowledgeValidatedEvent"; }
}
