package yadi.samuraiai.ai.navigation.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** Published on the NPC event bus; a fact about navigation, never a command. */
public record PathCancelledEvent(UUID npcId, UUID pathId, String reason) implements NpcEvent {
    @Override public String getName() { return "PathCancelledEvent"; }
}
