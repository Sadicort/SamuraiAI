package yadi.samuraiai.event.npc;

import yadi.samuraiai.emotion.Emotion;
import yadi.samuraiai.event.NpcEvent;

import java.util.UUID;

public record EmotionChangedEvent(UUID npcId, Emotion emotion, int newValue) implements NpcEvent {

    @Override
    public String getName() {
        return "EMOTION_CHANGED";
    }
}
