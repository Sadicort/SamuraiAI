package yadi.samuraiai.ai.navigation.engine;

import java.util.List;

/**
 * Immutable snapshot of every tunable navigation value, published atomically like {@code SamuraiSettings}.
 * The Forge-backed loader lives in {@link NavigationConfig}; tests build snapshots directly.
 */
public record NavigationSettings(
        int searchNodesPerTick, int maxSearchNodes, int maxSearchRadius, int scanRadius, int lookaheadNodes,
        int verifyIntervalTicks, int stuckTicks, double stuckMinProgress, int maxRecoveryAttempts, int maxRecalculations,
        int tempObstacleWaitTicks, int cacheCapacity, int cacheTtlTicks, double reachRadius, int defaultTimeoutTicks,
        boolean closeDoorsBehind, int doorWaitTicks, boolean allowTeleportRecovery, int maxSafeDrop, int maxSessionsPerTick,
        int nodeCacheMax, double walkSpeed, double runSpeed, double sprintSpeed, double sneakSpeed,
        double turnSlowdown, double maxTurnDegrees, int farUpdateInterval, double nearPlayerDistance,
        double farPlayerDistance, boolean debugLogging, double dangerCap, int partialMinGain, int chunkWaitTicks, int startRetryTicks, double entityLookaheadBlocks,
        List<String> costOverrides) {

    public NavigationSettings {
        searchNodesPerTick = clamp(searchNodesPerTick, 50, 20000);
        maxSearchNodes = clamp(maxSearchNodes, 100, 200000);
        maxSearchRadius = clamp(maxSearchRadius, 8, 512);
        scanRadius = clamp(scanRadius, 2, 64);
        lookaheadNodes = clamp(lookaheadNodes, 1, 64);
        verifyIntervalTicks = clamp(verifyIntervalTicks, 1, 200);
        stuckTicks = clamp(stuckTicks, 5, 1200);
        stuckMinProgress = clampD(stuckMinProgress, 0.01D, 5.0D);
        maxRecoveryAttempts = clamp(maxRecoveryAttempts, 0, 20);
        maxRecalculations = clamp(maxRecalculations, 0, 100);
        tempObstacleWaitTicks = clamp(tempObstacleWaitTicks, 0, 1200);
        cacheCapacity = clamp(cacheCapacity, 0, 10000);
        cacheTtlTicks = clamp(cacheTtlTicks, 20, 200000);
        reachRadius = clampD(reachRadius, 0.2D, 3.0D);
        defaultTimeoutTicks = clamp(defaultTimeoutTicks, 20, 200000);
        doorWaitTicks = clamp(doorWaitTicks, 0, 200);
        maxSafeDrop = clamp(maxSafeDrop, 1, 20);
        maxSessionsPerTick = clamp(maxSessionsPerTick, 1, 5000);
        nodeCacheMax = clamp(nodeCacheMax, 1000, 1_000_000);
        walkSpeed = clampD(walkSpeed, 0.1D, 4.0D); runSpeed = clampD(runSpeed, 0.1D, 4.0D);
        sprintSpeed = clampD(sprintSpeed, 0.1D, 4.0D); sneakSpeed = clampD(sneakSpeed, 0.1D, 4.0D);
        turnSlowdown = clampD(turnSlowdown, 0.0D, 1.0D);
        maxTurnDegrees = clampD(maxTurnDegrees, 5.0D, 360.0D);
        farUpdateInterval = clamp(farUpdateInterval, 1, 100);
        nearPlayerDistance = clampD(nearPlayerDistance, 4.0D, 512.0D);
        farPlayerDistance = clampD(farPlayerDistance, nearPlayerDistance, 1024.0D);
        dangerCap = clampD(dangerCap, 1.0D, 1000.0D);
        partialMinGain = clamp(partialMinGain, 1, 256);
        chunkWaitTicks = clamp(chunkWaitTicks, 1, 12000);
        startRetryTicks = clamp(startRetryTicks, 0, 400);
        entityLookaheadBlocks = clampD(entityLookaheadBlocks, 1.0D, 16.0D);
        costOverrides = costOverrides == null ? List.of() : List.copyOf(costOverrides);
    }

    public static NavigationSettings defaults() {
        return new NavigationSettings(600, 6000, 96, 12, 6, 5, 40, 0.35D, 4, 6, 40, 128, 1200, 0.6D, 2400,
                true, 6, false, 3, 64, 30000, 1.0D, 1.35D, 1.7D, 0.6D, 0.5D, 30.0D, 4, 32.0D, 64.0D, false, 100.0D, 4, 200, 20, 3.5D, List.of());
    }

    /** Starts from the defaults and overrides values by component name; unknown names are rejected loudly. */
    public static Builder builder() { return new Builder(defaults()); }
    public Builder toBuilder() { return new Builder(this); }

    public static final class Builder extends yadi.samuraiai.config.RecordSettingsBuilder<NavigationSettings, Builder> {
        private Builder(NavigationSettings base) { super(NavigationSettings.class, base); }
        @Override protected Builder self() { return this; }
    }

    private static volatile NavigationSettings current = defaults();
    public static NavigationSettings current() { return current; }
    public static void apply(NavigationSettings next) { current = java.util.Objects.requireNonNull(next); }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double clampD(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
}
