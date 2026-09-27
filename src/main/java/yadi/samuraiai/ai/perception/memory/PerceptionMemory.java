package yadi.samuraiai.ai.perception.memory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;

/**
 * An NPC's short-term perceptual memory: six kinds with independent lifetimes, entries that fade progressively (strength
 * falls quadratically with age, scaled by importance) and are forgotten below a threshold. Bounded per kind so a busy
 * place cannot grow it without limit. Server-thread only.
 */
public final class PerceptionMemory {
    private static final int MAX_PER_KIND = 64;
    private final Map<MemoryKind, Map<String, MemoryEntry>> entries = new EnumMap<>(MemoryKind.class);
    private long forgotten;

    public PerceptionMemory() { for (MemoryKind kind : MemoryKind.values()) entries.put(kind, new LinkedHashMap<>()); }

    /** Adds or reinforces an observation. Same key = same memory: its position and time are refreshed. */
    public MemoryEntry remember(MemoryKind kind, String key, UUID subject, String label, double x, double y, double z,
                                double vx, double vz, double importance, long tick, String detail) {
        Map<String, MemoryEntry> bucket = entries.get(kind);
        MemoryEntry entry = bucket.get(key);
        if (entry == null) {
            if (bucket.size() >= MAX_PER_KIND) evictWeakest(bucket);
            entry = new MemoryEntry(key, kind, subject, tick);
            bucket.put(key, entry);
        } else entry.reinforcements++;
        entry.label = label; entry.detail = detail == null ? "" : detail;
        entry.x = x; entry.y = y; entry.z = z; entry.vx = vx; entry.vz = vz;
        entry.importance = Math.max(0.05D, Math.min(1.0D, Math.max(importance, entry.reinforcements > 0 ? entry.importance : 0.0D)));
        entry.updatedTick = tick;
        entry.strength = entry.importance;
        return entry;
    }

    /** Recomputes every strength and forgets the ones that faded out. Returns how many were forgotten. */
    public int fade(long tick, PerceptionSettings s) {
        int removed = 0;
        for (MemoryKind kind : MemoryKind.values()) {
            double life = kind.lifetimeTicks(s);
            var iterator = entries.get(kind).values().iterator();
            while (iterator.hasNext()) {
                MemoryEntry entry = iterator.next();
                double effectiveLife = life * (0.5D + 0.5D * entry.importance);
                double remaining = Math.max(0.0D, 1.0D - entry.ageTicks(tick) / effectiveLife);
                entry.strength = entry.importance * remaining * remaining;
                if (entry.strength < s.memoryForgetThreshold()) { iterator.remove(); removed++; }
            }
        }
        forgotten += removed;
        return removed;
    }

    public Optional<MemoryEntry> get(MemoryKind kind, String key) { return Optional.ofNullable(entries.get(kind).get(key)); }
    public List<MemoryEntry> entries(MemoryKind kind) { return List.copyOf(entries.get(kind).values()); }
    public List<MemoryEntry> all() {
        List<MemoryEntry> all = new ArrayList<>();
        entries.values().forEach(bucket -> all.addAll(bucket.values()));
        return all;
    }

    /** Where this subject was last remembered, from the freshest visual, social or danger entry. */
    public Optional<MemoryEntry> lastKnown(UUID subject) {
        MemoryEntry best = null;
        for (MemoryKind kind : new MemoryKind[]{MemoryKind.VISUAL, MemoryKind.SOCIAL, MemoryKind.DANGER, MemoryKind.INTEREST})
            for (MemoryEntry entry : entries.get(kind).values())
                if (subject.equals(entry.subject) && (best == null || entry.updatedTick > best.updatedTick)) best = entry;
        return Optional.ofNullable(best);
    }

    public Optional<MemoryEntry> strongest(MemoryKind kind) {
        return entries.get(kind).values().stream().max(Comparator.comparingDouble(e -> e.strength));
    }

    public boolean knows(UUID subject) { return lastKnown(subject).isPresent(); }
    public int size() { return entries.values().stream().mapToInt(Map::size).sum(); }
    public int size(MemoryKind kind) { return entries.get(kind).size(); }
    public long forgottenTotal() { return forgotten; }

    /** Forgets one specific memory, e.g. a sound whose source the NPC went to look at. */
    public boolean forget(MemoryKind kind, String key) { return entries.get(kind).remove(key) != null; }

    public void forget(UUID subject) { entries.values().forEach(bucket -> bucket.values().removeIf(e -> subject.equals(e.subject))); }
    public void clear() { entries.values().forEach(Map::clear); }

    private static void evictWeakest(Map<String, MemoryEntry> bucket) {
        String weakest = null;
        double lowest = Double.MAX_VALUE;
        for (MemoryEntry entry : bucket.values()) if (entry.strength < lowest) { lowest = entry.strength; weakest = entry.key; }
        if (weakest != null) bucket.remove(weakest);
    }
}
