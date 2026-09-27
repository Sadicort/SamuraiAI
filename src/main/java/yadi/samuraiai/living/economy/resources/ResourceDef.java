package yadi.samuraiai.living.economy.resources;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A resource as the economy understands it (the Resource Record): its category, rarity (0 common .. 1 rare), weight per unit
 * (what a cart can carry), base value in coins, how fast it spoils (share lost per day), the seasons it is harvested in, the
 * professions that produce it and what consumes it, and how much food or fuel one unit is worth. {@code future} marks
 * resources that exist in the catalogue but nothing produces yet (gold, silver).
 */
public record ResourceDef(String id, String name, Category category, double rarity, double weight, double baseValue, double spoilPerDay, List<String> seasons,
                          List<String> producers, List<String> consumers, double food, double fuel, boolean future) {
    public enum Category {
        FOOD, WATER, MATERIAL, FUEL, TOOL, TEXTILE, MEDICINE, LUXURY;

        public static Optional<Category> parse(String s) {
            try { return Optional.of(valueOf(s.trim().toUpperCase(Locale.ROOT))); } catch (RuntimeException e) { return Optional.empty(); }
        }
    }

    public ResourceDef {
        rarity = clamp(rarity, 0, 1); weight = clamp(weight, 0.01, 1000); baseValue = clamp(baseValue, 0, 1_000_000); spoilPerDay = clamp(spoilPerDay, 0, 1);
        food = clamp(food, 0, 100); fuel = clamp(fuel, 0, 100);
        seasons = List.copyOf(seasons); producers = List.copyOf(producers); consumers = List.copyOf(consumers);
    }

    private static double clamp(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }

    public boolean perishable() { return spoilPerDay > 0; }
}
