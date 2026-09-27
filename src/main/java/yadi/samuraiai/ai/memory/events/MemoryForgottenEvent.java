package yadi.samuraiai.ai.memory.events;

import java.util.UUID;

/** A memory faded away (or was removed by an administrator). */
public record MemoryForgottenEvent(UUID npcId, UUID traceId, UUID memoryId, String kind, String reason) implements MemoryEvent {
    @Override public String getName() { return "MemoryForgottenEvent"; }
}
