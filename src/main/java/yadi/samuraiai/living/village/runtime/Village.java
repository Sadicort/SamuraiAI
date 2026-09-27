package yadi.samuraiai.living.village.runtime;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.village.buildings.Building;
import yadi.samuraiai.living.village.districts.District;
import yadi.samuraiai.living.village.districts.DistrictKind;
import yadi.samuraiai.living.village.homes.HomeRecord;
import yadi.samuraiai.living.village.life.VillageEventRecord;
import yadi.samuraiai.living.village.security.SecurityRuntime;
import yadi.samuraiai.living.village.visitors.VisitorRecord;

/**
 * The Village Runtime: one community. Its id is the id of the world settlement it lives in (one identity, not two). It holds
 * what the Village Engine owns — citizens, buildings, districts, homes, the layout, schedules' inputs, market and temple
 * life, the guard roster, security, visitors, village events, renown and the community's quick memory — and references to
 * what others own: the region (World), the knowledge community (collective knowledge, culture, reputation), the economy's
 * storage for its buildings.
 */
public final class Village {
    public enum Counter { FESTIVALS, FIRES, WARS, ATTACKS_REPELLED, AID, BETRAYALS, CONSTRUCTIONS, ARRIVALS, DEPARTURES, VISITORS }

    private final UUID id, region;
    private String name, culture;
    private final String communityKey, dimension;
    private double x, y, z, radius;
    private final long founded;
    private UUID mainTemple, market;
    private final Map<DistrictKind, District> districts = new EnumMap<>(DistrictKind.class);
    private final Map<UUID, Building> buildings = new LinkedHashMap<>();
    private final Set<UUID> citizens = new LinkedHashSet<>();
    private final Map<UUID, HomeRecord> homes = new LinkedHashMap<>();
    private final Map<UUID, Boolean> nightWatch = new LinkedHashMap<>();
    private final List<VisitorRecord> visitors = new ArrayList<>();
    private final List<VillageEventRecord> events = new ArrayList<>();
    private final Map<UUID, double[]> stalls = new LinkedHashMap<>();
    private final Map<Counter, Integer> counters = new EnumMap<>(Counter.class);
    private final Deque<String> renownCauses = new ArrayDeque<>();
    private final SecurityRuntime security = new SecurityRuntime();
    private final VillageLayout layout = new VillageLayout();
    private boolean marketOpen, ritualActive;
    private int marketFootfall;
    private double renown, unrest, prosperity = 0.5D, socialActivity;
    private long lastSimulated;
    private boolean dirty = true;

    public Village(UUID id, UUID region, String name, String culture, String communityKey, String dimension, double x, double y, double z, double radius, long founded) {
        this.id = id; this.region = region; this.name = name; this.culture = culture == null ? "village" : culture; this.communityKey = communityKey;
        this.dimension = dimension; this.x = x; this.y = y; this.z = z; this.radius = Math.max(16, radius); this.founded = founded; this.lastSimulated = founded;
    }

    public UUID id() { return id; }
    public String scope() { return "village:" + id; }
    public UUID region() { return region; }
    public String name() { return name; }
    public void name(String v) { name = v; dirty = true; }
    public String culture() { return culture; }
    public void culture(String v) { culture = v; dirty = true; }
    public String communityKey() { return communityKey; }
    public String dimension() { return dimension; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public double radius() { return radius; }
    public void radius(double v) { radius = Math.max(16, v); dirty = true; }
    public long founded() { return founded; }
    public UUID mainTemple() { return mainTemple; }
    public void mainTemple(UUID v) { mainTemple = v; dirty = true; }
    public UUID market() { return market; }
    public void market(UUID v) { market = v; dirty = true; }
    public Map<DistrictKind, District> districts() { return districts; }
    public Map<UUID, Building> buildings() { return buildings; }
    public Set<UUID> citizens() { return citizens; }
    public Map<UUID, HomeRecord> homes() { return homes; }
    /** Guards on the night watch (true) or the day watch (false). */
    public Map<UUID, Boolean> nightWatch() { return nightWatch; }
    public List<VisitorRecord> visitors() { return visitors; }
    public List<VillageEventRecord> events() { return events; }
    /** Market stall positions by merchant. */
    public Map<UUID, double[]> stalls() { return stalls; }
    public Map<Counter, Integer> counters() { return counters; }
    public int counter(Counter c) { return counters.getOrDefault(c, 0); }
    public void count(Counter c) { counters.merge(c, 1, Integer::sum); dirty = true; }
    public SecurityRuntime security() { return security; }
    public VillageLayout layout() { return layout; }
    public boolean marketOpen() { return marketOpen; }
    public void marketOpen(boolean v) { marketOpen = v; }
    public int marketFootfall() { return marketFootfall; }
    public void marketFootfall(int v) { marketFootfall = Math.max(0, v); }
    public boolean ritualActive() { return ritualActive; }
    public void ritualActive(boolean v) { ritualActive = v; }
    public double renown() { return renown; }
    public Deque<String> renownCauses() { return renownCauses; }
    /** The village's reputation among other villages moves with a cause (a festival well held, an attack repelled, a betrayal). */
    public void renown(double delta, String cause) {
        renown = Math.max(-100.0D, Math.min(100.0D, renown + delta));
        renownCauses.addLast(String.format("%+.1f %s", delta, cause));
        while (renownCauses.size() > 20) renownCauses.removeFirst();
        dirty = true;
    }
    public void restoreRenown(double v) { renown = v; }
    public double unrest() { return unrest; }
    public void unrest(double v) { unrest = Math.max(0.0D, Math.min(1.0D, v)); }
    public double prosperity() { return prosperity; }
    public void prosperity(double v) { prosperity = Math.max(0.0D, Math.min(1.0D, v)); }
    public double socialActivity() { return socialActivity; }
    public void socialActivity(double v) { socialActivity = Math.max(0.0D, v); }
    public long lastSimulated() { return lastSimulated; }
    public void lastSimulated(long v) { lastSimulated = v; }
    public boolean contains(String dim, double px, double pz) { return dimension.equals(dim) && Math.hypot(px - x, pz - z) <= radius; }
    public double distance(double px, double pz) { return Math.hypot(px - x, pz - z); }

    public List<Building> buildingsOf(yadi.samuraiai.living.village.buildings.BuildingKind kind) {
        List<Building> out = new ArrayList<>();
        for (Building b : buildings.values()) if (b.kind() == kind) out.add(b);
        return out;
    }

    public List<VillageEventRecord> activeEvents(long minute) {
        List<VillageEventRecord> out = new ArrayList<>();
        for (VillageEventRecord e : events) if (e.activeAt(minute)) out.add(e);
        return out;
    }

    public boolean dirty() { return dirty; }
    public void markDirty() { dirty = true; }
    public void clean() { dirty = false; }
}
