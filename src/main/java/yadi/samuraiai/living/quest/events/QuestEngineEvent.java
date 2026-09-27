package yadi.samuraiai.living.quest.events;

import yadi.samuraiai.living.core.LivingEvent;

/** Every event published by the Dynamic Quest Engine. */
public interface QuestEngineEvent extends LivingEvent {
    @Override default String domain() { return "quest"; }
}
