package yadi.samuraiai.ai.relationship.events;

import java.util.UUID;

public record FriendshipLevelChangedEvent(UUID npcId, UUID traceId, UUID targetId, String oldStage, String newStage) implements RelationshipEvent {
    @Override public String getName() { return "FriendshipLevelChangedEvent"; }
}
