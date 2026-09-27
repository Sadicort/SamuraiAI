package yadi.samuraiai.ai.emotion.events;

import java.util.UUID;

public record EmotionUpdatedEvent(UUID npcId, UUID traceId, UUID emotionId, String kind, double oldIntensity, double newIntensity, String reason) implements EmotionEvent {
    @Override public String getName() { return "EmotionUpdatedEvent"; }
}
