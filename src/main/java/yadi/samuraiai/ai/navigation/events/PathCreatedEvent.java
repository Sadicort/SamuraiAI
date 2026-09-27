package yadi.samuraiai.ai.navigation.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** Published on the NPC event bus; a fact about navigation, never a command. */
public record PathCreatedEvent(UUID npcId, UUID pathId, int nodes, double cost, boolean cached, boolean partial) implements NpcEvent {
    @Override public String getName() { return "PathCreatedEvent"; }
}
