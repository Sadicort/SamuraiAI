package yadi.samuraiai.event.npc;

import yadi.samuraiai.event.NpcEvent;

import java.util.UUID;

public record MemoryCreatedEvent(UUID npcId, String summary) implements NpcEvent {

    @Override
    public String getName() {
        return "MEMORY_CREATED";
    }
}
