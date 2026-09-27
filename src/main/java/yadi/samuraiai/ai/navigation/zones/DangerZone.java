package yadi.samuraiai.ai.navigation.zones;

import yadi.samuraiai.ai.navigation.graph.NavPos;

/** A temporary, spherical hazard: an explosion, a burning area, a hostile mob reported by perception. */
public record DangerZone(String dimension, NavPos center, double radius, double score, long expiresTick,
                         HazardType type, String source) {
    public DangerZone {
        if (dimension == null || center == null) throw new IllegalArgumentException("dimension and center required");
        radius = Double.isFinite(radius) ? Math.max(0.5D, radius) : 1.0D;
        score = Double.isFinite(score) ? Math.max(0.0D, score) : 0.0D;
        type = type == null ? HazardType.CUSTOM : type;
        source = source == null ? "unknown" : source;
    }
    /** Score at a position: full at the center, linear falloff to zero at the radius. */
    public double scoreAt(NavPos pos) {
        double distance = center.distance(pos);
        return distance >= radius ? 0.0D : score * (1.0D - distance / radius);
    }
    public boolean expired(long tick) { return tick >= expiresTick; }
    public DangerZone withSource(String value) { return new DangerZone(dimension, center, radius, score, expiresTick, type, value); }
}
