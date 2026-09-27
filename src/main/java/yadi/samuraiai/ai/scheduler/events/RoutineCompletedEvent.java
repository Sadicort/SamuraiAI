package yadi.samuraiai.ai.scheduler.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** An NPC finished a routine, or gave it up (outcome says which). */
public record RoutineCompletedEvent(UUID npcId, String routine, int performedTicks, String outcome) implements NpcEvent {
    @Override public String getName() { return "RoutineCompletedEvent"; }
}
