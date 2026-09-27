package yadi.samuraiai.event.npc;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

public record NPCDeactivatedEvent(UUID npcId, String reason) implements NpcEvent {
    @Override public String getName() { return "NPCDeactivatedEvent"; }
}
