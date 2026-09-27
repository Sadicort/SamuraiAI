package yadi.samuraiai.event.npc;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

public record DialogueStartedEvent(UUID npcId, UUID conversationId, UUID turnId) implements NpcEvent {
    @Override public String getName() { return "DialogueStartedEvent"; }
}
