package yadi.samuraiai.ai.memory.metrics;

import java.util.concurrent.atomic.AtomicLong;

/** Counters and timings for the memory engine. */
public final class MemoryMetrics {
    /** The last failure inside the engine, for diagnostics. */
    public volatile String lastError = "";
    public final AtomicLong created = new AtomicLong(), discarded = new AtomicLong(), reinforced = new AtomicLong(), merged = new AtomicLong(),
            forgotten = new AtomicLong(), compressed = new AtomicLong(), consolidated = new AtomicLong(), retrieved = new AtomicLong(), retrievals = new AtomicLong(),
            echoes = new AtomicLong(), reinterpreted = new AtomicLong(), errors = new AtomicLong();
    public final AtomicLong retrievalNanos = new AtomicLong(), consolidationNanos = new AtomicLong(), maintenanceNanos = new AtomicLong(), observeNanos = new AtomicLong();
    public final AtomicLong recordsBeforeCompression = new AtomicLong(), recordsAfterCompression = new AtomicLong();
    public final AtomicLong cacheHits = new AtomicLong(), cacheMisses = new AtomicLong();

    public record Snapshot(long created, long discarded, long reinforced, long merged, long forgotten, long compressed, long consolidated, long retrieved,
                           long retrievals, long echoes, long reinterpreted, long errors, double retrievalMicros, double consolidationMicros, double observeMicros,
                           double compressionRatio, double cacheHitRate) { }

    public Snapshot snapshot() {
        long r = retrievals.get(), o = created.get() + reinforced.get() + discarded.get();
        long consolidations = Math.max(1, consolidated.get() + merged.get());
        long before = recordsBeforeCompression.get(), after = recordsAfterCompression.get();
        long hits = cacheHits.get(), misses = cacheMisses.get();
        return new Snapshot(created.get(), discarded.get(), reinforced.get(), merged.get(), forgotten.get(), compressed.get(), consolidated.get(), retrieved.get(), r,
                echoes.get(), reinterpreted.get(), errors.get(), r == 0 ? 0 : retrievalNanos.get() / 1000.0 / r, consolidationNanos.get() / 1000.0 / consolidations,
                o == 0 ? 0 : observeNanos.get() / 1000.0 / o, after == 0 ? 1.0 : (double) before / after, hits + misses == 0 ? 0 : (double) hits / (hits + misses));
    }

    public void reset() {
        for (AtomicLong l : new AtomicLong[] {created, discarded, reinforced, merged, forgotten, compressed, consolidated, retrieved, retrievals, echoes, reinterpreted, errors,
                retrievalNanos, consolidationNanos, maintenanceNanos, observeNanos, recordsBeforeCompression, recordsAfterCompression, cacheHits, cacheMisses}) l.set(0);
    }
}
