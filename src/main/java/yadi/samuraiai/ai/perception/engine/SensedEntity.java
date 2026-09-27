package yadi.samuraiai.ai.perception.engine;

import java.util.UUID;

/**
 * An entity as the world reports it to perception: identity, class, position, velocity and posture. Ground truth from
 * the world; what the NPC actually notices of it is decided later by the sensors.
 */
public record SensedEntity(UUID id, EntityClass kind, String name, double x, double y, double z, double vx, double vy, double vz,
                           double width, double height, boolean sneaking, boolean sprinting, boolean onGround, boolean invisible) {

    public double speed() { return Math.sqrt(vx * vx + vz * vz); }
    public double centerY() { return y + height * 0.5D; }
    public double distanceTo(double ox, double oy, double oz) {
        double dx = x - ox, dy = y - oy, dz = z - oz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    public static SensedEntity at(UUID id, EntityClass kind, String name, double x, double y, double z) {
        return new SensedEntity(id, kind, name, x, y, z, 0, 0, 0, 0.6D, 1.8D, false, false, true, false);
    }
    public SensedEntity moving(double vx, double vz, boolean sprinting) {
        return new SensedEntity(id, kind, name, x, y, z, vx, 0, vz, width, height, sneaking, sprinting, onGround, invisible);
    }
    public SensedEntity sneaking(boolean value) {
        return new SensedEntity(id, kind, name, x, y, z, vx, vy, vz, width, height, value, sprinting, onGround, invisible);
    }
}
