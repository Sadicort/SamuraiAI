package yadi.samuraiai.living.calendar.engine;

import java.util.List;
import yadi.samuraiai.config.RecordSettingsBuilder;

/**
 * Every tunable value of the Living Calendar ({@code samuraiai-calendar.toml}). Empty lists use the built-in data, which is
 * written in the same line format so it can be copied into the file and changed.
 *
 * <p>{@code offlineMode}: 0 = time passes only while the server runs (default); 1 = when the server starts, the real time it
 * was stopped is converted to Deiliora time at {@code offlineRealMinutesPerDay}; 2 = like 1 but never more than
 * {@code offlineMaxDays}. The world catches up that time with bounded, abstract simulation.
 */
public record CalendarSettings(
        // clock
        int ticksPerDay, boolean alignToSun, int sunOffsetMinutes, int epochYear, int startDayOfYear, int startHour,
        // seasons, moon, sun
        int seasonBlendDays, int moonCycleDays, int moonOffsetDays, double daylightAmplitudeHours, int longestDayOfYear,
        // weather
        int weatherMinHours, int weatherMaxHours, double weatherPersistence, int weatherMaxSteps, boolean driveVanillaWeather,
        // agriculture verdicts
        double greatHarvestYield, double badHarvestYield,
        // day processing and history
        int maxDaysPerAdvance, int timelineMax, double timelineKeepSignificance, int anniversaryMax,
        // offline time
        int offlineMode, int offlineRealMinutesPerDay, int offlineMaxDays,
        boolean debugLogging,
        // data (empty = built-in)
        List<String> months, List<String> weekdays, List<String> phases, List<String> seasons, List<String> climates, List<String> festivals,
        List<String> holidays, List<String> crops) {

    public CalendarSettings {
        ticksPerDay = i(ticksPerDay, 200, 24000000); sunOffsetMinutes = Math.floorMod(sunOffsetMinutes, 1440); epochYear = i(epochYear, -100000, 100000);
        startDayOfYear = i(startDayOfYear, 1, 4000); startHour = i(startHour, 0, 23);
        seasonBlendDays = i(seasonBlendDays, 0, 90); moonCycleDays = i(moonCycleDays, 2, 400); daylightAmplitudeHours = c(daylightAmplitudeHours, 0, 10); longestDayOfYear = i(longestDayOfYear, 1, 4000);
        weatherMinHours = i(weatherMinHours, 1, 240); weatherMaxHours = i(weatherMaxHours, weatherMinHours, 480); weatherPersistence = c(weatherPersistence, 0, 0.95); weatherMaxSteps = i(weatherMaxSteps, 1, 100000);
        greatHarvestYield = c(greatHarvestYield, 1, 2); badHarvestYield = c(badHarvestYield, 0, 1);
        maxDaysPerAdvance = i(maxDaysPerAdvance, 1, 3650); timelineMax = i(timelineMax, 64, 10000000); timelineKeepSignificance = c(timelineKeepSignificance, 0, 1); anniversaryMax = i(anniversaryMax, 16, 10000000);
        offlineMode = i(offlineMode, 0, 2); offlineRealMinutesPerDay = i(offlineRealMinutesPerDay, 1, 100000); offlineMaxDays = i(offlineMaxDays, 0, 36500);
        months = copy(months); weekdays = copy(weekdays); phases = copy(phases); seasons = copy(seasons); climates = copy(climates); festivals = copy(festivals); holidays = copy(holidays); crops = copy(crops);
    }

    public static CalendarSettings defaults() {
        return new CalendarSettings(24000, true, 360, 100, 1, 6,
                15, 30, 1, 2.5, 135,
                3, 16, 0.35, 200, false,
                1.25, 0.7,
                90, 20000, 0.7, 50000,
                0, 20, 7,
                false,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static volatile CalendarSettings current = defaults();
    public static CalendarSettings current() { return current; }
    public static void apply(CalendarSettings next) { current = java.util.Objects.requireNonNull(next); }
    public static Builder builder() { return new Builder(defaults()); }
    public static Builder builder(CalendarSettings base) { return new Builder(base); }

    public static final class Builder extends RecordSettingsBuilder<CalendarSettings, Builder> {
        private Builder(CalendarSettings base) { super(CalendarSettings.class, base); }
        @Override protected Builder self() { return this; }
    }

    private static List<String> copy(List<String> l) { return List.copyOf(l == null ? List.of() : l); }
    private static double c(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
    private static int i(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
