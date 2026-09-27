package yadi.samuraiai.event.npc;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

public record NPCActivatedEvent(UUID npcId) implements NpcEvent {
    @Override public String getName() { return "NPCActivatedEvent"; }
}
