package yadi.samuraiai.ai.navigation.metrics;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import yadi.samuraiai.ai.navigation.engine.NavigationFailure;

/** Global navigation counters. Cheap atomics so any thread (a command, a report writer) can read them. */
public final class NavigationMetrics {
    private final AtomicLong requests = new AtomicLong(), created = new AtomicLong(), cacheHits = new AtomicLong(),
            completed = new AtomicLong(), failed = new AtomicLong(), cancelled = new AtomicLong(),
            recalculations = new AtomicLong(), blocks = new AtomicLong(), stuck = new AtomicLong(),
            obstacles = new AtomicLong(), doorsOpened = new AtomicLong(), doorsClosed = new AtomicLong(),
            jumps = new AtomicLong(), nodesExpanded = new AtomicLong(), searchNanos = new AtomicLong(),
            movementNanos = new AtomicLong(), chunksTraversed = new AtomicLong(), distanceMillis = new AtomicLong(),
            costMillis = new AtomicLong(), ticks = new AtomicLong(), tickNanos = new AtomicLong(), maxTickNanos = new AtomicLong(),
            budgetExhausted = new AtomicLong(), recoveries = new AtomicLong();
    private final Map<NavigationFailure, AtomicLong> failures = new EnumMap<>(NavigationFailure.class);
    private volatile int activeSessions;

    public NavigationMetrics() { for (NavigationFailure f : NavigationFailure.values()) failures.put(f, new AtomicLong()); }

    public void requested() { requests.incrementAndGet(); }
    public void pathCreated(boolean cached) { created.incrementAndGet(); if (cached) cacheHits.incrementAndGet(); }
    public void blocked() { blocks.incrementAndGet(); }
    public void recalculated() { recalculations.incrementAndGet(); }
    public void stuck() { stuck.incrementAndGet(); }
    public void recovery() { recoveries.incrementAndGet(); }
    public void budgetExhausted() { budgetExhausted.incrementAndGet(); }
    public void activeSessions(int value) { activeSessions = value; }
    public void tick(long nanos) {
        ticks.incrementAndGet(); tickNanos.addAndGet(nanos); maxTickNanos.accumulateAndGet(nanos, Math::max);
    }

    /** Folds a finished session into the totals. */
    public void finished(SessionMetrics s, boolean success, boolean wasCancelled, NavigationFailure failure) {
        if (wasCancelled) cancelled.incrementAndGet();
        else if (success) completed.incrementAndGet();
        else { failed.incrementAndGet(); failures.get(failure == null ? NavigationFailure.INTERNAL_ERROR : failure).incrementAndGet(); }
        obstacles.addAndGet(s.obstacles); doorsOpened.addAndGet(s.doorsOpened); doorsClosed.addAndGet(s.doorsClosed);
        jumps.addAndGet(s.jumps); nodesExpanded.addAndGet(s.nodesExpanded); searchNanos.addAndGet(s.searchNanos);
        movementNanos.addAndGet(s.movementNanos); chunksTraversed.addAndGet(s.chunksEntered);
        distanceMillis.addAndGet((long) (s.distance * 1000)); costMillis.addAndGet((long) (s.cost * 1000));
    }

    public Snapshot snapshot() {
        Map<NavigationFailure, Long> byReason = new EnumMap<>(NavigationFailure.class);
        failures.forEach((k, v) -> { if (v.get() > 0) byReason.put(k, v.get()); });
        long t = ticks.get();
        return new Snapshot(requests.get(), created.get(), cacheHits.get(), completed.get(), failed.get(), cancelled.get(),
                recalculations.get(), blocks.get(), stuck.get(), obstacles.get(), doorsOpened.get(), doorsClosed.get(),
                jumps.get(), nodesExpanded.get(), searchNanos.get() / 1_000_000D, movementNanos.get() / 1_000_000D,
                chunksTraversed.get(), distanceMillis.get() / 1000D, costMillis.get() / 1000D, activeSessions,
                t == 0 ? 0 : tickNanos.get() / 1000D / t, maxTickNanos.get() / 1000D, budgetExhausted.get(), recoveries.get(), byReason);
    }

    public void reset() {
        for (AtomicLong v : new AtomicLong[]{requests, created, cacheHits, completed, failed, cancelled, recalculations, blocks, stuck,
                obstacles, doorsOpened, doorsClosed, jumps, nodesExpanded, searchNanos, movementNanos, chunksTraversed,
                distanceMillis, costMillis, ticks, tickNanos, maxTickNanos, budgetExhausted, recoveries}) v.set(0);
        failures.values().forEach(v -> v.set(0));
        activeSessions = 0;
    }

    public record Snapshot(long requests, long created, long cacheHits, long completed, long failed, long cancelled,
                           long recalculations, long blocks, long stuck, long obstacles, long doorsOpened, long doorsClosed,
                           long jumps, long nodesExpanded, double searchMillis, double movementMillis, long chunksTraversed,
                           double distance, double cost, int activeSessions, double averageTickMicros, double maxTickMicros,
                           long budgetExhausted, long recoveries, Map<NavigationFailure, Long> failuresByReason) { }
}
