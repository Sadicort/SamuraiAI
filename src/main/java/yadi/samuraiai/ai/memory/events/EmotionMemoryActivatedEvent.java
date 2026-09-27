package yadi.samuraiai.ai.memory.events;

import java.util.UUID;

/** A memory reawakened a feeling (an emotional echo): the place or person that hurt, or helped, is here again. */
public record EmotionMemoryActivatedEvent(UUID npcId, UUID traceId, UUID memoryId, String emotion, double intensity) implements MemoryEvent {
    @Override public String getName() { return "EmotionMemoryActivatedEvent"; }
}
