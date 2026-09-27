package yadi.samuraiai.living.world.events_api;

import yadi.samuraiai.living.core.LivingEvent;

/** Every event published by the Living World Engine. */
public interface WorldEngineEvent extends LivingEvent {
    @Override default String domain() { return "world"; }
}
