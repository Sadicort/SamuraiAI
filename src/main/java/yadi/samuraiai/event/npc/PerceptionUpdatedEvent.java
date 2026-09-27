package yadi.samuraiai.event.npc;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

public record PerceptionUpdatedEvent(UUID npcId, yadi.samuraiai.context.WorldContext world) implements NpcEvent {
    @Override public String getName() { return "PerceptionUpdatedEvent"; }
}
