package yadi.samuraiai.ai.relationship.events;

import java.util.UUID;

public record HonorChangedEvent(UUID npcId, UUID traceId, UUID targetId, double oldValue, double newValue, String category, String reason) implements RelationshipEvent {
    @Override public String getName() { return "HonorChangedEvent"; }
}
