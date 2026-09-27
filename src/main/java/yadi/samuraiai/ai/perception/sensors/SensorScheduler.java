package yadi.samuraiai.ai.perception.sensors;

import yadi.samuraiai.ai.perception.awareness.AwarenessLevel;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;

/**
 * Decides when each sensor of an NPC scans. Nothing scans every tick: each sensor has a base interval (near vision is
 * frequent, weather rare), stretched for NPCs far from any player and shortened for alert ones. A sensor that throws is
 * parked in FAILED for a cooldown and then retried, so one broken sensor never takes the NPC's perception down.
 */
public final class SensorScheduler {

    public int interval(SensorType type, PerceptionSettings s, int tierMultiplier, AwarenessLevel awareness) {
        int base = type.baseInterval(s);
        if (base <= 1) return 1;
        double value = (double) base * Math.max(1, tierMultiplier);
        if (awareness.atLeast(AwarenessLevel.ALERT)) value /= s.alertIntervalDivisor();
        return Math.max(1, (int) Math.round(value));
    }

    public boolean due(SensorRecord record, long tick, int interval) {
        if (record.state == SensorState.DISABLED) return false;
        if (record.state == SensorState.FAILED) {
            if (tick < record.cooldownUntilTick) return false;
            record.state = SensorState.READY;
        }
        return record.lastScanTick < 0 || tick - record.lastScanTick >= interval;
    }

    public void begin(SensorRecord record) { record.state = SensorState.SCANNING; }

    public void end(SensorRecord record, long tick, long nanos) {
        record.state = SensorState.COOLDOWN;
        record.lastScanTick = tick;
        record.scans++;
        record.lastNanos = nanos;
        record.totalNanos += nanos;
    }

    public void fail(SensorRecord record, long tick, RuntimeException error, PerceptionSettings s) {
        record.state = SensorState.FAILED;
        record.failures++;
        record.lastScanTick = tick;
        record.cooldownUntilTick = tick + s.failureCooldownTicks();
        record.lastError = error.getClass().getSimpleName() + ": " + error.getMessage();
    }
}
