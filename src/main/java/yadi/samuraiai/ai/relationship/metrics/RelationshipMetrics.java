package yadi.samuraiai.ai.relationship.metrics;

import java.util.concurrent.atomic.AtomicLong;

/** Counters and timings for the relationship engine. */
public final class RelationshipMetrics {
    public final AtomicLong applied = new AtomicLong(), created = new AtomicLong(), promisesMade = new AtomicLong(), promisesKept = new AtomicLong(),
            promisesBroken = new AtomicLong(), promisesExpired = new AtomicLong(), hearsayApplied = new AtomicLong(), hearsayIgnored = new AtomicLong(),
            decayed = new AtomicLong(), pruned = new AtomicLong(), errors = new AtomicLong(), friendshipChanges = new AtomicLong();
    public final AtomicLong applyNanos = new AtomicLong(), decayNanos = new AtomicLong();

    public record Snapshot(long applied, long created, long promisesMade, long promisesKept, long promisesBroken, long promisesExpired, long hearsayApplied,
                           long hearsayIgnored, long decayed, long pruned, long errors, long friendshipChanges, double applyMicros, double decayMicros) { }

    public Snapshot snapshot() {
        long a = Math.max(1, applied.get());
        return new Snapshot(applied.get(), created.get(), promisesMade.get(), promisesKept.get(), promisesBroken.get(), promisesExpired.get(), hearsayApplied.get(),
                hearsayIgnored.get(), decayed.get(), pruned.get(), errors.get(), friendshipChanges.get(), applyNanos.get() / 1000.0 / a, decayNanos.get() / 1000.0 / Math.max(1, decayed.get()));
    }

    public void reset() {
        for (AtomicLong l : new AtomicLong[] {applied, created, promisesMade, promisesKept, promisesBroken, promisesExpired, hearsayApplied, hearsayIgnored, decayed, pruned, errors,
                friendshipChanges, applyNanos, decayNanos}) l.set(0);
    }
}
