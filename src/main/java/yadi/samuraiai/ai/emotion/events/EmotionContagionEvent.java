package yadi.samuraiai.ai.emotion.events;

import java.util.UUID;

public record EmotionContagionEvent(UUID npcId, UUID traceId, UUID targetId, String kind, double intensity) implements EmotionEvent {
    @Override public String getName() { return "EmotionContagionEvent"; }
}
