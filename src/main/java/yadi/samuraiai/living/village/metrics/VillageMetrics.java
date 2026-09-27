package yadi.samuraiai.living.village.metrics;

import java.util.concurrent.atomic.AtomicLong;

/** Volume and cost of the Living Villages Engine: villages, citizens, schedules, events, security changes, visitors, bias queries. */
public final class VillageMetrics {
    public final AtomicLong villagesCreated = new AtomicLong(), buildingsRegistered = new AtomicLong(), citizensJoined = new AtomicLong(), citizensLeft = new AtomicLong(),
            professionsAssigned = new AtomicLong(), homesAssigned = new AtomicLong(), securityChanges = new AtomicLong(), eventsStarted = new AtomicLong(), visitorsArrived = new AtomicLong(),
            marketOpenings = new AtomicLong(), rituals = new AtomicLong(), shiftChanges = new AtomicLong(), biasQueries = new AtomicLong(), villageUpdates = new AtomicLong(), simulations = new AtomicLong();
    public final AtomicLong updateNanos = new AtomicLong(), biasNanos = new AtomicLong();

    public record Snapshot(long villagesCreated, long buildingsRegistered, long citizensJoined, long citizensLeft, long professionsAssigned, long homesAssigned, long securityChanges,
                           long eventsStarted, long visitorsArrived, long marketOpenings, long rituals, long shiftChanges, long biasQueries, long villageUpdates, long simulations,
                           double updateMicros, double biasMicros) { }

    public Snapshot snapshot() {
        return new Snapshot(villagesCreated.get(), buildingsRegistered.get(), citizensJoined.get(), citizensLeft.get(), professionsAssigned.get(), homesAssigned.get(), securityChanges.get(),
                eventsStarted.get(), visitorsArrived.get(), marketOpenings.get(), rituals.get(), shiftChanges.get(), biasQueries.get(), villageUpdates.get(), simulations.get(),
                updateNanos.get() / 1000.0 / Math.max(1, villageUpdates.get()), biasNanos.get() / 1000.0 / Math.max(1, biasQueries.get()));
    }

    public void reset() {
        for (AtomicLong l : new AtomicLong[] {villagesCreated, buildingsRegistered, citizensJoined, citizensLeft, professionsAssigned, homesAssigned, securityChanges, eventsStarted,
                visitorsArrived, marketOpenings, rituals, shiftChanges, biasQueries, villageUpdates, simulations, updateNanos, biasNanos}) l.set(0);
    }
}
