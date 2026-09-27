package yadi.samuraiai.ai.scheduler.events;

import yadi.samuraiai.event.NpcEvent;

/** The scheduler changed how much work it does (crowd scaling or deferral because a budget ran out). */
public record SchedulerOptimizationEvent(String reason, int npcs, int deferred, double intervalScale) implements NpcEvent {
    @Override public String getName() { return "SchedulerOptimizationEvent"; }
}
