package yadi.samuraiai.ai.cognition.engine;

import java.util.concurrent.atomic.AtomicLong;

/** Cost and volume of the cognitive layer as a whole: experiences processed, time in each stage, persistence. */
public final class CognitionMetrics {
    /** The last failure inside the layer (an experience that could not be processed), for diagnostics. */
    public volatile String lastError = "";
    public final AtomicLong errors = new AtomicLong();
    public final AtomicLong experiences = new AtomicLong(), discarded = new AtomicLong(), rumorsStarted = new AtomicLong(), evolutions = new AtomicLong(), echoes = new AtomicLong(),
            ticks = new AtomicLong(), saves = new AtomicLong(), loads = new AtomicLong(), saveFailures = new AtomicLong(), loadFailures = new AtomicLong(), recoveries = new AtomicLong();
    public final AtomicLong experienceNanos = new AtomicLong(), memoryNanos = new AtomicLong(), emotionNanos = new AtomicLong(), relationshipNanos = new AtomicLong(), knowledgeNanos = new AtomicLong(),
            tickNanos = new AtomicLong(), saveNanos = new AtomicLong(), loadNanos = new AtomicLong();

    public record Snapshot(long experiences, long discarded, long rumorsStarted, long evolutions, long echoes, long ticks, long saves, long loads, long saveFailures, long loadFailures,
                           long recoveries, double experienceMicros, double memoryMicros, double emotionMicros, double relationshipMicros, double knowledgeMicros, double tickMicros,
                           double saveMillis, double loadMillis) { }

    public Snapshot snapshot() {
        long e = Math.max(1, experiences.get() - discarded.get()), t = Math.max(1, ticks.get());
        return new Snapshot(experiences.get(), discarded.get(), rumorsStarted.get(), evolutions.get(), echoes.get(), ticks.get(), saves.get(), loads.get(), saveFailures.get(), loadFailures.get(), recoveries.get(),
                experienceNanos.get() / 1000.0 / Math.max(1, experiences.get()), memoryNanos.get() / 1000.0 / Math.max(1, experiences.get()), emotionNanos.get() / 1000.0 / e, relationshipNanos.get() / 1000.0 / e,
                knowledgeNanos.get() / 1000.0 / e, tickNanos.get() / 1000.0 / t, saveNanos.get() / 1e6 / Math.max(1, saves.get()), loadNanos.get() / 1e6 / Math.max(1, loads.get()));
    }

    public void reset() {
        for (AtomicLong l : new AtomicLong[] {experiences, discarded, rumorsStarted, evolutions, echoes, ticks, saves, loads, saveFailures, loadFailures, recoveries, experienceNanos, memoryNanos, emotionNanos,
                relationshipNanos, knowledgeNanos, tickNanos, saveNanos, loadNanos}) l.set(0);
    }
}
