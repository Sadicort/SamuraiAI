package yadi.samuraiai.ai.emotion.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.emotion.model.Blend;
import yadi.samuraiai.ai.emotion.model.EmotionRecord;
import yadi.samuraiai.ai.emotion.model.HistoryEntry;
import yadi.samuraiai.ai.emotion.model.MoodKind;
import yadi.samuraiai.ai.emotion.model.Technique;
import yadi.samuraiai.ai.emotion.model.TraumaRecord;
import yadi.samuraiai.ai.emotion.regulation.RegulationAdvice;

/** Everything one NPC feels: active emotions, its mood and the scores behind it, traumas, the technique it is using, the emotional history and the current blend. Never shared. */
public final class EmotionRuntime {
    private final UUID npcId;
    private final Map<String, EmotionRecord> active = new LinkedHashMap<>();
    private final List<TraumaRecord> traumas = new ArrayList<>();
    private final Deque<HistoryEntry> history = new ArrayDeque<>();
    private final double[] moodScores = new double[MoodKind.values().length];
    private MoodKind mood = MoodKind.NEUTRAL;
    long moodSince, lastMoodUpdate = Long.MIN_VALUE / 2, lastUpdate = Long.MIN_VALUE / 2, lastRegulation = Long.MIN_VALUE / 2, lastContagion = Long.MIN_VALUE / 2;
    private Technique technique = Technique.NONE;
    private long techniqueUntil;
    private RegulationAdvice advice = RegulationAdvice.NONE;
    private Blend blend = Blend.CALM;
    private String lastBlendLabel = "";
    final Map<UUID, Long> lastFlashback = new HashMap<>();
    private final Map<MoodKind, Long> moodTicks = new EnumMap<>(MoodKind.class);
    private boolean dirty, urgent;
    private String storageState = "NEW";
    private long lastSaved, lastTriggerAt;
    private String lastTrigger = "";

    public EmotionRuntime(UUID npcId) { this.npcId = npcId; moodScores[MoodKind.NEUTRAL.ordinal()] = 0.15D; }

    public UUID npcId() { return npcId; }
    public Map<String, EmotionRecord> active() { return active; }
    public List<EmotionRecord> activeRecords() { return new ArrayList<>(active.values()); }
    public List<TraumaRecord> traumas() { return traumas; }
    public Deque<HistoryEntry> history() { return history; }
    public double[] moodScores() { return moodScores; }
    public MoodKind mood() { return mood; }
    public void mood(MoodKind v, long now) { mood = v; moodSince = now; }
    public long moodSince() { return moodSince; }
    public Technique technique() { return technique; }
    public long techniqueUntil() { return techniqueUntil; }
    public void technique(Technique t, long until) { technique = t; techniqueUntil = until; }
    public RegulationAdvice advice() { return advice; }
    public void advice(RegulationAdvice a) { advice = a; }
    public Blend blend() { return blend; }
    public void blend(Blend b) { blend = b; }
    public String lastBlendLabel() { return lastBlendLabel; }
    public void lastBlendLabel(String v) { lastBlendLabel = v; }
    public Map<MoodKind, Long> moodTicks() { return moodTicks; }
    public long lastUpdate() { return lastUpdate; }
    public void lastUpdate(long v) { lastUpdate = v; lastMoodUpdate = v; }

    public void addHistory(HistoryEntry entry, int max) {
        history.addLast(entry);
        while (history.size() > max) history.pollFirst();
        dirty = true;
    }

    public void trigger(String note, long at) { lastTrigger = note; lastTriggerAt = at; }
    public String lastTrigger() { return lastTrigger; }
    public long lastTriggerAt() { return lastTriggerAt; }

    public void markDirty() { dirty = true; }
    public void markUrgent() { dirty = true; urgent = true; }
    public boolean dirty() { return dirty; }
    public boolean urgent() { return urgent; }
    public void saved(long now) { dirty = false; urgent = false; lastSaved = now; storageState = "SAVED"; }
    public long lastSaved() { return lastSaved; }
    public String storageState() { return storageState; }
    public void storageState(String v) { storageState = v; }

    /** Total of the unpleasant emotions' intensity, the load that regulation reacts to. */
    public double unpleasantLoad() {
        double load = 0;
        for (EmotionRecord r : active.values()) if (r.kind().unpleasant()) load += r.intensity();
        return load;
    }

    public double intensityOf(yadi.samuraiai.ai.cognition.model.EmotionKind kind) {
        double remaining = 1.0D;
        for (EmotionRecord r : active.values()) if (r.kind() == kind) remaining *= 1.0D - r.intensity() / 100.0D;
        return 100.0D * (1.0D - remaining);
    }
}
