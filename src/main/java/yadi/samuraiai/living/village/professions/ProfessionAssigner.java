package yadi.samuraiai.living.village.professions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import yadi.samuraiai.living.village.buildings.BuildingKind;
import yadi.samuraiai.living.village.runtime.Village;

/**
 * Decides the local profession of a citizen who has none. The NPC's type decides first (a guard NPC guards); otherwise the
 * Economy's most needed profession; otherwise the village's own quotas — a share of farmers, a guard for every eight people,
 * a cook, a smith, a carpenter, a woodcutter, a monk if there is a temple, a merchant if there is a market — picking the
 * profession furthest below its quota. It never overrides a profession someone already has.
 */
public final class ProfessionAssigner {
    public record Choice(String profession, String reason) { }

    private static final Map<String, Double> QUOTAS = new LinkedHashMap<>();
    static {
        QUOTAS.put("farmer", 0.30D); QUOTAS.put("guard", 1.0D / 8); QUOTAS.put("woodcutter", 1.0D / 12); QUOTAS.put("cook", 1.0D / 15); QUOTAS.put("fisherman", 1.0D / 12);
        QUOTAS.put("blacksmith", 1.0D / 20); QUOTAS.put("carpenter", 1.0D / 20); QUOTAS.put("monk", 1.0D / 20); QUOTAS.put("merchant", 1.0D / 15);
        QUOTAS.put("hunter", 1.0D / 20); QUOTAS.put("herbalist", 1.0D / 25); QUOTAS.put("weaver", 1.0D / 25); QUOTAS.put("miner", 1.0D / 25);
    }

    public Choice choose(Village village, Map<String, Integer> current, Optional<String> byType, Optional<String> needed) {
        if (byType.isPresent()) return new Choice(byType.get(), "tipo de NPC");
        if (needed.isPresent()) return new Choice(needed.get(), "la economía de la aldea lo necesita");
        int population = Math.max(1, village.citizens().size());
        String best = "farmer";
        double bestGap = -Double.MAX_VALUE;
        for (var e : QUOTAS.entrySet()) {
            String p = e.getKey();
            if (p.equals("monk") && village.mainTemple() == null) continue;
            if (p.equals("merchant") && village.market() == null) continue;
            if (p.equals("fisherman") && village.buildingsOf(BuildingKind.DOCK).isEmpty() && population < 12) continue;
            if (p.equals("miner") && village.buildingsOf(BuildingKind.MINE).isEmpty()) continue;
            double gap = e.getValue() * population - current.getOrDefault(p, 0);
            if (gap > bestGap) { bestGap = gap; best = p; }
        }
        return new Choice(best, "cuotas de la aldea");
    }
}
