package yadi.samuraiai.ai.knowledge.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** The family of events the knowledge and society engines publish. The owner is an NPC or a community. */
public interface KnowledgeEvent extends NpcEvent {
    UUID ownerId();
    UUID traceId();
}
