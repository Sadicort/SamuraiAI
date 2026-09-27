package yadi.samuraiai.living.core;

import java.util.UUID;

/**
 * Deterministic chance. Every roll is a pure function of a world seed, a key and a step, so the same world simulated twice
 * (or caught up after a restart) makes the same decisions, and tests can reproduce any outcome.
 */
public final class Dice {
    private final long seed;

    public Dice(long seed) { this.seed = seed; }

    public long seed() { return seed; }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private long hash(String key, long step) {
        long h = seed * 0x9E3779B97F4A7C15L;
        for (int i = 0; i < key.length(); i++) h = mix(h ^ key.charAt(i)) + i;
        return mix(h ^ mix(step + 0x632BE59BD9B4E019L));
    }

    /** Uniform in [0,1). */
    public double unit(String key, long step) { return (hash(key, step) >>> 11) * 0x1.0p-53; }

    public boolean chance(String key, long step, double probability) { return probability > 0 && unit(key, step) < probability; }

    /** Uniform integer in [0, bound). */
    public int below(String key, long step, int bound) { return bound <= 1 ? 0 : (int) Math.floor(unit(key, step) * bound); }

    /** Uniform in [lo, hi). */
    public double between(String key, long step, double lo, double hi) { return lo + (hi - lo) * unit(key, step); }

    /** Picks an index by weight; -1 when every weight is zero or negative. */
    public int weighted(String key, long step, double[] weights) {
        double total = 0;
        for (double w : weights) if (w > 0) total += w;
        if (total <= 0) return -1;
        double r = unit(key, step) * total;
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] <= 0) continue;
            r -= weights[i];
            if (r < 0) return i;
        }
        for (int i = weights.length - 1; i >= 0; i--) if (weights[i] > 0) return i;
        return -1;
    }

    public static String key(UUID id) { return id == null ? "" : id.toString(); }
}
