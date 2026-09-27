package yadi.samuraiai.living.calendar.seasons;

import java.util.EnumMap;
import java.util.Map;
import yadi.samuraiai.living.core.Season;
import yadi.samuraiai.living.core.WeatherKind;

/**
 * What a season does to the world, as numbers other engines read: the base temperature and its daily swing, how likely each
 * weather is, and multipliers for vegetation, crop growth, food and fuel consumption, trade, social life, travel and animal
 * activity (1 = neutral). Pure data from the calendar configuration.
 */
public record SeasonProfile(Season season, double baseTemperature, double diurnalSwing, Map<WeatherKind, Double> weather, double vegetation, double crops,
                            double food, double fuel, double trade, double social, double travel, double animals) {
    public SeasonProfile {
        Map<WeatherKind, Double> copy = new EnumMap<>(WeatherKind.class);
        if (weather != null) weather.forEach((k, v) -> { if (v != null && v > 0) copy.put(k, v); });
        if (copy.isEmpty()) copy.put(WeatherKind.SUNNY, 1.0D);
        weather = Map.copyOf(copy);
        diurnalSwing = Math.max(0.0D, diurnalSwing);
        vegetation = clamp(vegetation); crops = clamp(crops); food = clamp(food); fuel = clamp(fuel); trade = clamp(trade); social = clamp(social); travel = clamp(travel); animals = clamp(animals);
    }

    private static double clamp(double v) { return Double.isFinite(v) ? Math.max(0.0D, Math.min(10.0D, v)) : 1.0D; }

    public double weatherWeight(WeatherKind kind) { return weather.getOrDefault(kind, 0.0D); }
}
