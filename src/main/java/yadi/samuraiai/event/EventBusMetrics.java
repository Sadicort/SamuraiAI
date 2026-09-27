package yadi.samuraiai.event;

public record EventBusMetrics(long published, long deliveries, long listenerFailures,
                              double averageDispatchMicros, double maximumDispatchMicros,
                              int listenerCount) { }
