package yadi.samuraiai.ai.navigation.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** Published on the NPC event bus; a fact about navigation, never a command. */
public record PathStartedEvent(UUID npcId, UUID pathId) implements NpcEvent {
    @Override public String getName() { return "PathStartedEvent"; }
}
