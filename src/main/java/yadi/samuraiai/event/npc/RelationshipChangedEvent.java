package yadi.samuraiai.event.npc;

import yadi.samuraiai.event.NpcEvent;

import java.util.UUID;

public record RelationshipChangedEvent(UUID ownerId, UUID targetId) implements NpcEvent {

    @Override
    public String getName() {
        return "RELATIONSHIP_CHANGED";
    }
}
