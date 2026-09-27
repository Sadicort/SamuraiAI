package yadi.samuraiai.ai.scheduler.routine;

import yadi.samuraiai.ai.scheduler.interrupt.InterruptPolicy;

/**
 * How one kind of routine behaves: how long it lasts, what an interruption does to it, what it costs or restores per tick
 * (energy, fatigue, focus, stress, motivation), how long before it is wanted again, and how social it is.
 */
public record RoutineProfile(RoutineType type, int minTicks, int maxTicks, InterruptPolicy policy,
                             double energy, double fatigue, double focus, double stress, double motivation,
                             int cooldownTicks, double socialLoad) {
    public RoutineProfile {
        minTicks = Math.max(1, minTicks);
        maxTicks = Math.max(minTicks, maxTicks);
        socialLoad = Math.max(0.0D, Math.min(1.0D, socialLoad));
        cooldownTicks = Math.max(0, cooldownTicks);
    }

    /** True when the routine restores energy instead of spending it. */
    public boolean restorative() { return energy > 0 && fatigue < 0; }
}
