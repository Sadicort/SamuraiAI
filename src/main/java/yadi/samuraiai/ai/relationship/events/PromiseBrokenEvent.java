package yadi.samuraiai.ai.relationship.events;

import java.util.UUID;

public record PromiseBrokenEvent(UUID npcId, UUID traceId, UUID targetId, UUID promiseId, String kind, String status) implements RelationshipEvent {
    @Override public String getName() { return "PromiseBrokenEvent"; }
}
