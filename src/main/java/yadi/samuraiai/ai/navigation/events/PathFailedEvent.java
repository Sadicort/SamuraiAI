package yadi.samuraiai.ai.navigation.events;

import java.util.UUID;
import yadi.samuraiai.ai.navigation.engine.NavigationFailure;
import yadi.samuraiai.event.NpcEvent;

/** Published on the NPC event bus; a fact about navigation, never a command. */
public record PathFailedEvent(UUID npcId, UUID pathId, NavigationFailure reason, String detail) implements NpcEvent {
    @Override public String getName() { return "PathFailedEvent"; }
}
