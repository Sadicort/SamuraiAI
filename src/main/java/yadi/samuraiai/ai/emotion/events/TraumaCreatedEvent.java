package yadi.samuraiai.ai.emotion.events;

import java.util.UUID;

public record TraumaCreatedEvent(UUID npcId, UUID traceId, UUID traumaId, String origin, double intensity, boolean reinforced) implements EmotionEvent {
    @Override public String getName() { return "TraumaCreatedEvent"; }
}
