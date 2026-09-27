package yadi.samuraiai.living.world.metrics;

import java.util.concurrent.atomic.AtomicLong;

/** Volume and cost of the Living World Engine: regions and settlements created, streaming, simulation, events, roads, resources taken. */
public final class WorldMetrics {
    public final AtomicLong ticks = new AtomicLong(), regionsCreated = new AtomicLong(), settlementsFounded = new AtomicLong(), streamingUpdates = new AtomicLong(),
            activations = new AtomicLong(), sleeps = new AtomicLong(), catchUps = new AtomicLong(), eventsScheduled = new AtomicLong(), eventTransitions = new AtomicLong(),
            roadsBuilt = new AtomicLong(), roadsBlocked = new AtomicLong(), routesPlanned = new AtomicLong(), extractions = new AtomicLong(), hunts = new AtomicLong();
    public final AtomicLong tickNanos = new AtomicLong(), maxTickNanos = new AtomicLong(), dailyNanos = new AtomicLong();

    public record Snapshot(long ticks, long regionsCreated, long settlementsFounded, long streamingUpdates, long activations, long sleeps, long catchUps, long eventsScheduled,
                           long eventTransitions, long roadsBuilt, long roadsBlocked, long routesPlanned, long extractions, long hunts, double tickMicros, double maxTickMicros, double dailyMillis) { }

    public void tick(long nanos) { ticks.incrementAndGet(); tickNanos.addAndGet(nanos); maxTickNanos.accumulateAndGet(nanos, Math::max); }

    public Snapshot snapshot() {
        return new Snapshot(ticks.get(), regionsCreated.get(), settlementsFounded.get(), streamingUpdates.get(), activations.get(), sleeps.get(), catchUps.get(), eventsScheduled.get(),
                eventTransitions.get(), roadsBuilt.get(), roadsBlocked.get(), routesPlanned.get(), extractions.get(), hunts.get(),
                tickNanos.get() / 1000.0 / Math.max(1, ticks.get()), maxTickNanos.get() / 1000.0, dailyNanos.get() / 1e6);
    }

    public void reset() {
        for (AtomicLong l : new AtomicLong[] {ticks, regionsCreated, settlementsFounded, streamingUpdates, activations, sleeps, catchUps, eventsScheduled, eventTransitions, roadsBuilt,
                roadsBlocked, routesPlanned, extractions, hunts, tickNanos, maxTickNanos, dailyNanos}) l.set(0);
    }
}
