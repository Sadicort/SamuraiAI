package yadi.samuraiai.ai.scheduler.events;

import yadi.samuraiai.event.NpcEvent;

/** The world moved into another part of the day. */
public record TimelineChangedEvent(String from, String to, long dayNumber) implements NpcEvent {
    @Override public String getName() { return "TimelineChangedEvent"; }
}
