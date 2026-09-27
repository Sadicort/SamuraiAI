package yadi.samuraiai.ai.knowledge.events;

import java.util.UUID;

public record RumorSpreadEvent(UUID ownerId, UUID traceId, UUID rumorId, UUID toNpc, int hop, double credibility, boolean transformed) implements KnowledgeEvent {
    @Override public String getName() { return "RumorSpreadEvent"; }
}
