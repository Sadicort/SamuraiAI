package yadi.samuraiai.ai.scheduler.zone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** All zones of the world, by id. Every change bumps a version so caches built on the zone set know when to rebuild. */
public final class ZoneRegistry {
    private final Map<String, Zone> zones = new LinkedHashMap<>();
    private long version;

    public long version() { return version; }
    public int size() { return zones.size(); }

    public void add(Zone zone) { zones.put(zone.id(), zone); version++; }
    public boolean remove(String id) { boolean had = zones.remove(id) != null; if (had) version++; return had; }
    public void clear() { if (!zones.isEmpty()) version++; zones.clear(); }
    public Optional<Zone> get(String id) { return Optional.ofNullable(zones.get(id)); }
    public List<Zone> all() { return List.copyOf(zones.values()); }

    public List<Zone> byKind(String dimension, ZoneKind kind) {
        List<Zone> out = new ArrayList<>();
        for (Zone z : zones.values()) if (z.kind() == kind && z.dimension().equals(dimension)) out.add(z);
        return out;
    }

    public Optional<Zone> nearest(String dimension, ZoneKind kind, double x, double z) {
        return byKind(dimension, kind).stream().min(Comparator.comparingDouble(zone -> zone.distance(x, z)));
    }

    public List<Zone> containing(String dimension, double x, double z) {
        List<Zone> out = new ArrayList<>();
        for (Zone zone : zones.values()) if (zone.contains(dimension, x, z)) out.add(zone);
        return out;
    }

    /** Claims a zone for an owner; the zone keeps its identity and only its owner changes. */
    public boolean claim(String id, String owner) {
        Zone zone = zones.get(id);
        if (zone == null) return false;
        zones.put(id, zone.withOwner(owner));
        version++;
        return true;
    }
}
