package yadi.samuraiai.ai.scheduler.zone;

/** Somewhere an NPC is asked to be: a point with a tolerance, optionally belonging to a named zone. */
public record Place(String dimension, double x, double y, double z, double radius, String zoneId) {
    public Place {
        radius = Math.max(0.5D, radius);
    }

    public double horizontalDistance(double px, double pz) { return Math.hypot(px - x, pz - z); }
    public boolean near(String dim, double px, double pz, double extra) { return dimension.equals(dim) && horizontalDistance(px, pz) <= radius + extra; }
    public Place withPoint(double nx, double ny, double nz) { return new Place(dimension, nx, ny, nz, radius, zoneId); }
    public Place withRadius(double r) { return new Place(dimension, x, y, z, r, zoneId); }
    public String describe() { return String.format("%s(%.0f,%.0f,%.0f r%.1f)", zoneId == null ? "point" : zoneId, x, y, z, radius); }
}
