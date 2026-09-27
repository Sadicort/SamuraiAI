package yadi.samuraiai.ai.scheduler.zone;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import yadi.samuraiai.ai.scheduler.time.DayPeriod;

/**
 * A named area with a purpose. It can be limited to certain periods of the day, have a capacity, and be owned (claimed) by
 * one NPC or group, in which case others only use it when nobody else is available. Patrol routes may carry explicit
 * waypoints; otherwise their points are spread around the zone's centre.
 */
public record Zone(String id, String dimension, ZoneKind kind, double x, double y, double z, double radius, int capacity,
                   Set<DayPeriod> openPeriods, String owner, List<double[]> waypoints) {
    public Zone {
        radius = Math.max(0.5D, radius);
        capacity = Math.max(1, capacity);
        openPeriods = openPeriods == null || openPeriods.isEmpty() ? EnumSet.allOf(DayPeriod.class) : EnumSet.copyOf(openPeriods);
        waypoints = waypoints == null ? List.of() : List.copyOf(waypoints);
    }

    public boolean openAt(DayPeriod period) { return openPeriods.contains(period); }
    public boolean contains(String dim, double px, double pz) { return dimension.equals(dim) && Math.hypot(px - x, pz - z) <= radius; }
    public double distance(double px, double pz) { return Math.hypot(px - x, pz - z); }
    public Zone withOwner(String newOwner) { return new Zone(id, dimension, kind, x, y, z, radius, capacity, openPeriods, newOwner, waypoints); }

    /** The whole zone as a place: arriving anywhere inside counts. */
    public Place place() { return new Place(dimension, x, y, z, Math.max(1.0D, radius * 0.6D), id); }

    /** The {@code index}th point of a patrol loop of {@code count} points. */
    public Place patrolPoint(int index, int count) {
        if (!waypoints.isEmpty()) {
            double[] w = waypoints.get(Math.floorMod(index, waypoints.size()));
            return new Place(dimension, w[0], w.length > 1 ? w[1] : y, w.length > 2 ? w[2] : z, 1.5D, id);
        }
        int n = Math.max(2, count);
        double angle = 2.0D * Math.PI * Math.floorMod(index, n) / n;
        return new Place(dimension, x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius, 1.5D, id);
    }
}
