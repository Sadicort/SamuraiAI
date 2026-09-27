package yadi.samuraiai.living.quest.memory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.living.quest.runtime.Quest;

/**
 * Quest history: every quest accepted, completed, failed, abandoned or resolved by the world, with the path chosen, the
 * decisions and the ending. Kept per player (what the world remembers you did) and as totals.
 */
public final class QuestHistory {
    public record Entry(UUID quest, String template, String title, Quest.State outcome, String path, List<String> decisions, String ending, long minute, UUID settlement) {
        public Entry { decisions = List.copyOf(decisions); }
    }

    private final Map<UUID, Deque<Entry>> byPlayer = new java.util.LinkedHashMap<>();
    private final Deque<Entry> world = new ArrayDeque<>();
    private final Map<Quest.State, Long> totals = new EnumMap<>(Quest.State.class);
    private int perPlayer = 200, worldMax = 1000;
    private boolean dirty;

    public void configure(int playerMax, int max) { perPlayer = Math.max(8, playerMax); worldMax = Math.max(16, max); }

    public void record(Quest q, long minute) {
        List<String> decisions = new ArrayList<>();
        for (Quest.Decision d : q.decisions()) decisions.add(d.choice());
        Entry e = new Entry(q.id(), q.template(), q.title(), q.state(), q.path() == null ? "" : q.path().name(), decisions, q.text(), minute, q.settlement());
        world.addLast(e);
        while (world.size() > worldMax) world.removeFirst();
        for (UUID p : q.players().keySet()) {
            Deque<Entry> d = byPlayer.computeIfAbsent(p, k -> new ArrayDeque<>());
            d.addLast(e);
            while (d.size() > perPlayer) d.removeFirst();
        }
        totals.merge(q.state(), 1L, Long::sum);
        dirty = true;
    }

    public void accepted(Quest q, UUID player) { totals.merge(Quest.State.ACTIVE, 1L, Long::sum); dirty = true; }

    public List<Entry> of(UUID player) { return new ArrayList<>(byPlayer.getOrDefault(player, new ArrayDeque<>())); }
    public List<Entry> world(int limit) { List<Entry> all = new ArrayList<>(world); return all.size() > limit ? new ArrayList<>(all.subList(all.size() - limit, all.size())) : all; }
    public long total(Quest.State s) { return totals.getOrDefault(s, 0L); }
    public Map<Quest.State, Long> totals() { return Map.copyOf(totals); }
    public Map<UUID, Deque<Entry>> players() { return byPlayer; }
    public void restore(UUID player, Entry e) { if (player == null) world.addLast(e); else byPlayer.computeIfAbsent(player, k -> new ArrayDeque<>()).addLast(e); }
    public void restoreTotals(Map<Quest.State, Long> t) { totals.clear(); totals.putAll(t); }
    public boolean dirty() { return dirty; }
    public void clean() { dirty = false; }
    public void clear() { byPlayer.clear(); world.clear(); totals.clear(); dirty = false; }
}
