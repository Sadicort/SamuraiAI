package yadi.samuraiai.ai.scheduler.metrics;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import yadi.samuraiai.ai.scheduler.optimize.TickBucket;

/** Counters and timings of the scheduler, cheap enough to keep always on. */
public final class SchedulerMetrics {
    private final AtomicLong evaluations = new AtomicLong(), evaluationNanos = new AtomicLong(), maxEvaluationNanos = new AtomicLong();
    private final AtomicLong ticks = new AtomicLong(), tickNanos = new AtomicLong(), maxTickNanos = new AtomicLong();
    private final AtomicLong started = new AtomicLong(), completed = new AtomicLong(), interrupted = new AtomicLong(), resumed = new AtomicLong();
    private final AtomicLong lapsed = new AtomicLong(), conflicts = new AtomicLong(), leaderChanges = new AtomicLong(), timelineChanges = new AtomicLong();
    private final AtomicLong moodChanges = new AtomicLong(), emergencies = new AtomicLong(), deferred = new AtomicLong(), personalityChanges = new AtomicLong();
    private final AtomicLong alarms = new AtomicLong(), failures = new AtomicLong();
    private volatile int npcs, groups, zones;
    private volatile String lastFailure = "";
    private volatile double crowdScale = 1.0D;
    private final Map<TickBucket, Integer> buckets = new EnumMap<>(TickBucket.class);

    public void evaluation(long nanos) { evaluations.incrementAndGet(); evaluationNanos.addAndGet(nanos); maxEvaluationNanos.accumulateAndGet(nanos, Math::max); }
    public void tick(long nanos) { ticks.incrementAndGet(); tickNanos.addAndGet(nanos); maxTickNanos.accumulateAndGet(nanos, Math::max); }
    public void routineStarted() { started.incrementAndGet(); }
    public void routineCompleted() { completed.incrementAndGet(); }
    public void routineInterrupted() { interrupted.incrementAndGet(); }
    public void routineResumed() { resumed.incrementAndGet(); }
    public void routineLapsed() { lapsed.incrementAndGet(); }
    public void conflictResolved() { conflicts.incrementAndGet(); }
    public void leaderChanged() { leaderChanges.incrementAndGet(); }
    public void timelineChanged() { timelineChanges.incrementAndGet(); }
    public void moodChanged() { moodChanges.incrementAndGet(); }
    public void emergency() { emergencies.incrementAndGet(); }
    public void deferred(int count) { deferred.addAndGet(count); }
    public void personalityChanged() { personalityChanges.incrementAndGet(); }
    public void alarm() { alarms.incrementAndGet(); }
    public void failure(RuntimeException error) { failures.incrementAndGet(); lastFailure = error.toString(); }
    public String lastFailure() { return lastFailure; }

    public synchronized void population(int npcs, int groups, int zones, double crowdScale, Map<TickBucket, Integer> distribution) {
        this.npcs = npcs; this.groups = groups; this.zones = zones; this.crowdScale = crowdScale;
        buckets.clear(); buckets.putAll(distribution);
    }

    public synchronized Snapshot snapshot() {
        long ev = Math.max(1, evaluations.get()), tk = Math.max(1, ticks.get());
        return new Snapshot(evaluations.get(), evaluationNanos.get() / (double) ev / 1000.0D, maxEvaluationNanos.get() / 1000.0D,
                ticks.get(), tickNanos.get() / (double) tk / 1000.0D, maxTickNanos.get() / 1000.0D,
                started.get(), completed.get(), interrupted.get(), resumed.get(), lapsed.get(), conflicts.get(), leaderChanges.get(), timelineChanges.get(),
                moodChanges.get(), emergencies.get(), deferred.get(), personalityChanges.get(), alarms.get(), failures.get(),
                npcs, groups, zones, crowdScale, new EnumMap<>(buckets.isEmpty() ? new EnumMap<TickBucket, Integer>(TickBucket.class) : buckets));
    }

    public void reset() {
        for (AtomicLong l : new AtomicLong[]{evaluations, evaluationNanos, maxEvaluationNanos, ticks, tickNanos, maxTickNanos, started, completed, interrupted, resumed,
                lapsed, conflicts, leaderChanges, timelineChanges, moodChanges, emergencies, deferred, personalityChanges, alarms, failures}) l.set(0);
    }

    /** A consistent copy of every figure. */
    public record Snapshot(long evaluations, double averageEvaluationMicros, double maxEvaluationMicros, long ticks, double averageTickMicros, double maxTickMicros,
                           long routinesStarted, long routinesCompleted, long interruptions, long resumes, long lapsed, long conflicts, long leaderChanges,
                           long timelineChanges, long moodChanges, long emergencies, long deferred, long personalityChanges, long alarms, long failures,
                           int npcs, int groups, int zones, double crowdScale, Map<TickBucket, Integer> buckets) { }
}
