package yadi.samuraiai.ai.scheduler.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** An NPC's mood shifted enough to change what it feels like doing. */
public record EmotionPriorityChangedEvent(UUID npcId, String from, String to, double strength) implements NpcEvent {
    @Override public String getName() { return "EmotionPriorityChangedEvent"; }
}
