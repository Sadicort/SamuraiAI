package yadi.samuraiai.ai.cognition.model;

import java.util.UUID;

/** Small numeric helpers shared by the cognitive engines. */
public final class Ids {
    private Ids() { }

    public static double clamp(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    public static double unit(double v) { return clamp(v, 0.0D, 1.0D); }

    /** A reproducible pseudo-random number in [0,1) from a seed and a salt: deterministic behaviour that tests can rely on. */
    public static double noise(UUID seed, long salt) {
        long h = seed.getMostSignificantBits() * 0x9E3779B97F4A7C15L ^ seed.getLeastSignificantBits() ^ (salt * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 33; h *= 0xFF51AFD7ED558CCDL; h ^= h >>> 33; h *= 0xC4CEB9FE1A85EC53L; h ^= h >>> 33;
        return ((h >>> 11) & 0xFFFFFFFFFFFFFL) / (double) (1L << 53);
    }
}
