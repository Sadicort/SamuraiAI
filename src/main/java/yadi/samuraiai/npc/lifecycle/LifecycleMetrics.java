package yadi.samuraiai.npc.lifecycle;

import java.util.Map;

public record LifecycleMetrics(long transitions, long rejectedTransitions,
                               Map<NPCLifecycleState, Long> currentStates) {
    public LifecycleMetrics { currentStates = Map.copyOf(currentStates); }
}
