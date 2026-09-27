package yadi.samuraiai.ai.perception.engine;

import yadi.samuraiai.config.RecordSettingsBuilder;

/**
 * Every tunable perception value, immutable and published atomically like the other settings snapshots. The Forge
 * loader is {@code world.PerceptionConfig}; tests build snapshots with {@link #builder()}.
 */
public record PerceptionSettings(
        // vision
        double visionHorizontalFov, double visionVerticalFov, double visionNear, double visionFar, double visionPeripheralDegrees,
        double visionRearSensitivity, int raysPerTarget, double visibleConfidence, double minConfidence, int lostAfterTicks,
        // hearing
        double hearingSensitivity, double hearingThreshold, int soundLogTicks, double footstepWalkRadius, double footstepSprintRadius,
        double doorRadius, double blockBreakRadius, double blockPlaceRadius, double explosionRadius, double projectileRadius,
        double damageRadius, double animalRadius, double voiceRadius, double wallDampening,
        // attention
        double attentionSwitchRatio, int attentionRecoverTicks, double attentionMinScore,
        // memory (ticks of life per kind) and fade
        int visualMemoryTicks, int auditoryMemoryTicks, int environmentMemoryTicks, int socialMemoryTicks, int dangerMemoryTicks,
        int interestMemoryTicks, double memoryForgetThreshold,
        // suspicion / interest / threat
        double suspicionDecayPerTick, double suspicionRaiseThreshold, double suspicionClearThreshold,
        double interestThreshold, double interestDecayPerTick,
        double threatWarning, double threatDanger, double threatCritical, double threatDecayPerTick,
        // awareness
        int awarenessMinDwellTicks,
        // scheduling: base interval of each sensor, failure cooldown, budgets and update tiers
        int visionInterval, int hearingInterval, int movementInterval, int entityInterval, int blockInterval, int lightInterval,
        int weatherInterval, int environmentInterval, int smellInterval, int failureCooldownTicks,
        int maxRaycastsPerTick, int maxNpcsPerTick, int blockScanCellsPerScan, double blockScanRadius, double entityScanRadius,
        double tierNearDistance, double tierFarDistance, int midIntervalMultiplier, int farIntervalMultiplier, int alertIntervalDivisor,
        // misc
        double touchRadius, int cacheTtlTicks, boolean debugLogging) {

    public PerceptionSettings {
        visionHorizontalFov = c(visionHorizontalFov, 20, 360);
        visionVerticalFov = c(visionVerticalFov, 10, 180);
        visionNear = c(visionNear, 0.5, 16);
        visionFar = c(visionFar, 4, 128);
        visionPeripheralDegrees = c(visionPeripheralDegrees, 0, 90);
        visionRearSensitivity = c(visionRearSensitivity, 0, 1);
        raysPerTarget = i(raysPerTarget, 1, 9);
        visibleConfidence = c(visibleConfidence, 0.1, 1);
        minConfidence = c(minConfidence, 0.01, visibleConfidence);
        lostAfterTicks = i(lostAfterTicks, 1, 2400);
        hearingSensitivity = c(hearingSensitivity, 0.05, 4);
        hearingThreshold = c(hearingThreshold, 0.001, 1);
        soundLogTicks = i(soundLogTicks, 2, 1200);
        footstepWalkRadius = c(footstepWalkRadius, 0, 64); footstepSprintRadius = c(footstepSprintRadius, 0, 128);
        doorRadius = c(doorRadius, 0, 128); blockBreakRadius = c(blockBreakRadius, 0, 128); blockPlaceRadius = c(blockPlaceRadius, 0, 128);
        explosionRadius = c(explosionRadius, 0, 256); projectileRadius = c(projectileRadius, 0, 128); damageRadius = c(damageRadius, 0, 128);
        animalRadius = c(animalRadius, 0, 128); voiceRadius = c(voiceRadius, 0, 128); wallDampening = c(wallDampening, 0, 1);
        attentionSwitchRatio = c(attentionSwitchRatio, 1, 10); attentionRecoverTicks = i(attentionRecoverTicks, 1, 6000);
        attentionMinScore = c(attentionMinScore, 0, 100);
        visualMemoryTicks = i(visualMemoryTicks, 20, 240000); auditoryMemoryTicks = i(auditoryMemoryTicks, 20, 240000);
        environmentMemoryTicks = i(environmentMemoryTicks, 20, 240000); socialMemoryTicks = i(socialMemoryTicks, 20, 240000);
        dangerMemoryTicks = i(dangerMemoryTicks, 20, 240000); interestMemoryTicks = i(interestMemoryTicks, 20, 240000);
        memoryForgetThreshold = c(memoryForgetThreshold, 0.001, 0.9);
        suspicionDecayPerTick = c(suspicionDecayPerTick, 0, 10); suspicionRaiseThreshold = c(suspicionRaiseThreshold, 1, 100);
        suspicionClearThreshold = c(suspicionClearThreshold, 0, suspicionRaiseThreshold);
        interestThreshold = c(interestThreshold, 1, 100); interestDecayPerTick = c(interestDecayPerTick, 0, 10);
        threatWarning = c(threatWarning, 1, 100); threatDanger = c(threatDanger, threatWarning, 100); threatCritical = c(threatCritical, threatDanger, 100);
        threatDecayPerTick = c(threatDecayPerTick, 0, 10);
        awarenessMinDwellTicks = i(awarenessMinDwellTicks, 0, 1200);
        visionInterval = i(visionInterval, 1, 200); hearingInterval = i(hearingInterval, 1, 200); movementInterval = i(movementInterval, 1, 200);
        entityInterval = i(entityInterval, 1, 200); blockInterval = i(blockInterval, 1, 2000); lightInterval = i(lightInterval, 1, 2000);
        weatherInterval = i(weatherInterval, 1, 6000); environmentInterval = i(environmentInterval, 1, 6000); smellInterval = i(smellInterval, 1, 2000);
        failureCooldownTicks = i(failureCooldownTicks, 1, 24000);
        maxRaycastsPerTick = i(maxRaycastsPerTick, 8, 100000); maxNpcsPerTick = i(maxNpcsPerTick, 1, 10000);
        blockScanCellsPerScan = i(blockScanCellsPerScan, 16, 100000); blockScanRadius = c(blockScanRadius, 2, 32);
        entityScanRadius = c(entityScanRadius, 4, 128);
        tierNearDistance = c(tierNearDistance, 4, 512); tierFarDistance = c(tierFarDistance, tierNearDistance, 1024);
        midIntervalMultiplier = i(midIntervalMultiplier, 1, 50); farIntervalMultiplier = i(farIntervalMultiplier, 1, 200);
        alertIntervalDivisor = i(alertIntervalDivisor, 1, 20);
        touchRadius = c(touchRadius, 0.2, 4); cacheTtlTicks = i(cacheTtlTicks, 1, 1200);
    }

    public static PerceptionSettings defaults() {
        return new PerceptionSettings(
                110, 90, 2.0, 24.0, 40, 0.1, 3, 0.6, 0.2, 40,
                1.0, 0.05, 60, 6, 12, 10, 14, 8, 64, 16, 18, 12, 16, 0.25,
                1.25, 100, 5,
                600, 400, 2400, 6000, 1800, 1200, 0.05,
                0.2, 30, 10, 40, 0.1, 15, 40, 70, 0.35,
                20,
                3, 2, 3, 4, 20, 20, 100, 100, 40, 100,
                300, 96, 512, 8, 32, 32, 64, 2, 4, 2,
                1.5, 20, false);
    }

    private static volatile PerceptionSettings current = defaults();
    public static PerceptionSettings current() { return current; }
    public static void apply(PerceptionSettings next) { current = java.util.Objects.requireNonNull(next); }
    public static Builder builder() { return new Builder(defaults()); }
    public Builder toBuilder() { return new Builder(this); }

    public static final class Builder extends RecordSettingsBuilder<PerceptionSettings, Builder> {
        private Builder(PerceptionSettings base) { super(PerceptionSettings.class, base); }
        @Override protected Builder self() { return this; }
    }

    private static double c(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
    private static int i(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
