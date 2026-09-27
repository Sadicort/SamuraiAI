package yadi.samuraiai.living.core;

import yadi.samuraiai.event.NpcEvent;

/**
 * Root of every living-world event (world, village, economy, quest, calendar, family). They travel on the existing
 * {@link yadi.samuraiai.event.NPCEventBus} like every other SamuraiAI fact; {@link #domain()} names the engine that
 * published it and {@link #minute()} the Deiliora minute it refers to.
 */
public interface LivingEvent extends NpcEvent {
    String domain();

    long minute();

    @Override default String getName() { return getClass().getSimpleName(); }
}
