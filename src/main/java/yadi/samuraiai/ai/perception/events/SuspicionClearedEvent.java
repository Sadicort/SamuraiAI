package yadi.samuraiai.ai.perception.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** Perceptual evidence published on the NPC event bus. A fact about what was perceived, never a command. */
public record SuspicionClearedEvent(UUID npcId) implements NpcEvent {
    @Override public String getName() { return "SuspicionClearedEvent"; }
}
