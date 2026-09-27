package yadi.samuraiai.ai.emotion.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.CauseLog;
import yadi.samuraiai.ai.cognition.model.EmotionKind;

/**
 * One felt emotion: its kind and intensity, why it exists, how it fades, and the dimensions that describe it (stability,
 * control, valence, arousal, persistence). Several can coexist; records with the same kind and origin key are fused.
 */
public final class EmotionRecord {
    private final UUID id, npcId;
    private final EmotionKind kind;
    private final EmotionOrigin origin;
    private final String key;
    private double intensity, initial, peak;
    private EmotionPhase phase = EmotionPhase.ACTIVE;
    private DecayCurve curve = DecayCurve.EXPONENTIAL;
    private double customHalfLife;
    private long created, updated;
    private double stability = 0.5D, control = 0.5D, valence, arousal, persistence = 0.4D;
    private final CauseLog influences;
    private final Set<UUID> relatedEvents = new LinkedHashSet<>();
    private int version = 1, reinforcements;
    private UUID traumaId;

    public EmotionRecord(UUID id, UUID npcId, EmotionKind kind, EmotionOrigin origin, String key, double intensity, long now, int causeCapacity) {
        this.id = id; this.npcId = npcId; this.kind = kind; this.origin = origin; this.key = key;
        this.intensity = intensity; this.initial = intensity; this.peak = intensity; this.created = now; this.updated = now;
        this.valence = kind.valence(); this.arousal = kind.arousal();
        this.influences = new CauseLog(causeCapacity);
    }

    public UUID id() { return id; }
    public UUID npcId() { return npcId; }
    public EmotionKind kind() { return kind; }
    public EmotionOrigin origin() { return origin; }
    public String key() { return key; }
    public double intensity() { return intensity; }
    public void intensity(double v) { intensity = Math.max(0.0D, Math.min(100.0D, v)); if (intensity > peak) peak = intensity; }
    public double initial() { return initial; }
    public void initial(double v) { initial = v; }
    public double peak() { return peak; }
    public void peak(double v) { peak = v; }
    public EmotionPhase phase() { return phase; }
    public void phase(EmotionPhase v) { phase = v; }
    public DecayCurve curve() { return curve; }
    public void curve(DecayCurve v) { curve = v; }
    public double customHalfLife() { return customHalfLife; }
    public void customHalfLife(double v) { customHalfLife = v; }
    public long created() { return created; }
    public long updated() { return updated; }
    public void updated(long v) { updated = v; }
    public long duration(long now) { return Math.max(0, now - created); }
    public double stability() { return stability; }
    public void stability(double v) { stability = Math.max(0, Math.min(1, v)); }
    public double control() { return control; }
    public void control(double v) { control = Math.max(0, Math.min(1, v)); }
    public double valence() { return valence; }
    public void valence(double v) { valence = Math.max(-1, Math.min(1, v)); }
    public double arousal() { return arousal; }
    public void arousal(double v) { arousal = Math.max(0, Math.min(1, v)); }
    public double persistence() { return persistence; }
    public void persistence(double v) { persistence = Math.max(0, Math.min(1, v)); }
    public CauseLog influences() { return influences; }
    public Set<UUID> relatedEvents() { return relatedEvents; }
    public int version() { return version; }
    public void version(int v) { version = Math.max(1, v); }
    public void bump() { version++; }
    public int reinforcements() { return reinforcements; }
    public void reinforcements(int v) { reinforcements = Math.max(0, v); }
    public UUID traumaId() { return traumaId; }
    public void traumaId(UUID v) { traumaId = v; }
    public boolean active() { return phase == EmotionPhase.ACTIVE || phase == EmotionPhase.FADING; }
}
