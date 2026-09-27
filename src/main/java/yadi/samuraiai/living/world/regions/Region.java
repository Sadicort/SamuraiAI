package yadi.samuraiai.living.world.regions;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.world.streaming.SimulationLevel;
import yadi.samuraiai.living.world.wildlife.WildlifePopulation;

/**
 * The Region Runtime: one cell of the world map with its own identity — name, type, biome, culture, danger, natural resources,
 * wildlife, settlements, relations with neighbouring regions, the aggregate population the villages report, and history
 * counters (fires, wars, festivals, disasters, heroes). Its dated history lives in the world timeline under the scope
 * {@link #scope()}; the counters are the region's quick memory of it.
 */
public final class Region {
    public enum Relation { ADJACENT, ALLIED, TRADE, RIVAL, HOSTILE }
    public enum Counter { CONSTRUCTIONS, FIRES, WARS, BATTLES, FESTIVALS, DISASTERS, HEROES, VISITS }

    private final UUID id;
    private final String key, dimension;
    private final int cellX, cellZ, cellSize;
    private String name, biome, cultureId;
    private RegionType type;
    private double baseDanger, eventDanger, altitude = 64;
    private final Map<String, ResourceDeposit> deposits = new LinkedHashMap<>();
    private final Map<String, WildlifePopulation> wildlife = new LinkedHashMap<>();
    private final Map<UUID, Relation> relations = new LinkedHashMap<>();
    private final Set<UUID> settlements = new LinkedHashSet<>();
    private final Map<Counter, Integer> counters = new EnumMap<>(Counter.class);
    private int population;
    private SimulationLevel level = SimulationLevel.ABSTRACT;
    private long created, lastSimulated, lastPlayerSeen = Long.MIN_VALUE / 2;
    private boolean dirty = true;

    public Region(UUID id, String key, String dimension, int cellX, int cellZ, int cellSize, String name, RegionType type, String biome, String cultureId, long created) {
        this.id = id; this.key = key; this.dimension = dimension; this.cellX = cellX; this.cellZ = cellZ; this.cellSize = cellSize;
        this.name = name; this.type = type; this.biome = biome == null ? "" : biome; this.cultureId = cultureId == null ? "village" : cultureId;
        this.created = created; this.lastSimulated = created;
    }

    public UUID id() { return id; }
    public String key() { return key; }
    /** The timeline and weather scope of this region. */
    public String scope() { return "region:" + id; }
    public String dimension() { return dimension; }
    public int cellX() { return cellX; }
    public int cellZ() { return cellZ; }
    public int cellSize() { return cellSize; }
    public double centerX() { return (cellX + 0.5D) * cellSize; }
    public double centerZ() { return (cellZ + 0.5D) * cellSize; }
    public boolean contains(String dim, double x, double z) { return dimension.equals(dim) && Math.floorDiv((int) Math.floor(x), cellSize) == cellX && Math.floorDiv((int) Math.floor(z), cellSize) == cellZ; }
    /** Distance from a point to the nearest edge of the cell (0 inside). */
    public double distanceTo(double x, double z) {
        double minX = (double) cellX * cellSize, minZ = (double) cellZ * cellSize, maxX = minX + cellSize, maxZ = minZ + cellSize;
        double dx = Math.max(0.0D, Math.max(minX - x, x - maxX)), dz = Math.max(0.0D, Math.max(minZ - z, z - maxZ));
        return Math.hypot(dx, dz);
    }

    public String name() { return name; }
    public void name(String v) { name = v; dirty = true; }
    public RegionType type() { return type; }
    public void type(RegionType v) { type = v; dirty = true; }
    public String biome() { return biome; }
    public void biome(String v) { biome = v == null ? "" : v; dirty = true; }
    public String cultureId() { return cultureId; }
    public void cultureId(String v) { cultureId = v; dirty = true; }
    public double altitude() { return altitude; }
    public void altitude(double v) { altitude = v; dirty = true; }

    /** Danger 0..1: the land's own (wolves, terrain) plus what current events add (bandits, war). */
    public double danger() { return Math.max(0.0D, Math.min(1.0D, baseDanger + eventDanger)); }
    public double baseDanger() { return baseDanger; }
    public double eventDanger() { return eventDanger; }
    public void baseDanger(double v) { baseDanger = Math.max(0.0D, Math.min(1.0D, v)); dirty = true; }
    public void eventDanger(double v) { eventDanger = Math.max(0.0D, Math.min(1.0D, v)); dirty = true; }

    public Map<String, ResourceDeposit> deposits() { return deposits; }
    public Map<String, WildlifePopulation> wildlife() { return wildlife; }
    public Map<UUID, Relation> relations() { return relations; }
    public Set<UUID> settlements() { return settlements; }
    public int counter(Counter c) { return counters.getOrDefault(c, 0); }
    public void count(Counter c) { counters.merge(c, 1, Integer::sum); dirty = true; }
    public Map<Counter, Integer> counters() { return counters; }
    public int population() { return population; }
    public void population(int v) { if (v != population) { population = Math.max(0, v); dirty = true; } }

    public SimulationLevel level() { return level; }
    public void level(SimulationLevel v) { level = v; }
    public long created() { return created; }
    public long lastSimulated() { return lastSimulated; }
    public void lastSimulated(long v) { lastSimulated = v; dirty = true; }
    public long lastPlayerSeen() { return lastPlayerSeen; }
    public void lastPlayerSeen(long v) { lastPlayerSeen = v; }

    public boolean dirty() { return dirty; }
    public void markDirty() { dirty = true; }
    public void clean() { dirty = false; }
}
