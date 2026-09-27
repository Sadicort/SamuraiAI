package yadi.samuraiai.ai.scheduler.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** A group has a new leader (the previous one is null when the group had none). */
public record GroupLeaderChangedEvent(String groupId, UUID previous, UUID leader, int size, double score) implements NpcEvent {
    @Override public String getName() { return "GroupLeaderChangedEvent"; }
}
