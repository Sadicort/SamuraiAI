package yadi.samuraiai.event.npc;

import yadi.samuraiai.event.NpcEvent;
import yadi.samuraiai.npc.NPCIdentity;

public record NPCSpawnedEvent(NPCIdentity identity) implements NpcEvent {

    @Override
    public String getName() {
        return "NPC_SPAWNED";
    }
}
