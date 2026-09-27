package yadi.samuraiai.ai.perception.vision;

import yadi.samuraiai.ai.perception.engine.PerceptionWorld;

/**
 * Voxel raycast (Amanatides-Woo) that accumulates opacity instead of stopping at the first block: glass, leaves and
 * water thin the ray, solid blocks and closed doors kill it. Returns the transmittance in 0..1 (1 = clear line).
 */
public final class Raycaster {
    private static final double CUTOFF = 0.01D;

    private Raycaster() { }

    public static double transmittance(PerceptionWorld world, double x0, double y0, double z0, double x1, double y1, double z1) {
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6D) return 1.0D;
        int x = (int) Math.floor(x0), y = (int) Math.floor(y0), z = (int) Math.floor(z0);
        int endX = (int) Math.floor(x1), endY = (int) Math.floor(y1), endZ = (int) Math.floor(z1);
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1, stepZ = dz > 0 ? 1 : -1;
        double tDeltaX = dx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / dx), tDeltaY = dy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / dy),
                tDeltaZ = dz == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / dz);
        double tMaxX = dx == 0 ? Double.POSITIVE_INFINITY : (dx > 0 ? (x + 1 - x0) : (x0 - x)) * tDeltaX;
        double tMaxY = dy == 0 ? Double.POSITIVE_INFINITY : (dy > 0 ? (y + 1 - y0) : (y0 - y)) * tDeltaY;
        double tMaxZ = dz == 0 ? Double.POSITIVE_INFINITY : (dz > 0 ? (z + 1 - z0) : (z0 - z)) * tDeltaZ;
        double transmittance = 1.0D;
        int guard = (int) (Math.abs(dx) + Math.abs(dy) + Math.abs(dz)) + 6;
        while (guard-- > 0 && !(x == endX && y == endY && z == endZ)) {
            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) { x += stepX; tMaxX += tDeltaX; }
            else if (tMaxY <= tMaxZ) { y += stepY; tMaxY += tDeltaY; }
            else { z += stepZ; tMaxZ += tDeltaZ; }
            if (x == endX && y == endY && z == endZ) break;
            transmittance *= 1.0D - world.opacity(x, y, z);
            if (transmittance < CUTOFF) return 0.0D;
        }
        return transmittance;
    }
}
