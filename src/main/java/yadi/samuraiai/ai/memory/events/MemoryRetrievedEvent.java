package yadi.samuraiai.ai.memory.events;

import java.util.UUID;

/** An explicit retrieval returned memories. */
public record MemoryRetrievedEvent(UUID npcId, UUID traceId, int count, String mode, double micros) implements MemoryEvent {
    @Override public String getName() { return "MemoryRetrievedEvent"; }
}
