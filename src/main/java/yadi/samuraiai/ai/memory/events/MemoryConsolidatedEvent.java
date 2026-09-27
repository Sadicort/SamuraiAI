package yadi.samuraiai.ai.memory.events;

import java.util.UUID;

/** A consolidation pass settled temporary memories (and possibly merged some). */
public record MemoryConsolidatedEvent(UUID npcId, UUID traceId, int promoted, int merged, boolean sleep) implements MemoryEvent {
    @Override public String getName() { return "MemoryConsolidatedEvent"; }
}
