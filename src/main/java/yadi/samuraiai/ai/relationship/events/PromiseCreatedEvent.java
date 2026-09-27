package yadi.samuraiai.ai.relationship.events;

import java.util.UUID;

public record PromiseCreatedEvent(UUID npcId, UUID traceId, UUID targetId, UUID promiseId, String kind, boolean publicPromise) implements RelationshipEvent {
    @Override public String getName() { return "PromiseCreatedEvent"; }
}
