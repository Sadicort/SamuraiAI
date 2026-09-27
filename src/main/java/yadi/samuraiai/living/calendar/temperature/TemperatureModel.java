package yadi.samuraiai.living.calendar.temperature;

import yadi.samuraiai.living.calendar.seasons.SeasonProfile;
import yadi.samuraiai.living.calendar.weather.ClimateProfile;
import yadi.samuraiai.living.core.WeatherKind;

/**
 * Temperature in °C from the five inputs the specification names: season (blended into the next one during its last
 * {@code blendDays}), hour (coldest near 04:00, warmest near 15:00), altitude (lapse per block above sea level), the land's
 * microclimate and the weather. Stateless.
 */
public final class TemperatureModel {
    private final int blendDays;

    public TemperatureModel(int blendDays) { this.blendDays = Math.max(0, blendDays); }

    public double temperature(SeasonProfile season, SeasonProfile next, int dayOfSeason, int seasonLength, double hourOfDay, ClimateProfile climate, double altitude, WeatherKind weather) {
        double base = season.baseTemperature();
        double swing = season.diurnalSwing();
        int remaining = seasonLength - dayOfSeason;
        if (next != null && blendDays > 0 && remaining < blendDays) {
            double t = 1.0D - (remaining + 0.5D) / blendDays;   // 0 at the start of the blend, ~1 on the last day
            base += (next.baseTemperature() - base) * 0.5D * t;
            swing += (next.diurnalSwing() - swing) * 0.5D * t;
        }
        // cosine day: minimum at 04:00, maximum at 16:00
        double diurnal = -swing * Math.cos(2.0D * Math.PI * (hourOfDay - 4.0D) / 24.0D);
        double altitudeDelta = altitude > climate.seaLevel() ? -(altitude - climate.seaLevel()) * climate.lapsePerBlock() : 0.0D;
        return base + diurnal + climate.temperatureOffset() + altitudeDelta + weatherOffset(weather, hourOfDay);
    }

    public static double weatherOffset(WeatherKind weather, double hourOfDay) {
        if (weather == null) return 0.0D;
        boolean day = hourOfDay >= 7 && hourOfDay < 18;
        return switch (weather) {
            case SUNNY -> day ? 1.5D : -1.0D;      // clear nights are colder
            case CLOUDY -> day ? -0.5D : 0.5D;
            case FOG -> -1.0D;
            case RAIN -> -2.0D;
            case STORM -> -3.0D;
            case SNOW -> -4.0D;
            case STRONG_WIND -> -2.0D;
            case HAIL -> -3.5D;
        };
    }

    /** How comfortable it is to be outdoors, 0 (dangerously cold or hot) .. 1 (mild). NPC routines read it. */
    public static double comfort(double celsius) {
        if (celsius >= 12 && celsius <= 26) return 1.0D;
        if (celsius < 12) return Math.max(0.0D, 1.0D - (12 - celsius) / 22.0D);
        return Math.max(0.0D, 1.0D - (celsius - 26) / 14.0D);
    }
}
