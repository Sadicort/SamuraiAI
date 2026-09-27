package yadi.samuraiai.living.economy.production;

import java.util.Map;

/**
 * What a profession makes in an hour of work and from what. {@code source} says where the goods come from:
 * <ul>
 *   <li>DEPOSIT — taken from the region's natural deposits (timber, ore, clay, herbs);</li>
 *   <li>WILDLIFE — taken from the region's animals (fishing, hunting, husbandry);</li>
 *   <li>FARM — grown on the village's fields: limited by its farm buildings and multiplied by the agricultural calendar;</li>
 *   <li>CRAFT — made from {@code inputs} taken out of the warehouse (no inputs, no output);</li>
 *   <li>SERVICE — no goods (guards, monks, samurai, merchants earn by other means).</li>
 * </ul>
 * {@code toolWear} is how many tools an hour wears out; working without tools halves the output.
 */
public record Recipe(String profession, Source source, Map<String, Double> outputs, Map<String, Double> inputs, double toolWear, String building) {
    public enum Source { DEPOSIT, WILDLIFE, FARM, CRAFT, SERVICE }

    public Recipe {
        outputs = Map.copyOf(outputs); inputs = Map.copyOf(inputs);
        toolWear = Math.max(0.0D, toolWear);
        building = building == null ? "" : building;
    }
}
