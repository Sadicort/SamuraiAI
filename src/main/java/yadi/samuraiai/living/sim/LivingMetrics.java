package yadi.samuraiai.living.sim;

import java.util.concurrent.atomic.AtomicLong;

/** Cost of the living world as a whole and per tick bucket, event traffic between engines, reactions that failed. */
public final class LivingMetrics {
    public final AtomicLong ticks = new AtomicLong(), days = new AtomicLong(), events = new AtomicLong(), reactions = new AtomicLong(), reactionFailures = new AtomicLong(),
            droppedReactions = new AtomicLong(), conditions = new AtomicLong();
    public final AtomicLong tickNanos = new AtomicLong(), maxTickNanos = new AtomicLong(), calendarNanos = new AtomicLong(), worldNanos = new AtomicLong(), villageNanos = new AtomicLong(),
            economyNanos = new AtomicLong(), questNanos = new AtomicLong(), dayNanos = new AtomicLong();
    /** Time spent in each daily stage (world events, families, trade, quest conditions) and the most expensive single stage. */
    public final AtomicLong[] dayStageNanos = {new AtomicLong(), new AtomicLong(), new AtomicLong(), new AtomicLong()};
    public final AtomicLong maxDayStageNanos = new AtomicLong();
    public volatile String lastError = "";
    public volatile int auditProblems;

    public void tick(long nanos) { ticks.incrementAndGet(); tickNanos.addAndGet(nanos); maxTickNanos.accumulateAndGet(nanos, Math::max); }

    public double micros(AtomicLong nanos) { return nanos.get() / 1000.0 / Math.max(1, ticks.get()); }

    public void reset() {
        for (AtomicLong l : new AtomicLong[] {ticks, days, events, reactions, reactionFailures, droppedReactions, conditions, tickNanos, maxTickNanos, calendarNanos, worldNanos, villageNanos,
                economyNanos, questNanos, dayNanos, maxDayStageNanos}) l.set(0);
        for (AtomicLong l : dayStageNanos) l.set(0);
        lastError = "";
    }
}
