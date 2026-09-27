package yadi.samuraiai.living.village.engine;

import java.util.List;
import yadi.samuraiai.config.RecordSettingsBuilder;

/** Every tunable value of the Living Villages Engine ({@code samuraiai-village.toml}). */
public record VillageSettings(
        // creation (whether and where villages are founded automatically is the hub's samuraiai-living.toml)
        int plannedHouses,
        // update cadence
        int tickIntervalTicks, int villagesPerTick,
        // how strongly village life biases the Behavior Scheduler (points)
        double scheduleBias, double professionBiasScale, double eventBiasScale, double festivalBiasScale, double weatherPenalty, double coldThreshold, double neighbourSocialBias,
        // security
        double threatDecayPerHour, double alertThreshold, double dangerThreshold, int recoveryMinutes, double threatPerLevel, double combatThreat,
        // visitors
        double visitorsPerDay, int visitorMinStayHours, int visitorMaxStayHours, int maxVisitors,
        // guards, unrest, memory
        double nightWatchShare, double unrestFoodDays, double memorySignificance, int nightShiftMinutes,
        boolean debugLogging,
        // data
        List<String> templates, List<String> professionSchedules) {

    public VillageSettings {
        plannedHouses = i(plannedHouses, 0, 200);
        tickIntervalTicks = i(tickIntervalTicks, 1, 1200); villagesPerTick = i(villagesPerTick, 1, 10000);
        scheduleBias = c(scheduleBias, 0, 200); professionBiasScale = c(professionBiasScale, 0, 5); eventBiasScale = c(eventBiasScale, 0, 5); festivalBiasScale = c(festivalBiasScale, 0, 5);
        weatherPenalty = c(weatherPenalty, 0, 200); coldThreshold = c(coldThreshold, -40, 40); neighbourSocialBias = c(neighbourSocialBias, 0, 100);
        threatDecayPerHour = c(threatDecayPerHour, 0, 100); alertThreshold = c(alertThreshold, 1, 100); dangerThreshold = c(dangerThreshold, alertThreshold, 100); recoveryMinutes = i(recoveryMinutes, 1, 100000);
        threatPerLevel = c(threatPerLevel, 0, 100); combatThreat = c(combatThreat, 0, 100);
        visitorsPerDay = c(visitorsPerDay, 0, 100); visitorMinStayHours = i(visitorMinStayHours, 1, 720); visitorMaxStayHours = i(visitorMaxStayHours, visitorMinStayHours, 2160); maxVisitors = i(maxVisitors, 0, 10000);
        nightWatchShare = c(nightWatchShare, 0, 1); unrestFoodDays = c(unrestFoodDays, 0, 100); memorySignificance = c(memorySignificance, 0, 1); nightShiftMinutes = i(nightShiftMinutes, 0, 1439);
        templates = List.copyOf(templates == null ? List.of() : templates); professionSchedules = List.copyOf(professionSchedules == null ? List.of() : professionSchedules);
    }

    public static VillageSettings defaults() {
        return new VillageSettings(8,
                20, 8,
                35, 1.0, 1.0, 1.0, 25, 5, 10,
                12, 25, 60, 360, 15, 35,
                0.8, 6, 36, 30,
                0.34, 1.0, 0.5, 720,
                false,
                List.of(), List.of());
    }

    private static volatile VillageSettings current = defaults();
    public static VillageSettings current() { return current; }
    public static void apply(VillageSettings next) { current = java.util.Objects.requireNonNull(next); }
    public static Builder builder() { return new Builder(defaults()); }

    public static final class Builder extends RecordSettingsBuilder<VillageSettings, Builder> {
        private Builder(VillageSettings base) { super(VillageSettings.class, base); }
        @Override protected Builder self() { return this; }
    }

    private static double c(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
    private static int i(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
