package yadi.samuraiai.foundation.scheduler;

import java.time.Duration;

/** Per-tick cooperative budget; callers defer remaining work instead of blocking the server. */
public final class TickBudget {
    public record Snapshot(int maximumTasks, int executedTasks, long maximumNanos,
                           long elapsedNanos, boolean exhausted) { }
    private final int maximumTasks;
    private final long maximumNanos;
    private final long started = System.nanoTime();
    private int executed;
    public TickBudget(int maximumTasks, Duration maximumTime) {
        if (maximumTasks < 1 || maximumTime == null || maximumTime.isNegative() || maximumTime.isZero())
            throw new IllegalArgumentException("Positive tick budget required");
        this.maximumTasks = maximumTasks; this.maximumNanos = maximumTime.toNanos();
    }
    public boolean tryAcquire() {
        if (executed >= maximumTasks || System.nanoTime() - started >= maximumNanos) return false;
        executed++; return true;
    }
    public Snapshot snapshot() {
        long elapsed = Math.max(0,System.nanoTime()-started);
        return new Snapshot(maximumTasks,executed,maximumNanos,elapsed,
                executed>=maximumTasks||elapsed>=maximumNanos);
    }
}
