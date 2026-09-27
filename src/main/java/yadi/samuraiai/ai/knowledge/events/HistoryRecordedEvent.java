package yadi.samuraiai.ai.knowledge.events;

import java.util.UUID;

public record HistoryRecordedEvent(UUID ownerId, UUID traceId, UUID eventId, String type, String community, double significance) implements KnowledgeEvent {
    @Override public String getName() { return "HistoryRecordedEvent"; }
}
