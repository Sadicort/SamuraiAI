package yadi.samuraiai.event.npc;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

public record AIRequestCompletedEvent(UUID npcId, UUID requestId) implements NpcEvent {
    @Override public String getName() { return "AIRequestCompletedEvent"; }
}
