package yadi.samuraiai.event.npc;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

public record AIRequestFailedEvent(UUID npcId, UUID requestId, String reason) implements NpcEvent {
    @Override public String getName() { return "AIRequestFailedEvent"; }
}
