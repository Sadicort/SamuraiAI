package yadi.samuraiai.ai.navigation.events;

import java.util.UUID;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.event.NpcEvent;

/** Published on the NPC event bus; a fact about navigation, never a command. */
public record DoorOpenedEvent(UUID npcId, NavPos door) implements NpcEvent {
    @Override public String getName() { return "DoorOpenedEvent"; }
}
