package yadi.samuraiai.ai.emotion.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EmotionKind;

/**
 * A lasting emotional wound: what caused it, how deep it is, the memories behind it, the places and people that bring it back
 * and how far along its recovery is. Each trauma recovers on its own schedule. The flashback and avoidance fields are the
 * structure prepared for a future post-traumatic-stress behavior; nothing acts on them yet.
 */
public final class TraumaRecord {
    public enum Phase { ACTIVE, RECOVERING, RECOVERED }

    private final UUID id, npcId;
    private final String originKind;
    private final EmotionKind emotion;
    private double intensity, progress;
    private final long at;
    private long lastProgress;
    private Phase phase = Phase.ACTIVE;
    private final Set<UUID> memories = new LinkedHashSet<>();
    private final Set<String> triggers = new LinkedHashSet<>();
    private int flashbacks, version = 1;
    private double avoidance;

    public TraumaRecord(UUID id, UUID npcId, String originKind, EmotionKind emotion, double intensity, long at) {
        this.id = id; this.npcId = npcId; this.originKind = originKind == null ? "" : originKind; this.emotion = emotion; this.intensity = intensity; this.at = at; this.lastProgress = at;
    }

    public UUID id() { return id; }
    public UUID npcId() { return npcId; }
    public String originKind() { return originKind; }
    public EmotionKind emotion() { return emotion; }
    public double intensity() { return intensity; }
    public void intensity(double v) { intensity = Math.max(0, Math.min(1, v)); }
    public double progress() { return progress; }
    public void progress(double v) { progress = Math.max(0, Math.min(1, v)); }
    public long at() { return at; }
    public long lastProgress() { return lastProgress; }
    public void lastProgress(long v) { lastProgress = v; }
    public Phase phase() { return phase; }
    public void phase(Phase v) { phase = v; }
    public Set<UUID> memories() { return memories; }
    public Set<String> triggers() { return triggers; }
    public int flashbacks() { return flashbacks; }
    public void flashbacks(int v) { flashbacks = Math.max(0, v); }
    public double avoidance() { return avoidance; }
    public void avoidance(double v) { avoidance = Math.max(0, Math.min(1, v)); }
    public int version() { return version; }
    public void version(int v) { version = Math.max(1, v); }
    public void bump() { version++; }
    public boolean active() { return phase != Phase.RECOVERED; }
    /** How much of the wound is left, 0-1. */
    public double remaining() { return intensity * (1.0D - progress); }
}
