package yadi.samuraiai.ai.perception.memory;

import java.util.UUID;

/**
 * One remembered observation: what, where, when, how it was moving and how sure the NPC still is. {@code strength} fades
 * with age (see {@link PerceptionMemory#fade}) instead of vanishing at a cutoff.
 */
public final class MemoryEntry {
    public final String key;
    public final MemoryKind kind;
    public final UUID subject;
    public final long createdTick;
    public String label, detail;
    public double x, y, z, vx, vz, importance, strength;
    public long updatedTick;
    public int reinforcements;
    /** Radius of doubt around the position, for sounds heard from a distance. */
    public double uncertainty;

    MemoryEntry(String key, MemoryKind kind, UUID subject, long tick) {
        this.key = key; this.kind = kind; this.subject = subject; this.createdTick = tick; this.updatedTick = tick;
    }

    /** Direction of travel in Minecraft yaw, or NaN when it was not moving. */
    public double heading() {
        return Math.hypot(vx, vz) < 0.02D ? Double.NaN : Math.toDegrees(Math.atan2(-vx, vz));
    }
    public long ageTicks(long tick) { return Math.max(0L, tick - updatedTick); }
    public double distanceTo(double ox, double oy, double oz) {
        double dx = x - ox, dy = y - oy, dz = z - oz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
