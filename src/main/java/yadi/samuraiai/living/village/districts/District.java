package yadi.samuraiai.living.village.districts;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The District Runtime: one district of a village, its buildings and how lively it is right now (0..1: people present over
 * capacity at full detail, planned occupancy from the schedules when the village is simulated abstractly).
 */
public final class District {
    private final UUID id;
    private final DistrictKind kind;
    private final String name;
    private double x, z, radius;
    private final Set<UUID> buildings = new LinkedHashSet<>();
    private double activity;
    private int present;

    public District(UUID id, DistrictKind kind, String name, double x, double z, double radius) {
        this.id = id; this.kind = kind; this.name = name; this.x = x; this.z = z; this.radius = Math.max(4.0D, radius);
    }

    public UUID id() { return id; }
    public DistrictKind kind() { return kind; }
    public String name() { return name; }
    public double x() { return x; }
    public double z() { return z; }
    public double radius() { return radius; }
    public void place(double nx, double nz, double r) { x = nx; z = nz; radius = Math.max(4.0D, r); }
    public Set<UUID> buildings() { return buildings; }
    public double activity() { return activity; }
    public int present() { return present; }
    public void activity(double value, int people) { activity = Math.max(0.0D, Math.min(1.0D, value)); present = Math.max(0, people); }
}
