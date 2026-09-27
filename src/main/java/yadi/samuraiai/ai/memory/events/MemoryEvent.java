package yadi.samuraiai.ai.memory.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** The family of events the memory engine publishes. {@link #traceId()} follows one world fact through every engine. */
public interface MemoryEvent extends NpcEvent {
    UUID npcId();
    UUID traceId();
}
