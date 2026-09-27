package yadi.samuraiai.ai.perception.vision;

import java.util.UUID;
import yadi.samuraiai.ai.perception.engine.EntityClass;

/**
 * Immutable view of a visual target for snapshots and events: identity, class, position, velocity, distance,
 * visibility, confidence and how long it has been observed. This is the "PlayerTarget / NPCTarget / HostileTarget /
 * PassiveTarget / ObjectTarget / ProjectileTarget / UnknownTarget" classification, carried by {@code kind}.
 */
public record VisualTarget(UUID id, EntityClass kind, String name, double x, double y, double z, double vx, double vy, double vz,
                           double distance, VisibilityState state, double confidence, long firstSeenTick, long lastSeenTick, long observedTicks) {
    public double speed() { return Math.sqrt(vx * vx + vz * vz); }
    public boolean seen() { return state.seen(); }
}
