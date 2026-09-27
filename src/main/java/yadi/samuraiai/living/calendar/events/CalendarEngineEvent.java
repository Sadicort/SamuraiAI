package yadi.samuraiai.living.calendar.events;

import yadi.samuraiai.living.core.LivingEvent;

/** Every event published by the Calendar Engine. */
public interface CalendarEngineEvent extends LivingEvent {
    @Override default String domain() { return "calendar"; }
}
