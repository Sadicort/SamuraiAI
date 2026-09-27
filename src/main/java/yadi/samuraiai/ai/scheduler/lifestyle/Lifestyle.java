package yadi.samuraiai.ai.scheduler.lifestyle;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import yadi.samuraiai.ai.scheduler.personality.PersonalityTraits;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.time.DayPeriod;

/**
 * A way of life: which routines an NPC of this kind wants in each part of the day (weights 0-100), the personality it starts
 * from, the working shifts it may keep (a shift moves its whole day) and the kind of group it forms. Pure data.
 */
public record Lifestyle(String id, Set<String> npcTypes, PersonalityTraits baseline, Map<DayPeriod, Map<RoutineType, Double>> weights,
                        List<Integer> shifts, String groupType) {
    public Lifestyle {
        npcTypes = Set.copyOf(npcTypes);
        Map<DayPeriod, Map<RoutineType, Double>> copy = new EnumMap<>(DayPeriod.class);
        weights.forEach((p, m) -> copy.put(p, Map.copyOf(m)));
        weights = copy;
        shifts = shifts.isEmpty() ? List.of(0) : List.copyOf(shifts);
    }

    public double weight(DayPeriod period, RoutineType routine) {
        Map<RoutineType, Double> m = weights.get(period);
        return m == null ? 0.0D : m.getOrDefault(routine, 0.0D);
    }

    /** The shift (in ticks) this particular NPC works, picked stably from its seed. */
    public int shiftFor(long seed) { return shifts.get((int) Math.floorMod(seed, (long) shifts.size())); }
}
