package yadi.samuraiai.ai.emotion.events;

import java.util.UUID;

public record TraumaRecoveredEvent(UUID npcId, UUID traceId, UUID traumaId, String origin) implements EmotionEvent {
    @Override public String getName() { return "TraumaRecoveredEvent"; }
}
