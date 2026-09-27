package yadi.samuraiai.living.core;

/**
 * A per-call time budget. Work loops ask {@link #exhausted()} between units of work; the first unit always runs so that a
 * tiny budget still makes progress instead of starving forever.
 */
public final class TickBudget {
    private final long started = System.nanoTime();
    private final long limitNanos;
    private int units;

    private TickBudget(long limitNanos) { this.limitNanos = limitNanos; }

    public static TickBudget micros(long micros) { return new TickBudget(Math.max(1L, micros) * 1000L); }
    public static TickBudget unlimited() { return new TickBudget(Long.MAX_VALUE); }

    public boolean exhausted() { return units > 0 && System.nanoTime() - started > limitNanos; }
    public void spent() { units++; }
    public int units() { return units; }
    public long elapsedNanos() { return System.nanoTime() - started; }
}
