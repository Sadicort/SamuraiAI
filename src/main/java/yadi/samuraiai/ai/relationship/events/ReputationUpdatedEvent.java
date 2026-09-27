package yadi.samuraiai.ai.relationship.events;

import java.util.UUID;

public record ReputationUpdatedEvent(UUID npcId, UUID traceId, UUID targetId, String label, String scope, double score, double confidence) implements RelationshipEvent {
    @Override public String getName() { return "ReputationUpdatedEvent"; }
}
