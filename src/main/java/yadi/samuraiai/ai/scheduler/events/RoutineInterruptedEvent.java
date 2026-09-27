package yadi.samuraiai.ai.scheduler.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** An NPC's routine was interrupted by something more important; the policy says whether it can be resumed. */
public record RoutineInterruptedEvent(UUID npcId, String routine, String by, String policy, int stackDepth) implements NpcEvent {
    @Override public String getName() { return "RoutineInterruptedEvent"; }
}
