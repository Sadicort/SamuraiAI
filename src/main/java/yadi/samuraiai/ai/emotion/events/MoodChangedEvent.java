package yadi.samuraiai.ai.emotion.events;

import java.util.UUID;

public record MoodChangedEvent(UUID npcId, UUID traceId, String oldMood, String newMood, String dominantEmotion) implements EmotionEvent {
    @Override public String getName() { return "MoodChangedEvent"; }
}
