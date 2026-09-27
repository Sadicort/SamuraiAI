package yadi.samuraiai.living.family.traditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import yadi.samuraiai.living.family.family_memory.FamilyMemoryEntry;
import yadi.samuraiai.living.family.heritage.Heirloom;
import yadi.samuraiai.living.family.registry.FamilyRecord;

/**
 * Family traditions emerge from repeated history rather than being assigned: a trade practised in two or more generations,
 * an heirloom handed down more than once, the ancestors honoured at Obon in two different years, a martial school followed
 * across generations. Returns the traditions not yet recorded.
 */
public final class TraditionDetector {
    public List<String> detect(FamilyRecord f, List<Heirloom> heirlooms, int martialGenerations, int minutesPerYear) {
        List<String> found = new ArrayList<>();
        Map<String, Set<String>> generationsByProfession = new HashMap<>();
        for (var e : f.professionsByGeneration().entrySet()) {
            int colon = e.getKey().indexOf(':');
            if (colon <= 0 || e.getValue() <= 0) continue;
            generationsByProfession.computeIfAbsent(e.getKey().substring(colon + 1), k -> new HashSet<>()).add(e.getKey().substring(0, colon));
        }
        generationsByProfession.forEach((p, gens) -> { if (gens.size() >= 2) found.add("oficio:" + p); });
        for (Heirloom h : heirlooms) if (h.transfers().size() >= 2) found.add("legado:" + h.name());
        Set<Long> obonYears = new HashSet<>();
        for (FamilyMemoryEntry e : f.memory()) if (e.kind() == FamilyMemoryEntry.Kind.TRADITION && e.text().contains("antepasados")) obonYears.add(Math.floorDiv(e.minute(), (long) Math.max(1, minutesPerYear)));
        if (obonYears.size() >= 2) found.add("honra a los antepasados en Obon");
        if (martialGenerations >= 2) found.add("camino del guerrero");
        found.removeIf(f.traditions()::contains);
        return found;
    }
}
