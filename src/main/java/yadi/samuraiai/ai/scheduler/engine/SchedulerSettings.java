package yadi.samuraiai.ai.scheduler.engine;

import java.util.List;
import yadi.samuraiai.config.RecordSettingsBuilder;

/**
 * Every tunable scheduler value, immutable and published atomically like the navigation and perception snapshots. The Forge
 * loader is {@code world.SchedulerConfig}; tests build snapshots with {@link #builder()}. The three lists carry data that
 * describes a world rather than the engine (lifestyles, the world calendar and per-routine overrides); an empty list means
 * "use the built-in catalogue".
 */
public record SchedulerSettings(
        // timeline (ticks into the day at which each period begins)
        int dayLength, int morningStart, int afternoonStart, int eveningStart, int nightStart, int lateNightStart, int dawnStart,
        // selection: dwell, hysteresis and the score a candidate needs, per priority layer, to be allowed to act
        int minRoutineTicks, double switchMargin, double stickiness,
        double baselineThreshold, double personalThreshold, double situationalThreshold, double emergencyThreshold,
        // how strongly each influence bends a routine's weight
        double lifestyleWeightScale, double personalityInfluence, double emotionInfluence, double energyInfluence, double calendarInfluence,
        // energy model (per tick) and the levels at which needs appear
        double energyDrainPerTick, double fatigueGainPerTick, double focusRecoveryPerTick, double stressDecayPerTick, double motivationDriftPerTick,
        double sleepNeedFatigue, double restNeedFatigue, double lowEnergy, double highStress,
        // interruption
        int maxInterruptDepth, int suspendMaxTicks, int pauseMaxTicks, int resumeDelayTicks,
        // cooldowns
        int routineCooldownTicks, int responseCooldownTicks,
        // social space
        double personalSpace, double socialDistance, double publicDistance, double crowdSpreadRadius,
        // groups and formations
        boolean groupsEnabled, double autoGroupRadius, int groupMaxSize, int leaderReelectTicks, double formationSpacing,
        double regroupDistance, double groupAlertRadius, int groupSyncTicks,
        // zones
        double zoneDefaultRadius, double arrivalTolerance, double fallbackRing, int patrolPoints, int zoneScanTicks,
        // optimisation: which bucket an NPC is in, how often each bucket is evaluated, and the crowd limits
        double visibleDistance, double nearbyDistance, double zoneActiveDistance, double farDistance, double hibernateDistance,
        int visibleInterval, int nearbyInterval, int zoneActiveInterval, int farInterval, int sleepingInterval, int hibernatingInterval,
        int maxEvaluationsPerTick, int crowdThreshold, double crowdIntervalScale, int budgetMicros,
        // personality
        double personalityJitter, double personalityDrift, double affinityStrength,
        // emotion
        double emotionShiftThreshold, double fearEmergency, double angerAggression,
        // event responses
        double investigateBase, double alarmBase, double fleeBase, double assistBase, double watchBase, double fleeDistance, int responseHoldTicks,
        boolean debugLogging,
        // world data
        List<String> lifestyles, List<String> calendar, List<String> routineOverrides) {

    public SchedulerSettings {
        dayLength = i(dayLength, 1200, 240000);
        morningStart = i(morningStart, 0, dayLength - 1); afternoonStart = i(afternoonStart, 0, dayLength - 1);
        eveningStart = i(eveningStart, 0, dayLength - 1); nightStart = i(nightStart, 0, dayLength - 1);
        lateNightStart = i(lateNightStart, 0, dayLength - 1); dawnStart = i(dawnStart, 0, dayLength - 1);
        minRoutineTicks = i(minRoutineTicks, 0, 24000); switchMargin = c(switchMargin, 0, 100); stickiness = c(stickiness, 0, 100);
        baselineThreshold = c(baselineThreshold, 0, 200); personalThreshold = c(personalThreshold, 0, 200);
        situationalThreshold = c(situationalThreshold, 0, 200); emergencyThreshold = c(emergencyThreshold, 0, 200);
        lifestyleWeightScale = c(lifestyleWeightScale, 0, 10); personalityInfluence = c(personalityInfluence, 0, 3);
        emotionInfluence = c(emotionInfluence, 0, 3); energyInfluence = c(energyInfluence, 0, 3); calendarInfluence = c(calendarInfluence, 0, 3);
        energyDrainPerTick = c(energyDrainPerTick, 0, 1); fatigueGainPerTick = c(fatigueGainPerTick, 0, 1);
        focusRecoveryPerTick = c(focusRecoveryPerTick, 0, 1); stressDecayPerTick = c(stressDecayPerTick, 0, 1);
        motivationDriftPerTick = c(motivationDriftPerTick, 0, 1);
        sleepNeedFatigue = c(sleepNeedFatigue, 1, 100); restNeedFatigue = c(restNeedFatigue, 1, 100);
        lowEnergy = c(lowEnergy, 0, 100); highStress = c(highStress, 1, 100);
        maxInterruptDepth = i(maxInterruptDepth, 1, 16); suspendMaxTicks = i(suspendMaxTicks, 20, 240000);
        pauseMaxTicks = i(pauseMaxTicks, 20, 240000); resumeDelayTicks = i(resumeDelayTicks, 0, 1200);
        routineCooldownTicks = i(routineCooldownTicks, 0, 240000); responseCooldownTicks = i(responseCooldownTicks, 0, 24000);
        personalSpace = c(personalSpace, 0.2, 8); socialDistance = c(socialDistance, personalSpace, 32);
        publicDistance = c(publicDistance, socialDistance, 64); crowdSpreadRadius = c(crowdSpreadRadius, 0.5, 32);
        autoGroupRadius = c(autoGroupRadius, 2, 256); groupMaxSize = i(groupMaxSize, 2, 32); leaderReelectTicks = i(leaderReelectTicks, 20, 240000);
        formationSpacing = c(formationSpacing, 0.8, 16); regroupDistance = c(regroupDistance, 2, 128);
        groupAlertRadius = c(groupAlertRadius, 4, 256); groupSyncTicks = i(groupSyncTicks, 1, 1200);
        zoneDefaultRadius = c(zoneDefaultRadius, 1, 128); arrivalTolerance = c(arrivalTolerance, 0.5, 16);
        fallbackRing = c(fallbackRing, 1, 64); patrolPoints = i(patrolPoints, 2, 32); zoneScanTicks = i(zoneScanTicks, 1, 6000);
        visibleDistance = c(visibleDistance, 4, 512); nearbyDistance = c(nearbyDistance, visibleDistance, 1024);
        zoneActiveDistance = c(zoneActiveDistance, nearbyDistance, 2048); farDistance = c(farDistance, zoneActiveDistance, 4096);
        hibernateDistance = c(hibernateDistance, farDistance, 8192);
        visibleInterval = i(visibleInterval, 1, 1200); nearbyInterval = i(nearbyInterval, visibleInterval, 2400);
        zoneActiveInterval = i(zoneActiveInterval, nearbyInterval, 6000); farInterval = i(farInterval, zoneActiveInterval, 12000);
        sleepingInterval = i(sleepingInterval, 1, 24000); hibernatingInterval = i(hibernatingInterval, farInterval, 48000);
        maxEvaluationsPerTick = i(maxEvaluationsPerTick, 1, 10000); crowdThreshold = i(crowdThreshold, 1, 100000);
        crowdIntervalScale = c(crowdIntervalScale, 1, 10); budgetMicros = i(budgetMicros, 100, 50000);
        personalityJitter = c(personalityJitter, 0, 50); personalityDrift = c(personalityDrift, 0, 10); affinityStrength = c(affinityStrength, 0, 3);
        emotionShiftThreshold = c(emotionShiftThreshold, 1, 100); fearEmergency = c(fearEmergency, 1, 100); angerAggression = c(angerAggression, 1, 100);
        investigateBase = c(investigateBase, 0, 200); alarmBase = c(alarmBase, 0, 200); fleeBase = c(fleeBase, 0, 200);
        assistBase = c(assistBase, 0, 200); watchBase = c(watchBase, 0, 200); fleeDistance = c(fleeDistance, 2, 128); responseHoldTicks = i(responseHoldTicks, 0, 24000);
        lifestyles = lifestyles == null ? List.of() : List.copyOf(lifestyles);
        calendar = calendar == null ? List.of() : List.copyOf(calendar);
        routineOverrides = routineOverrides == null ? List.of() : List.copyOf(routineOverrides);
    }

    public static SchedulerSettings defaults() {
        return new SchedulerSettings(
                24000, 1000, 6000, 11500, 14000, 18000, 22500,
                400, 8.0, 10.0,
                20, 30, 35, 70,
                1.0, 0.6, 0.6, 1.0, 1.0,
                0.002, 0.001, 0.003, 0.005, 0.0005,
                75, 55, 25, 70,
                4, 2400, 6000, 20,
                600, 200,
                1.2, 3.5, 8.0, 2.5,
                true, 24, 6, 200, 2.5,
                12, 32, 40,
                6.0, 2.5, 8.0, 6, 40,
                24, 64, 128, 256, 320,
                10, 20, 40, 100, 200, 600,
                16, 40, 1.5, 1500,
                12, 0.4, 0.5,
                15, 75, 60,
                50, 65, 90, 55, 35, 20, 200,
                false,
                List.of(), List.of(), List.of());
    }

    private static volatile SchedulerSettings current = defaults();
    public static SchedulerSettings current() { return current; }
    public static void apply(SchedulerSettings next) { current = java.util.Objects.requireNonNull(next); }
    public static Builder builder() { return new Builder(defaults()); }
    public Builder toBuilder() { return new Builder(this); }

    public static final class Builder extends RecordSettingsBuilder<SchedulerSettings, Builder> {
        private Builder(SchedulerSettings base) { super(SchedulerSettings.class, base); }
        @Override protected Builder self() { return this; }
    }

    private static double c(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
    private static int i(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
