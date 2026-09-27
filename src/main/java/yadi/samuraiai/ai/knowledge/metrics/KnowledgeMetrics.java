package yadi.samuraiai.ai.knowledge.metrics;

import java.util.concurrent.atomic.AtomicLong;

/** Counters and timings for the knowledge and society engines. */
public final class KnowledgeMetrics {
    public final AtomicLong created = new AtomicLong(), updated = new AtomicLong(), validated = new AtomicLong(), learned = new AtomicLong(), taught = new AtomicLong(), teachFailed = new AtomicLong(),
            discoveries = new AtomicLong(), forgotten = new AtomicLong(), rumorsCreated = new AtomicLong(), rumorsSpread = new AtomicLong(), rumorsConfirmed = new AtomicLong(),
            rumorsRejected = new AtomicLong(), rumorsForgotten = new AtomicLong(), rumorHops = new AtomicLong(), propagated = new AtomicLong(), queueDropped = new AtomicLong(),
            history = new AtomicLong(), adopted = new AtomicLong(), errors = new AtomicLong();
    public final AtomicLong queryNanos = new AtomicLong(), queries = new AtomicLong(), learnNanos = new AtomicLong();

    public record Snapshot(long created, long updated, long validated, long learned, long taught, long teachFailed, long discoveries, long forgotten, long rumorsCreated, long rumorsSpread,
                           long rumorsConfirmed, long rumorsRejected, long rumorsForgotten, double averageSpread, long propagated, long queueDropped, long history, long adopted, long errors,
                           double queryMicros, double learnMicros) { }

    public Snapshot snapshot() {
        return new Snapshot(created.get(), updated.get(), validated.get(), learned.get(), taught.get(), teachFailed.get(), discoveries.get(), forgotten.get(), rumorsCreated.get(), rumorsSpread.get(),
                rumorsConfirmed.get(), rumorsRejected.get(), rumorsForgotten.get(), rumorsCreated.get() == 0 ? 0 : (double) rumorHops.get() / rumorsCreated.get(), propagated.get(), queueDropped.get(),
                history.get(), adopted.get(), errors.get(), queryNanos.get() / 1000.0 / Math.max(1, queries.get()), learnNanos.get() / 1000.0 / Math.max(1, learned.get() + updated.get()));
    }

    public void reset() {
        for (AtomicLong l : new AtomicLong[] {created, updated, validated, learned, taught, teachFailed, discoveries, forgotten, rumorsCreated, rumorsSpread, rumorsConfirmed, rumorsRejected,
                rumorsForgotten, rumorHops, propagated, queueDropped, history, adopted, errors, queryNanos, queries, learnNanos}) l.set(0);
    }
}
