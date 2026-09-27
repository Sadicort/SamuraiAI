package yadi.samuraiai.ai.scheduler.optimize;

import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;

/** How closely an NPC is looked after: the closer to a player (or the more urgent), the more often its schedule is evaluated. */
public enum TickBucket {
    VISIBLE, NEARBY, ZONE_ACTIVE, FAR, SLEEPING, HIBERNATING;

    public int interval(SchedulerSettings s) {
        return switch (this) {
            case VISIBLE -> s.visibleInterval();
            case NEARBY -> s.nearbyInterval();
            case ZONE_ACTIVE -> s.zoneActiveInterval();
            case FAR -> s.farInterval();
            case SLEEPING -> s.sleepingInterval();
            case HIBERNATING -> s.hibernatingInterval();
        };
    }
}
