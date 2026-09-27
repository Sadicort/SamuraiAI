package yadi.samuraiai.ai.scheduler.events;

import yadi.samuraiai.event.NpcEvent;

/** A world calendar event (market day, festival...) began or ended. */
public record WorldScheduleEvent(String name, boolean started, String period, long dayNumber) implements NpcEvent {
    @Override public String getName() { return "WorldScheduleEvent"; }
}
