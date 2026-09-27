package yadi.samuraiai.ai.relationship.events;

import java.util.UUID;

public record PromiseFulfilledEvent(UUID npcId, UUID traceId, UUID targetId, UUID promiseId, String kind) implements RelationshipEvent {
    @Override public String getName() { return "PromiseFulfilledEvent"; }
}
