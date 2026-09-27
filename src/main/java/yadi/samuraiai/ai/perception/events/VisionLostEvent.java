package yadi.samuraiai.ai.perception.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** Perceptual evidence published on the NPC event bus. A fact about what was perceived, never a command. */
public record VisionLostEvent(UUID npcId, UUID targetId, String kind, String name, double lastX, double lastY, double lastZ) implements NpcEvent {
    @Override public String getName() { return "VisionLostEvent"; }
}
