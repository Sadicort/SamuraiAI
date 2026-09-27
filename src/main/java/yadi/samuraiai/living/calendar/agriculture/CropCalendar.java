package yadi.samuraiai.living.calendar.agriculture;

import java.util.Map;
import java.util.Set;

/**
 * The agricultural year of one crop: in which months it is planted, grows, is harvested and rests, plus what weather it
 * needs while growing ({@code wantsRain}: share of rainy days it thrives on) and how badly frost and storms hurt it.
 */
public record CropCalendar(String resource, Set<Integer> planting, Set<Integer> growth, Set<Integer> harvest, double wantsRain, double frostDamage, double stormDamage) {
    public CropCalendar {
        planting = Set.copyOf(planting); growth = Set.copyOf(growth); harvest = Set.copyOf(harvest);
        wantsRain = Math.max(0.0D, Math.min(1.0D, wantsRain));
        frostDamage = Math.max(0.0D, frostDamage); stormDamage = Math.max(0.0D, stormDamage);
    }

    public CropStage stage(int month) {
        if (harvest.contains(month)) return CropStage.HARVEST;
        if (planting.contains(month)) return CropStage.PLANTING;
        if (growth.contains(month)) return CropStage.GROWTH;
        return CropStage.REST;
    }

    public static Map.Entry<String, CropCalendar> entry(CropCalendar c) { return Map.entry(c.resource(), c); }
}
