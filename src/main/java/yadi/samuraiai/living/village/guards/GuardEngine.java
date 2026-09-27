package yadi.samuraiai.living.village.guards;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.village.security.SecurityState;

/**
 * The guard: guards and samurai are citizens with a watch. A share of them keeps the night watch (balanced as the roster
 * changes); the watch changes at 06:00 and 18:00; on duty a guard wants GUARD and PATROL, off duty TRAINING and rest; when
 * the village is in danger or under attack everyone is on duty.
 */
public final class GuardEngine {
    public static final Set<String> GUARD_PROFESSIONS = Set.of("guard", "samurai");

    /** Rebalances who keeps the night watch. Returns true when the roster changed. */
    public boolean roster(Village village, List<UUID> guards, double nightShare) {
        boolean changed = village.nightWatch().keySet().removeIf(g -> !guards.contains(g));
        int wantNight = (int) Math.round(guards.size() * nightShare);
        int night = (int) village.nightWatch().values().stream().filter(b -> b).count();
        for (UUID g : guards) {
            if (village.nightWatch().containsKey(g)) continue;
            boolean n = night < wantNight;
            village.nightWatch().put(g, n);
            if (n) night++;
            changed = true;
        }
        return changed;
    }

    public boolean nightWatch(Village village, UUID guard) { return village.nightWatch().getOrDefault(guard, false); }

    /** Whether a guard is on duty at a minute of the day. */
    public boolean onDuty(Village village, UUID guard, int minuteOfDay) {
        if (village.security().state().threatened()) return true;
        boolean day = minuteOfDay >= 6 * 60 && minuteOfDay < 18 * 60;
        return nightWatch(village, guard) != day;
    }

    public int onDutyCount(Village village, List<UUID> guards, int minuteOfDay) {
        int n = 0;
        for (UUID g : guards) if (onDuty(village, g, minuteOfDay)) n++;
        return n;
    }

    public Map<String, Double> bias(Village village, UUID guard, int minuteOfDay) {
        SecurityState s = village.security().state();
        if (s == SecurityState.ATTACK) return Map.of("GUARD", 70.0, "PATROL", 60.0, "SLEEP", -80.0, "REST", -60.0);
        if (onDuty(village, guard, minuteOfDay)) return s == SecurityState.PEACE ? Map.of("GUARD", 25.0, "PATROL", 20.0) : Map.of("GUARD", 40.0, "PATROL", 35.0);
        return Map.of("TRAINING", 10.0, "GUARD", -15.0);
    }

    public static List<UUID> guards(Village village, Map<UUID, String> professions) {
        List<UUID> out = new ArrayList<>();
        for (UUID c : village.citizens()) if (GUARD_PROFESSIONS.contains(professions.getOrDefault(c, ""))) out.add(c);
        return out;
    }
}
