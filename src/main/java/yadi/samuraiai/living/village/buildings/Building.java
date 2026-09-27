package yadi.samuraiai.living.village.buildings;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.core.DayPhase;

/**
 * The Building Runtime: a functional building of a village — its kind, place, state and condition, owner, who is inside now,
 * the storage the Economy keeps for it ({@code inventoryRef}), when it is open, what happened to it recently, and the
 * Behavior Scheduler zone that makes it a destination for routines.
 */
public final class Building {
    public enum State { PLANNED, BUILT, DAMAGED, DESTROYED, ABANDONED }

    private final UUID id, village;
    private final BuildingKind kind;
    private String name;
    private UUID district;
    private final String dimension;
    private double x, y, z, radius;
    private State state;
    private double condition = 1.0D;
    private OwnerRef owner = OwnerRef.NONE;
    private int capacity;
    private final Set<UUID> visitors = new LinkedHashSet<>();
    private String inventoryRef = "", zoneId = "";
    private final Set<DayPhase> openPhases = EnumSet.allOf(DayPhase.class);
    private final Deque<String> events = new ArrayDeque<>();
    private long builtAt;

    public Building(UUID id, UUID village, BuildingKind kind, String name, String dimension, double x, double y, double z, double radius, State state, long builtAt) {
        this.id = id; this.village = village; this.kind = kind; this.name = name == null || name.isBlank() ? kind.name().toLowerCase(java.util.Locale.ROOT) : name;
        this.dimension = dimension; this.x = x; this.y = y; this.z = z; this.radius = Math.max(1.5D, radius); this.state = state; this.capacity = kind.defaultCapacity(); this.builtAt = builtAt;
        if (kind == BuildingKind.MARKET) { openPhases.clear(); openPhases.addAll(List.of(DayPhase.MORNING, DayPhase.NOON, DayPhase.AFTERNOON, DayPhase.SUNSET)); }
        if (kind == BuildingKind.SMITHY || kind == BuildingKind.CARPENTRY || kind == BuildingKind.WORKSHOP) { openPhases.clear(); openPhases.addAll(List.of(DayPhase.MORNING, DayPhase.NOON, DayPhase.AFTERNOON)); }
    }

    public UUID id() { return id; }
    public UUID village() { return village; }
    public BuildingKind kind() { return kind; }
    public String name() { return name; }
    public void name(String v) { name = v; }
    public UUID district() { return district; }
    public void district(UUID v) { district = v; }
    public String dimension() { return dimension; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public double radius() { return radius; }
    public void move(double nx, double ny, double nz, double r) { x = nx; y = ny; z = nz; radius = Math.max(1.5D, r); }
    public State state() { return state; }
    public void state(State v) { state = v; }
    public boolean usable() { return state == State.BUILT || state == State.DAMAGED; }
    public double condition() { return condition; }
    public void condition(double v) { condition = Math.max(0.0D, Math.min(1.0D, v)); }
    public OwnerRef owner() { return owner; }
    public void owner(OwnerRef v) { owner = v == null ? OwnerRef.NONE : v; }
    public int capacity() { return capacity; }
    public void capacity(int v) { capacity = Math.max(0, v); }
    public Set<UUID> visitors() { return visitors; }
    public String inventoryRef() { return inventoryRef; }
    public void inventoryRef(String v) { inventoryRef = v == null ? "" : v; }
    public String zoneId() { return zoneId; }
    public void zoneId(String v) { zoneId = v == null ? "" : v; }
    public Set<DayPhase> openPhases() { return openPhases; }
    public boolean openAt(DayPhase phase) { return usable() && openPhases.contains(phase); }
    public Deque<String> events() { return events; }
    public void note(String event) { events.addLast(event); while (events.size() > 16) events.removeFirst(); }
    public long builtAt() { return builtAt; }
    public void builtAt(long v) { builtAt = v; }
    public boolean contains(String dim, double px, double pz) { return dimension.equals(dim) && Math.hypot(px - x, pz - z) <= radius; }
    public double distance(double px, double pz) { return Math.hypot(px - x, pz - z); }
}
