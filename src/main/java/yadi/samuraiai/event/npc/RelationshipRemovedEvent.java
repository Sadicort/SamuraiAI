package yadi.samuraiai.event.npc;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

public record RelationshipRemovedEvent(UUID ownerId, UUID targetId) implements NpcEvent {
    @Override public String getName() { return "RelationshipRemovedEvent"; }
}
