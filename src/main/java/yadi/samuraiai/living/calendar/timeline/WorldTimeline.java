package yadi.samuraiai.living.calendar.timeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import yadi.samuraiai.living.core.Provenance;

/**
 * The official chronology of Deiliora: every dated fact the living world considers history. Entries are kept in time order
 * with an index by scope, so "the history of this village" or "of this family" is a lookup, never a scan of everything.
 * When the timeline is over its capacity the least significant old entries are dropped; entries at or above
 * {@code keepSignificance} are never dropped.
 */
public final class WorldTimeline {
    private final Map<UUID, TimelineEntry> entries = new LinkedHashMap<>();
    private final Map<String, List<UUID>> byScope = new HashMap<>();
    private final List<TimelineEntry> ordered = new ArrayList<>();
    private int capacity;
    private double keepSignificance;
    private long dropped;
    private boolean dirty;

    public WorldTimeline(int capacity, double keepSignificance) { configure(capacity, keepSignificance); }

    public void configure(int newCapacity, double newKeep) { this.capacity = Math.max(64, newCapacity); this.keepSignificance = Math.max(0.0D, Math.min(1.0D, newKeep)); }

    /** Records a fact. The same id twice is ignored (records are idempotent). Returns the entry kept, if any. */
    public Optional<TimelineEntry> record(TimelineEntry entry) {
        if (entry == null || entries.containsKey(entry.id())) return Optional.empty();
        entries.put(entry.id(), entry);
        int at = ordered.size();
        while (at > 0 && ordered.get(at - 1).minute() > entry.minute()) at--;
        ordered.add(at, entry);
        for (String scope : entry.scopes()) byScope.computeIfAbsent(scope, k -> new ArrayList<>()).add(entry.id());
        dirty = true;
        trim();
        return Optional.ofNullable(entries.get(entry.id()));
    }

    public TimelineEntry record(long minute, TimelineCategory category, String title, String detail, Set<String> scopes, double significance, Provenance source) {
        TimelineEntry e = new TimelineEntry(UUID.randomUUID(), minute, category, title, detail, scopes, significance, source);
        record(e);
        return e;
    }

    private void trim() {
        if (entries.size() <= capacity) return;
        // drop the least significant among the oldest half, down to 90% of capacity so trimming is rare (amortised)
        int excess = entries.size() - capacity * 9 / 10;
        List<TimelineEntry> candidates = new ArrayList<>();
        int half = Math.max(1, ordered.size() / 2);
        for (int i = 0; i < half; i++) if (ordered.get(i).significance() < keepSignificance) candidates.add(ordered.get(i));
        candidates.sort((a, b) -> Double.compare(a.significance(), b.significance()));
        java.util.Set<UUID> doomed = new java.util.HashSet<>();
        for (int i = 0; i < Math.min(excess, candidates.size()); i++) doomed.add(candidates.get(i).id());
        if (doomed.isEmpty()) return;
        ordered.removeIf(e -> doomed.contains(e.id()));
        for (UUID id : doomed) {
            TimelineEntry e = entries.remove(id);
            if (e == null) continue;
            for (String scope : e.scopes()) { List<UUID> l = byScope.get(scope); if (l != null) { l.remove(id); if (l.isEmpty()) byScope.remove(scope); } }
            dropped++;
        }
        dirty = true;
    }

    public Optional<TimelineEntry> get(UUID id) { return Optional.ofNullable(entries.get(id)); }
    public int size() { return entries.size(); }
    public long dropped() { return dropped; }
    public Collection<TimelineEntry> all() { return List.copyOf(ordered); }

    /** The entries of one scope, oldest first, optionally limited to the last {@code limit}. */
    public List<TimelineEntry> of(String scope, int limit) {
        List<UUID> ids = byScope.getOrDefault(scope, List.of());
        List<TimelineEntry> out = new ArrayList<>(ids.size());
        for (UUID id : ids) { TimelineEntry e = entries.get(id); if (e != null) out.add(e); }
        out.sort((a, b) -> Long.compare(a.minute(), b.minute()));
        return limit > 0 && out.size() > limit ? new ArrayList<>(out.subList(out.size() - limit, out.size())) : out;
    }

    /** Entries in a time window (inclusive), filtered. */
    public List<TimelineEntry> between(long from, long to, Predicate<TimelineEntry> filter, int limit) {
        List<TimelineEntry> out = new ArrayList<>();
        for (TimelineEntry e : ordered) {
            if (e.minute() < from) continue;
            if (e.minute() > to) break;
            if (filter == null || filter.test(e)) out.add(e);
            if (limit > 0 && out.size() >= limit) break;
        }
        return out;
    }

    public List<TimelineEntry> latest(int limit) {
        int from = Math.max(0, ordered.size() - Math.max(1, limit));
        return new ArrayList<>(ordered.subList(from, ordered.size()));
    }

    public Set<String> scopes() { return Set.copyOf(byScope.keySet()); }
    public boolean dirty() { return dirty; }
    public void clean() { dirty = false; }
    public void clear() { entries.clear(); byScope.clear(); ordered.clear(); dirty = false; dropped = 0; }
}
