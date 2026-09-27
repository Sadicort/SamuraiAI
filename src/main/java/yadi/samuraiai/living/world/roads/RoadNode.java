package yadi.samuraiai.living.world.roads;

import java.util.UUID;

/** A point of the world road graph: a settlement's gate, a crossroads, a bridge or a landmark. */
public record RoadNode(UUID id, Kind kind, String name, String dimension, double x, double z, UUID settlement) {
    public enum Kind { SETTLEMENT, INTERSECTION, BRIDGE, LANDMARK }

    public double distance(RoadNode other) { return Math.hypot(x - other.x, z - other.z); }
}
