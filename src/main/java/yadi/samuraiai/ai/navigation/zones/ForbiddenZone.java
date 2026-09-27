package yadi.samuraiai.ai.navigation.zones;

import yadi.samuraiai.ai.navigation.graph.NavPos;

/** Axis-aligned box no path may enter. Rules that grant access to some walkers belong to the scheduler's zone system. */
public record ForbiddenZone(String id, String dimension, NavPos min, NavPos max) {
    public ForbiddenZone {
        if (id == null || dimension == null || min == null || max == null) throw new IllegalArgumentException("all fields required");
        NavPos low = new NavPos(Math.min(min.x(), max.x()), Math.min(min.y(), max.y()), Math.min(min.z(), max.z()));
        NavPos high = new NavPos(Math.max(min.x(), max.x()), Math.max(min.y(), max.y()), Math.max(min.z(), max.z()));
        min = low; max = high;
    }
    public boolean contains(String dim, NavPos p) {
        return dimension.equals(dim) && p.x() >= min.x() && p.x() <= max.x() && p.y() >= min.y() && p.y() <= max.y()
                && p.z() >= min.z() && p.z() <= max.z();
    }
}
