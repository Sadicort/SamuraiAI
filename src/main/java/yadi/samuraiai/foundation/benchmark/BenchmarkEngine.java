package yadi.samuraiai.foundation.benchmark;

import java.util.*;

/** Small deterministic harness; Minecraft TPS benchmarks must supply their real operation. */
public final class BenchmarkEngine {
    public BenchmarkResult run(String id, int warmups, int iterations, double p95ThresholdMillis, Runnable operation) {
        if (id == null || id.isBlank() || warmups < 0 || iterations < 1 || p95ThresholdMillis <= 0)
            throw new IllegalArgumentException("Invalid benchmark configuration");
        Objects.requireNonNull(operation);
        for (int index = 0; index < warmups; index++) operation.run();
        long[] samples = new long[iterations]; long total = 0, maximum = 0;
        for (int index = 0; index < iterations; index++) {
            long started = System.nanoTime(); operation.run(); long elapsed = Math.max(0, System.nanoTime() - started);
            samples[index] = elapsed; total += elapsed; maximum = Math.max(maximum, elapsed);
        }
        Arrays.sort(samples);
        double p50 = percentile(samples, .50), p95 = percentile(samples, .95);
        return new BenchmarkResult(id, iterations, total / 1_000_000D / iterations, p50, p95,
                maximum / 1_000_000D, p95 <= p95ThresholdMillis, p95ThresholdMillis);
    }
    private static double percentile(long[] sorted, double percentile) {
        int index = Math.min(sorted.length - 1, Math.max(0, (int)Math.ceil(percentile * sorted.length) - 1));
        return sorted[index] / 1_000_000D;
    }
}
