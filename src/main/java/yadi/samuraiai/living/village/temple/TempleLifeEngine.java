package yadi.samuraiai.living.village.temple;

import java.util.List;
import java.util.Map;
import yadi.samuraiai.living.core.DayPhase;
import yadi.samuraiai.living.village.buildings.Building;
import yadi.samuraiai.living.village.life.VillageEventKind;
import yadi.samuraiai.living.village.life.VillageEventRecord;
import yadi.samuraiai.living.village.runtime.Village;

/**
 * Temple life: the dawn and dusk rituals, teaching in the morning, ceremonies on holy days. While a ritual runs the temple
 * draws the devout (a PRAYER bias for everyone, stronger for monks); in the morning monks teach (MEDITATE/SOCIAL at the
 * temple); a ceremony event makes it the centre of the day.
 */
public final class TempleLifeEngine {
    public enum Change { NONE, RITUAL_STARTED, RITUAL_ENDED }

    public Change update(Village village, DayPhase phase, List<VillageEventRecord> events) {
        Building temple = village.mainTemple() == null ? null : village.buildings().get(village.mainTemple());
        boolean ceremony = events.stream().anyMatch(e -> e.kind() == VillageEventKind.CEREMONY || e.kind() == VillageEventKind.PROCESSION);
        boolean ritual = temple != null && temple.usable() && (phase == DayPhase.DAWN || phase == DayPhase.SUNSET || ceremony);
        if (ritual == village.ritualActive()) return Change.NONE;
        village.ritualActive(ritual);
        return ritual ? Change.RITUAL_STARTED : Change.RITUAL_ENDED;
    }

    /** Bias the temple adds to one citizen now. */
    public Map<String, Double> bias(Village village, DayPhase phase, boolean monk) {
        if (village.mainTemple() == null) return Map.of();
        if (village.ritualActive()) return monk ? Map.of("PRAYER", 45.0) : Map.of("PRAYER", 12.0);
        if (monk && phase == DayPhase.MORNING) return Map.of("MEDITATE", 15.0, "SOCIAL", 10.0);   // teaching hour
        return Map.of();
    }
}
