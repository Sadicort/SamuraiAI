package yadi.samuraiai.ai.emotion.metrics;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import yadi.samuraiai.ai.emotion.model.MoodKind;

/** Counters, timings and time spent in each mood (all NPCs together) for the emotion engine. */
public final class EmotionMetrics {
    public final AtomicLong triggers = new AtomicLong(), created = new AtomicLong(), fused = new AtomicLong(), recovered = new AtomicLong(), moodChanges = new AtomicLong(),
            traumas = new AtomicLong(), traumasRecovered = new AtomicLong(), contagions = new AtomicLong(), regulations = new AtomicLong(), echoes = new AtomicLong(), errors = new AtomicLong();
    public final AtomicLong triggerNanos = new AtomicLong(), tickNanos = new AtomicLong(), ticks = new AtomicLong();
    private final Map<MoodKind, AtomicLong> moodTicks = new EnumMap<>(MoodKind.class);

    public EmotionMetrics() { for (MoodKind m : MoodKind.values()) moodTicks.put(m, new AtomicLong()); }

    public void inMood(MoodKind mood, long ticks) { moodTicks.get(mood).addAndGet(ticks); }
    public long ticksIn(MoodKind mood) { return moodTicks.get(mood).get(); }

    public record Snapshot(long triggers, long created, long fused, long recovered, long moodChanges, long traumas, long traumasRecovered, long contagions, long regulations,
                           long echoes, long errors, double triggerMicros, double tickMicros, long happyTicks, long sadTicks) { }

    public Snapshot snapshot() {
        return new Snapshot(triggers.get(), created.get(), fused.get(), recovered.get(), moodChanges.get(), traumas.get(), traumasRecovered.get(), contagions.get(), regulations.get(),
                echoes.get(), errors.get(), triggerNanos.get() / 1000.0 / Math.max(1, triggers.get()), tickNanos.get() / 1000.0 / Math.max(1, ticks.get()),
                ticksIn(MoodKind.HAPPY) + ticksIn(MoodKind.INSPIRED), ticksIn(MoodKind.MELANCHOLIC));
    }

    public void reset() {
        for (AtomicLong l : new AtomicLong[] {triggers, created, fused, recovered, moodChanges, traumas, traumasRecovered, contagions, regulations, echoes, errors, triggerNanos, tickNanos, ticks}) l.set(0);
        moodTicks.values().forEach(l -> l.set(0));
    }
}
