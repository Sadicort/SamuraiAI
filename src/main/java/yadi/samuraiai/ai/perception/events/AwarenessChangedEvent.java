package yadi.samuraiai.ai.perception.events;

import java.util.UUID;
import yadi.samuraiai.ai.perception.awareness.AwarenessLevel;
import yadi.samuraiai.event.NpcEvent;

/** Perceptual evidence published on the NPC event bus. A fact about what was perceived, never a command. */
public record AwarenessChangedEvent(UUID npcId, AwarenessLevel previous, AwarenessLevel next, String reason) implements NpcEvent {
    @Override public String getName() { return "AwarenessChangedEvent"; }
}
