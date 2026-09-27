package yadi.samuraiai.event.npc;

import yadi.samuraiai.event.NpcEvent;
import yadi.samuraiai.npc.lifecycle.*;

public record NPCLifecycleTransitionEvent(NPCLifecycleTransition transition) implements NpcEvent {
    @Override public String getName() { return "NPCLifecycleTransitionEvent"; }
}
