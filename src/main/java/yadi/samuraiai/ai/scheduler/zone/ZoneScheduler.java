package yadi.samuraiai.ai.scheduler.zone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.scheduler.time.DayPeriod;

/**
 * The zone tier of the hierarchy. It knows who is in each zone, keeps zones from overcrowding, honours area ownership and
 * opening hours, and holds zone-level alerts. NPCs ask it where a routine can happen; it never tells an NPC to move.
 */
public final class ZoneScheduler {
    private final ZoneRegistry registry;
    private final Map<String, Set<UUID>> occupants = new HashMap<>();
    private final Map<String, Long> alertUntil = new HashMap<>();
    private final Map<UUID, String> assignment = new HashMap<>();

    public ZoneScheduler(ZoneRegistry registry) { this.registry = registry; }

    public ZoneRegistry registry() { return registry; }

    /** Records that an NPC is using a zone (called when its routine starts there). Replaces its previous assignment. */
    public void occupy(String zoneId, UUID npc) {
        release(npc);
        if (zoneId == null) return;
        occupants.computeIfAbsent(zoneId, k -> new HashSet<>()).add(npc);
        assignment.put(npc, zoneId);
    }

    public void release(UUID npc) {
        String previous = assignment.remove(npc);
        if (previous != null) {
            Set<UUID> set = occupants.get(previous);
            if (set != null) { set.remove(npc); if (set.isEmpty()) occupants.remove(previous); }
        }
    }

    public int occupancy(String zoneId) { return occupants.getOrDefault(zoneId, Set.of()).size(); }
    public Set<UUID> occupantsOf(String zoneId) { return Set.copyOf(occupants.getOrDefault(zoneId, Set.of())); }
    public Optional<String> assignmentOf(UUID npc) { return Optional.ofNullable(assignment.get(npc)); }

    public void alert(String zoneId, long until) { alertUntil.merge(zoneId, until, Math::max); }
    public boolean alerted(String zoneId, long now) { Long t = alertUntil.get(zoneId); return t != null && t > now; }

    /** Drops expired alerts and assignments of NPCs that no longer exist. */
    public void prune(long now, Set<UUID> living) {
        alertUntil.values().removeIf(t -> t <= now);
        for (UUID id : new ArrayList<>(assignment.keySet())) if (!living.contains(id)) release(id);
    }

    /**
     * The best zone of a kind for an NPC: open now, with room (or the NPC's own place), and either unowned or owned by the
     * NPC/its group. Owned zones are otherwise used only when nothing else is available. Nearer wins, then the emptier one.
     */
    public Optional<Zone> choose(String dimension, ZoneKind kind, double x, double z, UUID npc, String group, DayPeriod period) {
        List<Zone> open = new ArrayList<>();
        for (Zone zone : registry.byKind(dimension, kind)) if (zone.openAt(period)) open.add(zone);
        if (open.isEmpty()) return Optional.empty();
        String me = npc == null ? null : npc.toString();
        List<Zone> mine = open.stream().filter(zone -> zone.owner() != null && (zone.owner().equals(me) || zone.owner().equals(group))).toList();
        if (!mine.isEmpty()) return best(mine, x, z, npc, true);
        List<Zone> free = open.stream().filter(zone -> zone.owner() == null).toList();
        Optional<Zone> chosen = best(free, x, z, npc, false);
        return chosen.isPresent() ? chosen : best(open, x, z, npc, false);
    }

    private Optional<Zone> best(List<Zone> zones, double x, double z, UUID npc, boolean ignoreCapacity) {
        return zones.stream()
                .filter(zone -> ignoreCapacity || hasRoom(zone, npc))
                .min(Comparator.<Zone>comparingDouble(zone -> zone.distance(x, z) + occupancy(zone.id()) * 2.0D));
    }

    private boolean hasRoom(Zone zone, UUID npc) {
        return occupancy(zone.id()) < zone.capacity() || (npc != null && zone.id().equals(assignment.get(npc)));
    }

    public record Status(String id, ZoneKind kind, int occupancy, int capacity, String owner, boolean alerted) { }

    public List<Status> status(long now) {
        List<Status> out = new ArrayList<>();
        for (Zone zone : registry.all()) out.add(new Status(zone.id(), zone.kind(), occupancy(zone.id()), zone.capacity(), zone.owner(), alerted(zone.id(), now)));
        return out;
    }
}
