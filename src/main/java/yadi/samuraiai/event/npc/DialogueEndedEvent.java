package yadi.samuraiai.event.npc;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

public record DialogueEndedEvent(UUID npcId, UUID conversationId, UUID turnId, boolean success) implements NpcEvent {
    @Override public String getName() { return "DialogueEndedEvent"; }
}
