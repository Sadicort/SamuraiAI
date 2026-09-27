package yadi.samuraiai.ai.memory.events;

import java.util.UUID;

/** An existing memory changed (reinforced by a repeat, strengthened, reinterpreted...). */
public record MemoryUpdatedEvent(UUID npcId, UUID traceId, UUID memoryId, String reason) implements MemoryEvent {
    @Override public String getName() { return "MemoryUpdatedEvent"; }
}
