package yadi.samuraiai.ai.memory.events;

import java.util.UUID;

/** A new memory was kept. */
public record MemoryCreatedEvent(UUID npcId, UUID traceId, UUID memoryId, String kind, String importance, String type) implements MemoryEvent {
    @Override public String getName() { return "MemoryCreatedEvent"; }
}
