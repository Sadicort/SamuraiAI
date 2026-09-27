package yadi.samuraiai.ai.emotion.events;

import java.util.UUID;

public record EmotionRecoveredEvent(UUID npcId, UUID traceId, UUID emotionId, String kind, long durationTicks) implements EmotionEvent {
    @Override public String getName() { return "EmotionRecoveredEvent"; }
}
