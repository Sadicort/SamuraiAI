package yadi.samuraiai.ai.scheduler.events;

import java.util.List;
import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** Competing demands on an NPC were settled: what won and what lost, with the reasons. */
public record BehaviorConflictResolvedEvent(UUID npcId, String winner, String source, List<String> losers) implements NpcEvent {
    public BehaviorConflictResolvedEvent { losers = List.copyOf(losers); }
    @Override public String getName() { return "BehaviorConflictResolvedEvent"; }
}
