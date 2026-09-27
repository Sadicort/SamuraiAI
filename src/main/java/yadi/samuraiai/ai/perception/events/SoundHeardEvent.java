package yadi.samuraiai.ai.perception.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** Perceptual evidence published on the NPC event bus. A fact about what was perceived, never a command. */
public record SoundHeardEvent(UUID npcId, String category, double x, double y, double z, String direction, double intensity, double distance, double uncertainty, UUID source) implements NpcEvent {
    @Override public String getName() { return "SoundHeardEvent"; }
}
