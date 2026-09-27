package yadi.samuraiai.ai.relationship.events;

import java.util.UUID;

public record RelationshipCreatedEvent(UUID npcId, UUID traceId, UUID targetId, String targetName, String kind) implements RelationshipEvent {
    @Override public String getName() { return "RelationshipCreatedEvent"; }
}
