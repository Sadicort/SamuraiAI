package yadi.samuraiai.living.economy.memory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Trade memory: a daily snapshot of each settlement's economy (prices, stock, production, consumption, imports, exports,
 * shortfalls) kept for {@code days} days, and a log of notable economic happenings (scarcities, lost caravans, blocked
 * routes, great and bad harvests, agreements). Quests, debugging and NPC knowledge read it.
 */
public final class TradeMemory {
    public record Snapshot(long day, Map<String, Double> prices, Map<String, Double> stock, Map<String, Double> produced, Map<String, Double> consumed,
                           Map<String, Double> imported, Map<String, Double> exported, Map<String, Double> shortfall, double treasury) {
        public Snapshot {
            prices = Map.copyOf(prices); stock = Map.copyOf(stock); produced = Map.copyOf(produced); consumed = Map.copyOf(consumed);
            imported = Map.copyOf(imported); exported = Map.copyOf(exported); shortfall = Map.copyOf(shortfall);
        }
    }

    public record Happening(long minute, UUID settlement, String kind, String detail) { }

    private final Map<UUID, Deque<Snapshot>> daily = new HashMap<>();
    private final Deque<Happening> happenings = new ArrayDeque<>();
    private int days = 60, maxHappenings = 500;
    private boolean dirty;

    public void configure(int keepDays, int keepHappenings) { days = Math.max(2, keepDays); maxHappenings = Math.max(16, keepHappenings); }

    public void snapshot(UUID settlement, Snapshot s) {
        Deque<Snapshot> q = daily.computeIfAbsent(settlement, k -> new ArrayDeque<>());
        if (!q.isEmpty() && q.peekLast().day() == s.day()) q.pollLast();
        q.addLast(s);
        while (q.size() > days) q.removeFirst();
        dirty = true;
    }

    public void happened(Happening h) { happenings.addLast(h); while (happenings.size() > maxHappenings) happenings.removeFirst(); dirty = true; }

    public List<Snapshot> history(UUID settlement) { return new ArrayList<>(daily.getOrDefault(settlement, new ArrayDeque<>())); }
    public List<Happening> happenings(UUID settlement, int limit) {
        List<Happening> out = new ArrayList<>();
        for (Happening h : happenings) if (settlement == null || settlement.equals(h.settlement())) out.add(h);
        return out.size() > limit ? new ArrayList<>(out.subList(out.size() - limit, out.size())) : out;
    }
    /** Average price of a resource in a settlement over the remembered days (NaN when unknown). */
    public double averagePrice(UUID settlement, String resource) {
        double sum = 0; int n = 0;
        for (Snapshot s : daily.getOrDefault(settlement, new ArrayDeque<>())) { Double p = s.prices().get(resource); if (p != null) { sum += p; n++; } }
        return n == 0 ? Double.NaN : sum / n;
    }
    public Map<UUID, Deque<Snapshot>> all() { return new LinkedHashMap<>(daily); }
    public List<Happening> allHappenings() { return new ArrayList<>(happenings); }
    public boolean dirty() { return dirty; }
    public void clean() { dirty = false; }
    public void clear() { daily.clear(); happenings.clear(); dirty = false; }
}
