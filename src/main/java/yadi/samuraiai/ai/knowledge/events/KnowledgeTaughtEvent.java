package yadi.samuraiai.ai.knowledge.events;

import java.util.UUID;

public record KnowledgeTaughtEvent(UUID ownerId, UUID traceId, UUID studentId, UUID knowledgeId, double quality, boolean learned) implements KnowledgeEvent {
    @Override public String getName() { return "KnowledgeTaughtEvent"; }
}
