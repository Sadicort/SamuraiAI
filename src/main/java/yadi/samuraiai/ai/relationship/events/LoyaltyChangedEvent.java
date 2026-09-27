package yadi.samuraiai.ai.relationship.events;

import java.util.UUID;

public record LoyaltyChangedEvent(UUID npcId, UUID traceId, UUID targetId, double oldValue, double newValue, boolean broken, String reason) implements RelationshipEvent {
    @Override public String getName() { return "LoyaltyChangedEvent"; }
}
