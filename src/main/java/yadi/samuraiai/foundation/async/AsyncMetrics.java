package yadi.samuraiai.foundation.async;

public record AsyncMetrics(long submitted, long completed, long failed, long cancelled,
                           long timedOut, long rejected, int activeThreads, int queued,
                           double averageMillis, double maximumMillis) { }
