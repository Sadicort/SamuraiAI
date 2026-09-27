package yadi.samuraiai.living.world.roads;

import java.util.List;
import java.util.UUID;

/**
 * A stretch of road between two nodes: its kind, length in blocks (as travelled, longer than the straight line), condition,
 * danger (from the regions it crosses plus what events add), how many bridges it crosses, whether it is blocked and why, and
 * how much it has been used. {@code regions} are the regions it crosses, so a region's danger changing re-rates only these.
 */
public final class RoadEdge {
    public enum Kind { ROAD, TRAIL, BRIDGE, SHORTCUT }

    private final UUID id, a, b;
    private final Kind kind;
    private final double length;
    private final int bridges;
    private final List<UUID> regions;
    private double condition = 1.0D, danger, eventDanger;
    private boolean blocked;
    private String blockedReason = "";
    private long traffic, lastTravelled;

    public RoadEdge(UUID id, UUID a, UUID b, Kind kind, double length, int bridges, List<UUID> regions) {
        this.id = id; this.a = a; this.b = b; this.kind = kind; this.length = Math.max(1.0D, length); this.bridges = Math.max(0, bridges); this.regions = List.copyOf(regions);
    }

    public UUID id() { return id; }
    public UUID a() { return a; }
    public UUID b() { return b; }
    public UUID other(UUID node) { return node.equals(a) ? b : a; }
    public boolean touches(UUID node) { return a.equals(node) || b.equals(node); }
    public Kind kind() { return kind; }
    public double length() { return length; }
    public int bridges() { return bridges; }
    public List<UUID> regions() { return regions; }
    public double condition() { return condition; }
    public void condition(double v) { condition = Math.max(0.0D, Math.min(1.0D, v)); }
    /** Total danger 0..1 of travelling this stretch. */
    public double danger() { return Math.max(0.0D, Math.min(1.0D, danger + eventDanger)); }
    public double landDanger() { return danger; }
    public double eventDanger() { return eventDanger; }
    public void landDanger(double v) { danger = Math.max(0.0D, Math.min(1.0D, v)); }
    public void eventDanger(double v) { eventDanger = Math.max(0.0D, Math.min(1.0D, v)); }
    public boolean blocked() { return blocked; }
    public String blockedReason() { return blockedReason; }
    void block(String reason) { blocked = true; blockedReason = reason == null ? "" : reason; }
    void reopen() { blocked = false; blockedReason = ""; }
    public long traffic() { return traffic; }
    public long lastTravelled() { return lastTravelled; }
    public void travelled(long minute) { traffic++; lastTravelled = minute; }
    /** Speed factor on this stretch (roads are fastest, trails slow, poor condition slower). */
    public double speedFactor() { return (kind == Kind.TRAIL ? 0.7D : kind == Kind.SHORTCUT ? 0.85D : 1.0D) * (0.5D + 0.5D * condition); }

    public void restore(double savedCondition, double savedDanger, double savedEventDanger, boolean savedBlocked, String reason, long savedTraffic, long savedLast) {
        condition = savedCondition; danger = savedDanger; eventDanger = savedEventDanger; blocked = savedBlocked; blockedReason = reason == null ? "" : reason; traffic = savedTraffic; lastTravelled = savedLast;
    }
}
