package yadi.samuraiai.ai.navigation.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** Published on the NPC event bus; a fact about navigation, never a command. */
public record PathCompletedEvent(UUID npcId, UUID pathId, long ticks, double distance) implements NpcEvent {
    @Override public String getName() { return "PathCompletedEvent"; }
}
