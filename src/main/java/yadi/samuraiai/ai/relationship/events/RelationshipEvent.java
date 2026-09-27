package yadi.samuraiai.ai.relationship.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** The family of events the relationship engine publishes. */
public interface RelationshipEvent extends NpcEvent {
    UUID npcId();
    UUID traceId();
    UUID targetId();
}
