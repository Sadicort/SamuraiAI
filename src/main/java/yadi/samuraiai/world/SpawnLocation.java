package yadi.samuraiai.world;

/**
 * Minecraft-agnostic physical location. Kept as a plain value so the domain
 * layers (spawn, controller, perception) never need to import
 * {@code net.minecraft.*} types and stay unit-testable.
 */
public record SpawnLocation(String dimensionKey, double x, double y, double z, float yaw) {

    public SpawnLocation {
        dimensionKey = dimensionKey == null ? "minecraft:overworld" : dimensionKey;
        if (dimensionKey.isBlank() || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw))
            throw new IllegalArgumentException("Finite position and dimension required");
    }

    /**
     * Squared distance to a point. Squared on purpose: callers compare against
     * a squared radius, and skipping the square root matters when this runs
     * once per NPC per chat message.
     */
    public double distanceSquaredTo(double otherX, double otherY, double otherZ) {

        double dx = x - otherX;
        double dy = y - otherY;
        double dz = z - otherZ;

        return dx * dx + dy * dy + dz * dz;
    }

    public double distanceTo(SpawnLocation other) {

        if (other == null || !dimensionKey.equals(other.dimensionKey)) {
            return Double.MAX_VALUE;
        }

        return Math.sqrt(distanceSquaredTo(other.x, other.y, other.z));
    }

    public boolean sameDimension(SpawnLocation other) {
        return other != null && dimensionKey.equals(other.dimensionKey);
    }

    public SpawnLocation withPosition(double newX, double newY, double newZ) {
        return new SpawnLocation(dimensionKey, newX, newY, newZ, yaw);
    }

    /** Short form for command output and logs. */
    public String toShortString() {
        return String.format("%.1f, %.1f, %.1f", x, y, z);
    }
}
