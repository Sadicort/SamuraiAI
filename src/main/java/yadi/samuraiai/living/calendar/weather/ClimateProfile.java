package yadi.samuraiai.living.calendar.weather;

import java.util.EnumMap;
import java.util.Map;
import yadi.samuraiai.living.core.WeatherKind;

/**
 * A microclimate: how a kind of land shifts the season's weather and temperature. {@code weatherScale} multiplies the season's
 * weather weights (a mountain makes snow and wind likelier, a swamp fog), {@code temperatureOffset} is added in °C and
 * {@code lapsePerBlock} is how much colder it gets per block above {@code seaLevel}.
 */
public record ClimateProfile(String id, double temperatureOffset, Map<WeatherKind, Double> weatherScale, double lapsePerBlock, int seaLevel) {
    public ClimateProfile {
        id = id == null || id.isBlank() ? "temperate" : id;
        Map<WeatherKind, Double> copy = new EnumMap<>(WeatherKind.class);
        if (weatherScale != null) weatherScale.forEach((k, v) -> { if (v != null && v >= 0) copy.put(k, v); });
        weatherScale = Map.copyOf(copy);
        lapsePerBlock = Double.isFinite(lapsePerBlock) ? Math.max(0.0D, Math.min(1.0D, lapsePerBlock)) : 0.03D;
    }

    public double scale(WeatherKind kind) { return weatherScale.getOrDefault(kind, 1.0D); }
}
