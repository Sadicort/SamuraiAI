package yadi.samuraiai.living.economy.consumption;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.ai.scheduler.util.Segments;

/**
 * What one person consumes per day: food units (met by any food, best first), water, fuel units (wood, coal) and cloth; a
 * child counts for {@code childShare} of an adult. The season multiplies food and fuel (winter eats and burns more).
 * Line: {@code needs;food=1.0;water=2.0;fuel=0.3;cloth=0.01;child=0.6;foods=meal,rice,wheat,fish,meat}.
 */
public record NeedProfile(double food, double water, double fuel, double cloth, double childShare, List<String> foodPreference) {
    public static final String DEFAULT_LINE = "needs;food=1.0;water=2.0;fuel=0.3;cloth=0.01;child=0.6;foods=meal,rice,fish,wheat,meat";

    public NeedProfile {
        food = Math.max(0, food); water = Math.max(0, water); fuel = Math.max(0, fuel); cloth = Math.max(0, cloth); childShare = Math.max(0, Math.min(1, childShare));
        foodPreference = List.copyOf(foodPreference);
    }

    public static NeedProfile parse(String line) {
        Segments s = Segments.parse(line == null || line.isBlank() ? DEFAULT_LINE : line).orElse(Segments.parse(DEFAULT_LINE).orElseThrow());
        List<String> foods = new ArrayList<>();
        for (String f : s.list("foods")) foods.add(f.toLowerCase(java.util.Locale.ROOT));
        if (foods.isEmpty()) foods.addAll(List.of("meal", "rice", "fish", "wheat", "meat"));
        return new NeedProfile(s.number("food", 1.0), s.number("water", 2.0), s.number("fuel", 0.3), s.number("cloth", 0.01), s.number("child", 0.6), foods);
    }
}
