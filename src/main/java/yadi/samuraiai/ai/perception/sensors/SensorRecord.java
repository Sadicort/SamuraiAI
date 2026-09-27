package yadi.samuraiai.ai.perception.sensors;

/** Bookkeeping for one sensor of one NPC: its state, when it last scanned, when it may scan next, and how it has fared. */
public final class SensorRecord {
    public final SensorType type;
    public SensorState state = SensorState.CREATED;
    public long lastScanTick = -1, nextDueTick = 0, cooldownUntilTick = 0, scans, failures;
    public long totalNanos, lastNanos;
    public String lastError = "";

    public SensorRecord(SensorType type) { this.type = type; }

    public boolean enabled() { return state != SensorState.DISABLED; }
}
