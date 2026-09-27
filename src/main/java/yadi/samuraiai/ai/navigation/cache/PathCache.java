package yadi.samuraiai.ai.navigation.cache;

import java.util.*;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.pathfinding.NavigationPath;

/**
 * Bounded LRU of complete paths. Patrols repeat the same legs, so an exact hit is common, and a walker that
 * is already on a cached path reuses its remaining suffix. Entries expire by tick and are invalidated by
 * chunk when the world changes under them. Server-thread only.
 */
public final class PathCache {
    public record Key(String dimension, NavPos start, NavPos goal, int signature) { }
    private record GoalKey(String dimension, NavPos goal, int signature) { }
    private static final class Entry {
        final Key key; final NavigationPath path; final long expiresTick;
        Entry(Key key, NavigationPath path, long expiresTick) { this.key = key; this.path = path; this.expiresTick = expiresTick; }
    }

    private final int capacity;
    private final int ttlTicks;
    private final LinkedHashMap<Key, Entry> entries = new LinkedHashMap<>(64, 0.75f, true);
    private final Map<Long, Set<Key>> byChunk = new HashMap<>();
    private final Map<GoalKey, Set<Key>> byGoal = new HashMap<>();
    private long hits, suffixHits, misses, evictions, invalidated, expired;

    public PathCache(int capacity, int ttlTicks) { this.capacity = capacity; this.ttlTicks = ttlTicks; }

    public Optional<NavigationPath> get(String dimension, NavPos start, NavPos goal, int signature, long tick) {
        if (capacity == 0) return Optional.empty();
        Entry entry = entries.get(new Key(dimension, start, goal, signature));
        if (entry != null && live(entry, tick)) { hits++; return Optional.of(entry.path); }
        // A walker already standing on a cached route to the same goal only needs the rest of it.
        Set<Key> candidates = byGoal.get(new GoalKey(dimension, goal, signature));
        if (candidates != null) {
            for (Key key : List.copyOf(candidates)) {
                Entry candidate = entries.get(key);
                if (candidate == null || !live(candidate, tick)) continue;
                int index = candidate.path.indexOf(start);
                if (index > 0 && candidate.path.size() - index >= 2) { suffixHits++; return Optional.of(candidate.path.suffixFrom(index, tick)); }
            }
        }
        misses++;
        return Optional.empty();
    }

    /** Only complete paths are cached: a partial path describes a search limit, not the world. */
    public void put(String dimension, NavPos start, NavPos goal, int signature, NavigationPath path, long tick) {
        if (capacity == 0 || path.partial()) return;
        Key key = new Key(dimension, start, goal, signature);
        remove(key);
        entries.put(key, new Entry(key, path, tick + ttlTicks));
        for (long chunk : path.chunks()) byChunk.computeIfAbsent(chunk, k -> new HashSet<>()).add(key);
        byGoal.computeIfAbsent(new GoalKey(dimension, goal, signature), k -> new HashSet<>()).add(key);
        while (entries.size() > capacity) {
            Key eldest = entries.keySet().iterator().next();
            remove(eldest); evictions++;
        }
    }

    public int invalidate(NavPos changed) {
        int removed = invalidateChunk(changed.chunkX(), changed.chunkZ());
        int lx = changed.x() & 15, lz = changed.z() & 15;
        if (lx <= 1) removed += invalidateChunk(changed.chunkX() - 1, changed.chunkZ());
        if (lx >= 14) removed += invalidateChunk(changed.chunkX() + 1, changed.chunkZ());
        if (lz <= 1) removed += invalidateChunk(changed.chunkX(), changed.chunkZ() - 1);
        if (lz >= 14) removed += invalidateChunk(changed.chunkX(), changed.chunkZ() + 1);
        return removed;
    }

    public int invalidateChunk(int chunkX, int chunkZ) {
        Set<Key> keys = byChunk.get(NavPos.chunkKey(chunkX, chunkZ));
        if (keys == null) return 0;
        int removed = 0;
        for (Key key : List.copyOf(keys)) { if (remove(key)) removed++; }
        invalidated += removed;
        return removed;
    }

    public int purgeExpired(long tick) {
        int removed = 0;
        for (Entry entry : List.copyOf(entries.values())) if (entry.expiresTick <= tick && remove(entry.key)) { removed++; expired++; }
        return removed;
    }

    public void clear() { entries.clear(); byChunk.clear(); byGoal.clear(); }
    public int size() { return entries.size(); }
    public Stats stats() { return new Stats(entries.size(), capacity, hits, suffixHits, misses, evictions, invalidated, expired); }
    public record Stats(int size, int capacity, long hits, long suffixHits, long misses, long evictions, long invalidated, long expired) {
        public double hitRate() { long total = hits + suffixHits + misses; return total == 0 ? 0 : (double) (hits + suffixHits) / total; }
    }

    private boolean live(Entry entry, long tick) {
        if (entry.expiresTick <= tick) { remove(entry.key); expired++; return false; }
        return true;
    }

    private boolean remove(Key key) {
        Entry entry = entries.remove(key);
        if (entry == null) return false;
        for (long chunk : entry.path.chunks()) {
            Set<Key> keys = byChunk.get(chunk);
            if (keys != null) { keys.remove(key); if (keys.isEmpty()) byChunk.remove(chunk); }
        }
        GoalKey goal = new GoalKey(key.dimension(), key.goal(), key.signature());
        Set<Key> keys = byGoal.get(goal);
        if (keys != null) { keys.remove(key); if (keys.isEmpty()) byGoal.remove(goal); }
        return true;
    }
}
