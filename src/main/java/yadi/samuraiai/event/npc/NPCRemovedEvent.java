package yadi.samuraiai.event.npc;

import yadi.samuraiai.event.NpcEvent;

import java.util.UUID;

public record NPCRemovedEvent(UUID npcId, String reason) implements NpcEvent {

    @Override
    public String getName() {
        return "NPC_REMOVED";
    }
}
