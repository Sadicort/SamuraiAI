package yadi.samuraiai.living.economy.events;

import yadi.samuraiai.living.core.LivingEvent;

/** Every event published by the Economy & Trade Engine. */
public interface EconomyEngineEvent extends LivingEvent {
    @Override default String domain() { return "economy"; }
}
