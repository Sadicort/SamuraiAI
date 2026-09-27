package yadi.samuraiai.ai.memory.events;

import java.util.List;
import java.util.UUID;

/** Similar memories were fused into one. */
public record MemoryMergedEvent(UUID npcId, UUID traceId, UUID survivorId, List<UUID> mergedIds) implements MemoryEvent {
    public MemoryMergedEvent { mergedIds = List.copyOf(mergedIds); }
    @Override public String getName() { return "MemoryMergedEvent"; }
}
