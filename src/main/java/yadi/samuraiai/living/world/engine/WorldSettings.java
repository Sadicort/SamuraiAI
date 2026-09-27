package yadi.samuraiai.living.world.engine;

import java.util.List;
import yadi.samuraiai.config.RecordSettingsBuilder;

/** Every tunable value of the Living World Engine ({@code samuraiai-world.toml}). Empty lists use the built-in data. */
public record WorldSettings(
        // map and streaming
        int regionCellSize, double fullRadius, double nearRadius, double settlementRadius, int historicalAfterDays, int streamingIntervalTicks, int maxRegions,
        // simulation steps (Deiliora minutes) and budget
        int stepMinutesActive, int stepMinutesSettlement, int stepMinutesAbstract, int stepMinutesHistorical, int maxCatchUpSteps, int maxRegionsPerTick,
        int simulationIntervalTicks, int budgetMicros,
        // roads
        int roadsPerSettlement, double maxRoadLength, double roadTortuosity,
        // spontaneous world events (chance per day)
        double fireChancePerDay, double attackChancePerDay, double banditChancePerDay, double duelChancePerDay, double floodChanceInStorm, int specialMarketEveryDays,
        int eventConsequenceMinutes, int maxOpenEvents, int eventArchive,
        // misc
        int populationLog, int maxInteractionOrders, boolean debugLogging,
        // data lines
        List<String> regions, List<String> wildlife, List<String> professions) {

    public WorldSettings {
        regionCellSize = i(regionCellSize, 64, 16384); fullRadius = c(fullRadius, 8, 4096); nearRadius = c(nearRadius, fullRadius, 8192); settlementRadius = c(settlementRadius, nearRadius, 65536);
        historicalAfterDays = i(historicalAfterDays, 1, 36500); streamingIntervalTicks = i(streamingIntervalTicks, 1, 12000); maxRegions = i(maxRegions, 16, 1000000);
        stepMinutesActive = i(stepMinutesActive, 1, 1440); stepMinutesSettlement = i(stepMinutesSettlement, stepMinutesActive, 10080); stepMinutesAbstract = i(stepMinutesAbstract, stepMinutesSettlement, 43200);
        stepMinutesHistorical = i(stepMinutesHistorical, stepMinutesAbstract, 525600); maxCatchUpSteps = i(maxCatchUpSteps, 1, 10000); maxRegionsPerTick = i(maxRegionsPerTick, 1, 10000);
        simulationIntervalTicks = i(simulationIntervalTicks, 1, 1200); budgetMicros = i(budgetMicros, 50, 100000);
        roadsPerSettlement = i(roadsPerSettlement, 0, 16); maxRoadLength = c(maxRoadLength, 16, 100000); roadTortuosity = c(roadTortuosity, 1, 4);
        fireChancePerDay = c(fireChancePerDay, 0, 1); attackChancePerDay = c(attackChancePerDay, 0, 1); banditChancePerDay = c(banditChancePerDay, 0, 1); duelChancePerDay = c(duelChancePerDay, 0, 1);
        floodChanceInStorm = c(floodChanceInStorm, 0, 1); specialMarketEveryDays = i(specialMarketEveryDays, 0, 3650);
        eventConsequenceMinutes = i(eventConsequenceMinutes, 1, 10080); maxOpenEvents = i(maxOpenEvents, 1, 100000); eventArchive = i(eventArchive, 0, 100000);
        populationLog = i(populationLog, 16, 1000000); maxInteractionOrders = i(maxInteractionOrders, 0, 256);
        regions = List.copyOf(regions == null ? List.of() : regions); wildlife = List.copyOf(wildlife == null ? List.of() : wildlife); professions = List.copyOf(professions == null ? List.of() : professions);
    }

    public static WorldSettings defaults() {
        return new WorldSettings(512, 96, 320, 1200, 30, 40, 4096,
                60, 360, 1440, 10080, 48, 4, 5, 1500,
                2, 3000, 1.25,
                0.003, 0.002, 0.004, 0.002, 0.15, 30, 60, 64, 500,
                2000, 8, false,
                List.of(), List.of(), List.of());
    }

    private static volatile WorldSettings current = defaults();
    public static WorldSettings current() { return current; }
    public static void apply(WorldSettings next) { current = java.util.Objects.requireNonNull(next); }
    public static Builder builder() { return new Builder(defaults()); }

    public static final class Builder extends RecordSettingsBuilder<WorldSettings, Builder> {
        private Builder(WorldSettings base) { super(WorldSettings.class, base); }
        @Override protected Builder self() { return this; }
    }

    private static double c(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
    private static int i(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
