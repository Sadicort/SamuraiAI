package yadi.samuraiai.ai.emotion.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** The family of events the emotion engine publishes. */
public interface EmotionEvent extends NpcEvent {
    UUID npcId();
    UUID traceId();
}
