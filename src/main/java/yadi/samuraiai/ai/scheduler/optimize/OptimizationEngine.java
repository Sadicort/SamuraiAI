package yadi.samuraiai.ai.scheduler.optimize;

import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;

/** Decides which {@link TickBucket} an NPC belongs to from its distance to the nearest player, whether it sleeps and whether anything is urgent. */
public final class OptimizationEngine {
    private final SchedulerSettings settings;

    public OptimizationEngine(SchedulerSettings settings) { this.settings = settings; }

    public TickBucket classify(double playerDistance, boolean asleep, boolean urgent, boolean zoneActive) {
        if (urgent || playerDistance <= settings.visibleDistance()) return TickBucket.VISIBLE;
        if (asleep) return playerDistance > settings.hibernateDistance() ? TickBucket.HIBERNATING : TickBucket.SLEEPING;
        if (playerDistance <= settings.nearbyDistance()) return TickBucket.NEARBY;
        if (playerDistance <= settings.zoneActiveDistance() || zoneActive) return TickBucket.ZONE_ACTIVE;
        return playerDistance > settings.hibernateDistance() ? TickBucket.HIBERNATING : TickBucket.FAR;
    }

    /** The evaluation interval for a bucket at the current population: the crowd scale stretches every interval except the visible one's. */
    public int interval(TickBucket bucket, double crowdScale) {
        int base = bucket.interval(settings);
        return bucket == TickBucket.VISIBLE ? base : (int) Math.max(1, Math.round(base * crowdScale));
    }

    /** 1.0 while the population is below the crowd threshold, growing linearly (up to the configured scale) as it exceeds it. */
    public double crowdScale(int npcs) {
        if (npcs <= settings.crowdThreshold()) return 1.0D;
        double over = npcs / (double) settings.crowdThreshold() - 1.0D;
        return Math.min(settings.crowdIntervalScale() * 2.0D, 1.0D + over * (settings.crowdIntervalScale() - 1.0D));
    }
}
