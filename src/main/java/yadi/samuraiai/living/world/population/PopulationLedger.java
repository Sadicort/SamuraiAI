package yadi.samuraiai.living.world.population;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The world's view of population: how many people each settlement reports (the Village Engine owns who they are), the
 * professions counted there, and a bounded log of arrivals, departures, migrations, births and deaths. Region totals are
 * sums of their settlements.
 */
public final class PopulationLedger {
    public enum ChangeKind { ARRIVAL, DEPARTURE, MIGRATION, BIRTH, DEATH, MISSING }

    public record Change(long minute, ChangeKind kind, UUID settlement, UUID person, String name, String detail) { }

    private final Map<UUID, Integer> bySettlement = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> professions = new HashMap<>();
    private final Deque<Change> log = new ArrayDeque<>();
    private final Map<ChangeKind, Long> totals = new HashMap<>();
    private int logMax = 2000;
    private boolean dirty;

    public void configure(int max) { logMax = Math.max(16, max); }

    public void report(UUID settlement, int count, Map<String, Integer> professionCounts) {
        Integer old = bySettlement.put(settlement, Math.max(0, count));
        professions.put(settlement, professionCounts == null ? Map.of() : Map.copyOf(professionCounts));
        if (old == null || old != count) dirty = true;
    }

    public void record(Change change) {
        log.addLast(change);
        while (log.size() > logMax) log.removeFirst();
        totals.merge(change.kind(), 1L, Long::sum);
        dirty = true;
    }

    public int of(UUID settlement) { return bySettlement.getOrDefault(settlement, 0); }
    public Map<String, Integer> professionsOf(UUID settlement) { return professions.getOrDefault(settlement, Map.of()); }
    public int total() { int n = 0; for (int v : bySettlement.values()) n += v; return n; }
    public List<Change> log() { return List.copyOf(log); }
    public long total(ChangeKind kind) { return totals.getOrDefault(kind, 0L); }
    public Map<ChangeKind, Long> totals() { return Map.copyOf(totals); }
    public Map<UUID, Integer> settlements() { return Map.copyOf(bySettlement); }
    public boolean dirty() { return dirty; }
    public void clean() { dirty = false; }
    public void restoreTotals(Map<ChangeKind, Long> saved) { totals.clear(); totals.putAll(saved); }
    public void clear() { bySettlement.clear(); professions.clear(); log.clear(); totals.clear(); dirty = false; }
}
