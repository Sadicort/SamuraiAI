package yadi.samuraiai.ai.memory.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The tiers between a decision and the full history: a short cache of recent retrieval results (expires in ticks), a hot set
 * of the most used memories, a warm set of recently touched ones (LRU) and, below them, everything else in the runtime (cold)
 * and on disk (storage). Tiers only decide where a lookup starts and how it is counted; a memory never lives in the cache alone.
 */
public final class MemoryCache {
    public enum Tier { SHORT, HOT, WARM, COLD }

    private record Entry(List<UUID> ids, long expires) { }

    private final LinkedHashMap<String, Entry> results;
    private final LinkedHashMap<UUID, Long> warm = new LinkedHashMap<>(16, 0.75f, true);
    private final Map<UUID, Integer> uses = new HashMap<>();
    private final java.util.Set<UUID> hot = new java.util.LinkedHashSet<>();
    private final int shortSize, hotSize, warmSize, hotThreshold;
    private long shortHits, shortMisses, hotHits, warmHits, coldHits;

    public MemoryCache(int shortSize, int hotSize, int warmSize, int hotThreshold) {
        this.shortSize = shortSize; this.hotSize = hotSize; this.warmSize = warmSize; this.hotThreshold = hotThreshold;
        this.results = new LinkedHashMap<>(16, 0.75f, true);
    }

    /** A cached result list, or null when absent or expired. */
    public List<UUID> lookup(String key, long now) {
        Entry entry = results.get(key);
        if (entry == null || entry.expires() < now) { if (entry != null) results.remove(key); shortMisses++; return null; }
        shortHits++;
        return entry.ids();
    }

    public void store(String key, List<UUID> ids, long now, int ttlTicks) {
        if (ttlTicks <= 0) return;
        results.put(key, new Entry(List.copyOf(ids), now + ttlTicks));
        while (results.size() > shortSize) { var it = results.entrySet().iterator(); it.next(); it.remove(); }
    }

    /** Any change to the memories makes cached answers stale. */
    public void invalidate() { results.clear(); }

    /** Records that a memory was used: it becomes warm, and hot once used often enough. */
    public void touched(UUID id, long now) {
        warm.put(id, now);
        while (warm.size() > warmSize) { var it = warm.entrySet().iterator(); it.next(); it.remove(); }
        int count = uses.merge(id, 1, Integer::sum);
        if (count >= hotThreshold) {
            hot.add(id);
            if (hot.size() > hotSize) { UUID coldest = null; int least = Integer.MAX_VALUE; for (UUID h : hot) { int u = uses.getOrDefault(h, 0); if (u < least) { least = u; coldest = h; } } if (coldest != null) hot.remove(coldest); }
        }
    }

    public Tier tierOf(UUID id) { return hot.contains(id) ? Tier.HOT : warm.containsKey(id) ? Tier.WARM : Tier.COLD; }

    /** Counts which tier served a retrieved memory (before it is touched). */
    public void served(UUID id) {
        switch (tierOf(id)) { case HOT -> hotHits++; case WARM -> warmHits++; default -> coldHits++; }
    }

    public void forget(UUID id) { warm.remove(id); hot.remove(id); uses.remove(id); }

    public List<UUID> hot() { return new ArrayList<>(hot); }
    public int warmSize() { return warm.size(); }
    public int shortSize() { return results.size(); }
    public long shortHits() { return shortHits; }
    public long shortMisses() { return shortMisses; }
    public long hotHits() { return hotHits; }
    public long warmHits() { return warmHits; }
    public long coldHits() { return coldHits; }
    public double hitRate() { long total = shortHits + shortMisses; return total == 0 ? 0 : (double) shortHits / total; }
    public double tierHitRate() { long total = hotHits + warmHits + coldHits; return total == 0 ? 0 : (double) (hotHits + warmHits) / total; }
    public void clear() { results.clear(); warm.clear(); hot.clear(); uses.clear(); }
}
