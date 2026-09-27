package yadi.samuraiai.living.family.events;

import yadi.samuraiai.living.core.LivingEvent;

/** Every event published by the Family, Lineage & Legacy Engine. */
public interface FamilyEngineEvent extends LivingEvent {
    @Override default String domain() { return "family"; }
}
