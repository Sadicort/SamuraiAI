package yadi.samuraiai.ai.emotion.events;

import java.util.UUID;

public record EmotionCreatedEvent(UUID npcId, UUID traceId, UUID emotionId, String kind, double intensity, String source, UUID memoryId) implements EmotionEvent {
    @Override public String getName() { return "EmotionCreatedEvent"; }
}
