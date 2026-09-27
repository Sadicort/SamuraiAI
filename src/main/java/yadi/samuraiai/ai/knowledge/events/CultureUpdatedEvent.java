package yadi.samuraiai.ai.knowledge.events;

import java.util.UUID;

public record CultureUpdatedEvent(UUID ownerId, UUID traceId, String community, String tradition, double strength, String reason) implements KnowledgeEvent {
    @Override public String getName() { return "CultureUpdatedEvent"; }
}
