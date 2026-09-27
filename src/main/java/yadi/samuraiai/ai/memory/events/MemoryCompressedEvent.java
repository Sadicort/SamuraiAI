package yadi.samuraiai.ai.memory.events;

import java.util.UUID;

/** Repetitive or old memories were reduced to save space. */
public record MemoryCompressedEvent(UUID npcId, UUID traceId, int before, int after, int detailReduced) implements MemoryEvent {
    @Override public String getName() { return "MemoryCompressedEvent"; }
}
