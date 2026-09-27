package yadi.samuraiai.ai.knowledge.events;

import java.util.UUID;

/** A rumour was resolved: confirmed as true, or rejected as false. */
public record RumorConfirmedEvent(UUID ownerId, UUID traceId, UUID rumorId, boolean confirmed, String by) implements KnowledgeEvent {
    @Override public String getName() { return "RumorConfirmedEvent"; }
}
