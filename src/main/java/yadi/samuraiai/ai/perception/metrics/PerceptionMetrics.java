package yadi.samuraiai.ai.perception.metrics;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import yadi.samuraiai.ai.perception.sensors.SensorType;

/** Global perception counters: cheap atomics readable from any thread (commands, report writers). */
public final class PerceptionMetrics {
    private final AtomicLong passes = new AtomicLong(), nanos = new AtomicLong(), maxNanos = new AtomicLong(), rays = new AtomicLong(), raysRefused = new AtomicLong(),
            targetsEvaluated = new AtomicLong(), objectsSeen = new AtomicLong(), visionLost = new AtomicLong(), soundsHeard = new AtomicLong(),
            stimuliRaw = new AtomicLong(), stimuliAccepted = new AtomicLong(), stimuliRejected = new AtomicLong(), threats = new AtomicLong(),
            curiosity = new AtomicLong(), suspicionRaised = new AtomicLong(), awarenessChanges = new AtomicLong(), sensorFailures = new AtomicLong(),
            memoryForgotten = new AtomicLong(), npcsDeferred = new AtomicLong(), ticks = new AtomicLong(), tickNanos = new AtomicLong(), maxTickNanos = new AtomicLong();
    private final Map<SensorType, AtomicLong> sensorScans = new EnumMap<>(SensorType.class);
    private final Map<SensorType, AtomicLong> sensorNanos = new EnumMap<>(SensorType.class);
    private volatile int trackedNpcs;

    public PerceptionMetrics() {
        for (SensorType type : SensorType.values()) { sensorScans.put(type, new AtomicLong()); sensorNanos.put(type, new AtomicLong()); }
    }

    public void pass(long durationNanos) { passes.incrementAndGet(); nanos.addAndGet(durationNanos); maxNanos.accumulateAndGet(durationNanos, Math::max); }
    public void rays(int used, int refused) { rays.addAndGet(used); raysRefused.addAndGet(refused); }
    public void targets(int evaluated) { targetsEvaluated.addAndGet(evaluated); }
    public void sensorScan(SensorType type, long durationNanos) { sensorScans.get(type).incrementAndGet(); sensorNanos.get(type).addAndGet(durationNanos); }
    public void sensorFailed() { sensorFailures.incrementAndGet(); }
    public void objectSeen() { objectsSeen.incrementAndGet(); }
    public void visionLost() { visionLost.incrementAndGet(); }
    public void soundHeard() { soundsHeard.incrementAndGet(); }
    public void stimuli(int raw, int accepted, int rejected) { stimuliRaw.addAndGet(raw); stimuliAccepted.addAndGet(accepted); stimuliRejected.addAndGet(rejected); }
    public void threat() { threats.incrementAndGet(); }
    public void curiosity() { curiosity.incrementAndGet(); }
    public void suspicionRaised() { suspicionRaised.incrementAndGet(); }
    public void awarenessChanged() { awarenessChanges.incrementAndGet(); }
    public void forgotten(int count) { memoryForgotten.addAndGet(count); }
    public void deferred(int npcs) { npcsDeferred.addAndGet(npcs); }
    public void trackedNpcs(int value) { trackedNpcs = value; }
    public void tick(long durationNanos) { ticks.incrementAndGet(); tickNanos.addAndGet(durationNanos); maxTickNanos.accumulateAndGet(durationNanos, Math::max); }

    public Snapshot snapshot() {
        Map<SensorType, Long> scans = new EnumMap<>(SensorType.class);
        sensorScans.forEach((k, v) -> scans.put(k, v.get()));
        long p = passes.get(), t = ticks.get();
        return new Snapshot(p, p == 0 ? 0 : nanos.get() / 1000D / p, maxNanos.get() / 1000D, rays.get(), raysRefused.get(), targetsEvaluated.get(), objectsSeen.get(),
                visionLost.get(), soundsHeard.get(), stimuliRaw.get(), stimuliAccepted.get(), stimuliRejected.get(), threats.get(), curiosity.get(), suspicionRaised.get(),
                awarenessChanges.get(), sensorFailures.get(), memoryForgotten.get(), npcsDeferred.get(), trackedNpcs, t == 0 ? 0 : tickNanos.get() / 1000D / t, maxTickNanos.get() / 1000D, scans);
    }

    public void reset() {
        for (AtomicLong v : new AtomicLong[]{passes, nanos, maxNanos, rays, raysRefused, targetsEvaluated, objectsSeen, visionLost, soundsHeard, stimuliRaw, stimuliAccepted,
                stimuliRejected, threats, curiosity, suspicionRaised, awarenessChanges, sensorFailures, memoryForgotten, npcsDeferred, ticks, tickNanos, maxTickNanos}) v.set(0);
        sensorScans.values().forEach(v -> v.set(0)); sensorNanos.values().forEach(v -> v.set(0));
        trackedNpcs = 0;
    }

    public record Snapshot(long passes, double averagePassMicros, double maxPassMicros, long raycasts, long raysRefused, long targetsEvaluated, long objectsSeen,
                           long visionLosses, long soundsHeard, long stimuliRaw, long stimuliAccepted, long stimuliRejected, long threats, long curiosity,
                           long suspicionRaised, long awarenessChanges, long sensorFailures, long memoryForgotten, long npcsDeferred, int trackedNpcs,
                           double averageTickMicros, double maxTickMicros, Map<SensorType, Long> sensorScans) { }
}
