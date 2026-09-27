package yadi.samuraiai.ai.memory.forgetting;

/** Exponential forgetting: after one half-life a memory keeps half its strength. Gradual, never instantaneous. */
public final class ForgettingCurve {
    private ForgettingCurve() { }

    public static double retention(double elapsedTicks, double halfLifeTicks) {
        if (elapsedTicks <= 0) return 1.0D;
        if (halfLifeTicks <= 0 || !Double.isFinite(halfLifeTicks)) return 1.0D;
        return Math.pow(0.5D, elapsedTicks / halfLifeTicks);
    }
}
