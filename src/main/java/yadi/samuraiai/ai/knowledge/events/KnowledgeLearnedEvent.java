package yadi.samuraiai.ai.knowledge.events;

import java.util.UUID;

public record KnowledgeLearnedEvent(UUID ownerId, UUID traceId, UUID knowledgeId, String method, UUID sourceId) implements KnowledgeEvent {
    @Override public String getName() { return "KnowledgeLearnedEvent"; }
}
