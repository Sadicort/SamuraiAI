package yadi.samuraiai.living.village.events;

import yadi.samuraiai.living.core.LivingEvent;

/** Every event published by the Living Villages Engine. */
public interface VillageEngineEvent extends LivingEvent {
    @Override default String domain() { return "village"; }
}
