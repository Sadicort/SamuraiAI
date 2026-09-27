package yadi.samuraiai.ai.perception.events;

import java.util.UUID;
import yadi.samuraiai.ai.perception.awareness.ThreatLevel;
import yadi.samuraiai.event.NpcEvent;

/** Perceptual evidence published on the NPC event bus. A fact about what was perceived, never a command. */
public record ThreatDetectedEvent(UUID npcId, String key, ThreatLevel level, double score, String category, String label, double x, double y, double z) implements NpcEvent {
    @Override public String getName() { return "ThreatDetectedEvent"; }
}
