package yadi.samuraiai.ai.relationship.events;

import java.util.UUID;

public record RespectChangedEvent(UUID npcId, UUID traceId, UUID targetId, double oldValue, double newValue, String oldLevel, String newLevel, String reason) implements RelationshipEvent {
    @Override public String getName() { return "RespectChangedEvent"; }
}
