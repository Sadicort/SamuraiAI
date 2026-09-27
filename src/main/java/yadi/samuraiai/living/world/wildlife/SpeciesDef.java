package yadi.samuraiai.living.world.wildlife;

import java.util.Map;
import yadi.samuraiai.living.world.regions.RegionType;

/**
 * An animal species as the world simulates it: how many a region of each type can hold, how fast it grows (logistic rate
 * per day), what it preys on, what hunting, fishing or husbandry yields per animal (resource id → amount), how much danger
 * a hundred of them add to a region, and whether it thrives on a village's food surplus (rats). Pure data.
 */
public record SpeciesDef(String id, String name, Map<RegionType, Double> habitat, double growthPerDay, String prey, double predationPerDay,
                         Map<String, Double> yields, double dangerPer100, boolean pest, boolean domestic) {
    public SpeciesDef {
        habitat = Map.copyOf(habitat);
        yields = Map.copyOf(yields);
        growthPerDay = Math.max(0.0D, growthPerDay);
        predationPerDay = Math.max(0.0D, predationPerDay);
        dangerPer100 = Math.max(0.0D, dangerPer100);
        prey = prey == null ? "" : prey;
    }

    public double capacity(RegionType type) { return habitat.getOrDefault(type, 0.0D); }
}
