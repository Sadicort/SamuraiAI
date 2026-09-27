package yadi.samuraiai.ai.cognition.engine;

import java.util.List;
import yadi.samuraiai.config.RecordSettingsBuilder;

/** Every tunable value of the cognitive layer that orchestrates memory, relationships, emotion, knowledge and society ({@code samuraiai-cognition.toml}). */
public record CognitionSettings(
        // persistence
        int saveIntervalTicks, int maxSavesPerTick, boolean compressStorage, int traceCapacity,
        // how the world feeds the layer
        double witnessRadius, int gossipScanTicks, int exploreScanTicks, int contextRefreshTicks, int echoCheckTicks, int ritualCheckTicks,
        // cost control: how often an NPC's mind is updated, by distance to the nearest player, and the per-tick budget
        double nearDistance, double midDistance, double farDistance, int nearInterval, int midInterval, int farInterval, int hibernateInterval, int maxMindsPerTick, int budgetMicros,
        // personality evolution
        double evolutionRate, double evolutionCap, double evolutionMinMagnitude, double evolutionDailyLimit, double temporaryCap,
        // integration thresholds
        double emotionMemoryThreshold, double rumorMinMagnitude, double extractMinImportance, double dangerKnownThreshold, double familiarRadius,
        // projection into the legacy systems
        int projectionTicks, boolean projectEmotions, boolean projectRelationships,
        boolean debugLogging,
        // "KIND|magnitude|danger|EMOTION:n;...|trust,respect,affinity,fear,loyalty,rivalry,honor": overrides of the experience catalogue. Empty = built-in.
        List<String> experienceOverrides) {

    public CognitionSettings {
        saveIntervalTicks = i(saveIntervalTicks, 20, 24000000); maxSavesPerTick = i(maxSavesPerTick, 1, 1000); traceCapacity = i(traceCapacity, 16, 100000);
        witnessRadius = c(witnessRadius, 1, 128); gossipScanTicks = i(gossipScanTicks, 1, 240000); exploreScanTicks = i(exploreScanTicks, 1, 240000);
        contextRefreshTicks = i(contextRefreshTicks, 1, 24000); echoCheckTicks = i(echoCheckTicks, 1, 24000); ritualCheckTicks = i(ritualCheckTicks, 1, 240000);
        nearDistance = c(nearDistance, 1, 4096); midDistance = c(midDistance, nearDistance, 8192); farDistance = c(farDistance, midDistance, 16384);
        nearInterval = i(nearInterval, 1, 24000); midInterval = i(midInterval, nearInterval, 48000); farInterval = i(farInterval, midInterval, 240000); hibernateInterval = i(hibernateInterval, farInterval, 2400000);
        maxMindsPerTick = i(maxMindsPerTick, 1, 10000); budgetMicros = i(budgetMicros, 100, 100000);
        evolutionRate = c(evolutionRate, 0, 10); evolutionCap = c(evolutionCap, 0, 50); evolutionMinMagnitude = c(evolutionMinMagnitude, 0, 1);
        evolutionDailyLimit = c(evolutionDailyLimit, 0, 20); temporaryCap = c(temporaryCap, 0, 50);
        emotionMemoryThreshold = c(emotionMemoryThreshold, 0, 1); rumorMinMagnitude = c(rumorMinMagnitude, 0, 1); extractMinImportance = c(extractMinImportance, 0, 1);
        dangerKnownThreshold = c(dangerKnownThreshold, 0, 1); familiarRadius = c(familiarRadius, 1, 128);
        projectionTicks = i(projectionTicks, 1, 24000);
        experienceOverrides = List.copyOf(experienceOverrides == null ? List.of() : experienceOverrides);
    }

    public static CognitionSettings defaults() {
        return new CognitionSettings(1200, 4, false, 512,
                16.0, 200, 200, 100, 100, 200,
                32.0, 96.0, 256.0, 20, 100, 400, 1200, 8, 2000,
                3.0, 25.0, 0.35, 2.0, 10.0,
                0.6, 0.5, 0.25, 0.5, 12.0,
                20, true, true,
                false, List.of());
    }

    private static volatile CognitionSettings current = defaults();
    public static CognitionSettings current() { return current; }
    public static void apply(CognitionSettings next) { current = java.util.Objects.requireNonNull(next); }
    public static Builder builder() { return new Builder(defaults()); }

    public static final class Builder extends RecordSettingsBuilder<CognitionSettings, Builder> {
        private Builder(CognitionSettings base) { super(CognitionSettings.class, base); }
        @Override protected Builder self() { return this; }
    }

    private static double c(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
    private static int i(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
