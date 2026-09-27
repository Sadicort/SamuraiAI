package yadi.samuraiai.living.world.settlements;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.core.Provenance;

/**
 * A settlement as the world sees it: that it exists, where, of what kind, in which region, since when and why. What happens
 * inside it (citizens, buildings, schedules, security) is the Village Engine's; what it produces and stores is the Economy
 * Engine's. Both refer to it by {@link #id()}.
 */
public final class Settlement {
    public enum Status { ACTIVE, ABANDONED, DESTROYED, RUINED }

    private final UUID id;
    private final String key;
    private String name;
    private SettlementType type;
    private final UUID regionId;
    private final String dimension;
    private double x, y, z, radius;
    private final long founded;
    private final Provenance origin;
    private Status status = Status.ACTIVE;
    private UUID roadNode;
    private final Set<String> tags = new LinkedHashSet<>();
    private boolean dirty = true;

    public Settlement(UUID id, String key, String name, SettlementType type, UUID regionId, String dimension, double x, double y, double z, double radius, long founded, Provenance origin) {
        this.id = id; this.key = key; this.name = name; this.type = type; this.regionId = regionId; this.dimension = dimension;
        this.x = x; this.y = y; this.z = z; this.radius = Math.max(8.0D, radius); this.founded = founded; this.origin = origin;
    }

    public UUID id() { return id; }
    public String key() { return key; }
    public String scope() { return "settlement:" + id; }
    public String name() { return name; }
    public void name(String v) { name = v; dirty = true; }
    public SettlementType type() { return type; }
    public void type(SettlementType v) { type = v; dirty = true; }
    public UUID regionId() { return regionId; }
    public String dimension() { return dimension; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public double radius() { return radius; }
    public void radius(double v) { radius = Math.max(8.0D, v); dirty = true; }
    public void move(double nx, double ny, double nz) { x = nx; y = ny; z = nz; dirty = true; }
    public long founded() { return founded; }
    public Provenance origin() { return origin; }
    public Status status() { return status; }
    public void status(Status v) { status = v; dirty = true; }
    public boolean active() { return status == Status.ACTIVE; }
    public UUID roadNode() { return roadNode; }
    public void roadNode(UUID v) { roadNode = v; dirty = true; }
    public Set<String> tags() { return tags; }
    public boolean contains(String dim, double px, double pz) { return dimension.equals(dim) && Math.hypot(px - x, pz - z) <= radius; }
    public double distance(double px, double pz) { return Math.hypot(px - x, pz - z); }

    public boolean dirty() { return dirty; }
    public void markDirty() { dirty = true; }
    public void clean() { dirty = false; }
}
