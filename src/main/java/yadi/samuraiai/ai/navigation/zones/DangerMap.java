package yadi.samuraiai.ai.navigation.zones;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import yadi.samuraiai.ai.navigation.graph.NavPos;

/**
 * Dynamic hazards layered over the static block-derived danger of the graph. Zones expire, sources are
 * polled by the runtime at a low frequency, and forbidden boxes are hard barriers.
 */
public final class DangerMap {
    private final List<DangerZone> zones = new ArrayList<>();
    private final List<ForbiddenZone> forbidden = new ArrayList<>();
    private final java.util.Map<String, DangerSource> sources = new java.util.concurrent.ConcurrentHashMap<>();
    private long changes;

    public synchronized void add(DangerZone zone) { zones.add(zone); changes++; }
    public synchronized void addForbidden(ForbiddenZone zone) {
        forbidden.removeIf(existing -> existing.id().equals(zone.id())); forbidden.add(zone); changes++;
    }
    public synchronized boolean removeForbidden(String id) { changes++; return forbidden.removeIf(zone -> zone.id().equals(id)); }
    /** Registers a named source. Its zones are replaced, not accumulated, each time the map is refreshed. */
    public void registerSource(String id, DangerSource source) { sources.put(java.util.Objects.requireNonNull(id), java.util.Objects.requireNonNull(source)); }
    public void registerSource(DangerSource source) { registerSource("source-" + sources.size(), source); }
    public long changes() { return changes; }

    /** Pulls fresh zones from registered sources, replacing what each source reported last time. */
    public void refresh(String dimension, long tick) {
        for (var entry : sources.entrySet()) {
            String tag = "src:" + entry.getKey();
            java.util.List<DangerZone> collected = new ArrayList<>();
            try { entry.getValue().collect(dimension, tick, zone -> collected.add(zone.withSource(tag))); }
            catch (RuntimeException error) { yadi.samuraiai.logging.SamuraiLogger.CORE.warn("Navigation danger source failed", error); continue; }
            synchronized (this) {
                zones.removeIf(zone -> zone.source().equals(tag) && zone.dimension().equals(dimension));
                zones.addAll(collected);
                changes++;
            }
        }
    }

    public synchronized double dynamicDanger(String dimension, NavPos pos, long tick) {
        double total = 0;
        for (DangerZone zone : zones)
            if (zone.dimension().equals(dimension) && !zone.expired(tick)) total += zone.scoreAt(pos);
        return total;
    }

    public synchronized boolean isForbidden(String dimension, NavPos pos) {
        for (ForbiddenZone zone : forbidden) if (zone.contains(dimension, pos)) return true;
        return false;
    }

    public synchronized int prune(long tick) {
        int before = zones.size();
        zones.removeIf(zone -> zone.expired(tick));
        int removed = before - zones.size();
        if (removed > 0) changes++;
        return removed;
    }

    public synchronized List<DangerZone> activeZones(String dimension, long tick) {
        return zones.stream().filter(zone -> zone.dimension().equals(dimension) && !zone.expired(tick)).toList();
    }
    public synchronized List<ForbiddenZone> forbiddenZones() { return List.copyOf(forbidden); }
    public synchronized boolean anyDynamic() { return !zones.isEmpty(); }
    public synchronized void clear() { zones.clear(); forbidden.clear(); changes++; }
}
