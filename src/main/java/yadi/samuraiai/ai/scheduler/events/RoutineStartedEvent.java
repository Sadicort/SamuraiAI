package yadi.samuraiai.ai.scheduler.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** An NPC began a routine or a response, in this priority layer, for this reason. */
public record RoutineStartedEvent(UUID npcId, String routine, String layer, String place, String reason) implements NpcEvent {
    @Override public String getName() { return "RoutineStartedEvent"; }
}
