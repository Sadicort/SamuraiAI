package yadi.samuraiai.foundation.benchmark;

public record BenchmarkResult(String id, int iterations, double averageMillis, double p50Millis,
                              double p95Millis, double maximumMillis, boolean thresholdPassed,
                              double thresholdMillis) { }
