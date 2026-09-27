package yadi.samuraiai.ai.knowledge.events;

import java.util.UUID;

public record RumorCreatedEvent(UUID ownerId, UUID traceId, UUID rumorId, UUID originMemory, String subject, String claim, double credibility) implements KnowledgeEvent {
    @Override public String getName() { return "RumorCreatedEvent"; }
}
