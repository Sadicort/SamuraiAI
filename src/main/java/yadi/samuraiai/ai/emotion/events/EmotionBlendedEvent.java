package yadi.samuraiai.ai.emotion.events;

import java.util.UUID;

public record EmotionBlendedEvent(UUID npcId, UUID traceId, String label, double valence, double arousal) implements EmotionEvent {
    @Override public String getName() { return "EmotionBlendedEvent"; }
}
