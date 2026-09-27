package yadi.samuraiai.ai.memory.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.memory.cache.MemoryCache;
import yadi.samuraiai.ai.memory.indexing.MemoryIndex;
import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.memory.procedural.ProceduralMemory;
import yadi.samuraiai.ai.memory.semantic.SemanticMemory;
import yadi.samuraiai.ai.memory.spatial.SpatialMap;

/**
 * Everything one NPC remembers, and only that NPC: its memories, the indexes and caches over them, its mental map, its skills
 * and its stable beliefs. Never shared between NPCs. Not thread safe; the server thread owns it.
 */
public final class MemoryRuntime {
    private final UUID npcId;
    private final Map<UUID, MemoryRecord> records = new LinkedHashMap<>();
    private MemoryIndex index;
    private final MemoryCache cache;
    private final SpatialMap spatial = new SpatialMap();
    private final ProceduralMemory procedural = new ProceduralMemory();
    private final SemanticMemory semantic = new SemanticMemory();
    private final Set<UUID> temporary = new LinkedHashSet<>();
    private boolean dirty, urgent;
    long lastConsolidation = Long.MIN_VALUE / 2, lastMaintenance = Long.MIN_VALUE / 2, lastCompression = Long.MIN_VALUE / 2;
    int cursor;
    private long lastSaved;
    private String storageState = "NEW";
    private EmotionKind snapshotEmotion = EmotionKind.CALM;
    private double snapshotIntensity;
    private String snapshotMood = "";

    public MemoryRuntime(UUID npcId, MemorySettings settings) {
        this.npcId = npcId;
        this.index = new MemoryIndex(settings.cellSize());
        this.cache = new MemoryCache(settings.shortCacheSize(), settings.hotSize(), settings.warmSize(), settings.hotAccessThreshold());
    }

    public UUID npcId() { return npcId; }
    public MemoryIndex index() { return index; }
    public MemoryCache cache() { return cache; }
    public SpatialMap spatial() { return spatial; }
    public ProceduralMemory procedural() { return procedural; }
    public SemanticMemory semantic() { return semantic; }

    public MemoryRecord get(UUID id) { return records.get(id); }
    public boolean contains(UUID id) { return records.containsKey(id); }
    public int size() { return records.size(); }
    public List<MemoryRecord> all() { return new ArrayList<>(records.values()); }
    public List<UUID> ids() { return new ArrayList<>(records.keySet()); }

    public void add(MemoryRecord r) {
        records.put(r.id(), r);
        index.add(r);
        if (r.state() == yadi.samuraiai.ai.memory.model.MemoryState.TEMPORARY) temporary.add(r.id());
        cache.invalidate();
        dirty = true;
    }

    public MemoryRecord remove(UUID id) {
        MemoryRecord r = records.remove(id);
        if (r == null) return null;
        index.remove(r);
        temporary.remove(id);
        cache.forget(id);
        cache.invalidate();
        dirty = true;
        return r;
    }

    /** Runs a change that alters a record's index keys, keeping the indexes consistent. */
    public void reindex(MemoryRecord r, Runnable change) {
        if (records.get(r.id()) == r) index.reindex(r, change); else change.run();
        cache.invalidate();
        dirty = true;
    }

    public List<UUID> temporaryIds() { return new ArrayList<>(temporary); }
    public void dropTemporary(UUID id) { temporary.remove(id); }
    public int temporaryCount() { return temporary.size(); }

    public void markDirty() { dirty = true; }
    public void markUrgent() { dirty = true; urgent = true; }
    public boolean dirty() { return dirty; }
    public boolean urgent() { return urgent; }
    public void saved(long now) { dirty = false; urgent = false; lastSaved = now; storageState = "SAVED"; }
    public long lastSaved() { return lastSaved; }
    public String storageState() { return storageState; }
    public void storageState(String v) { storageState = v; }

    public void snapshotEmotion(EmotionKind kind, double intensity, String mood) { snapshotEmotion = kind; snapshotIntensity = intensity; snapshotMood = mood == null ? "" : mood; }
    public EmotionKind snapshotEmotion() { return snapshotEmotion; }
    public double snapshotIntensity() { return snapshotIntensity; }
    public String snapshotMood() { return snapshotMood; }

    /** Rebuilds the indexes (after the cell size changed, or after loading). */
    public void rebuildIndex(int cellSize) {
        index = new MemoryIndex(cellSize);
        for (MemoryRecord r : records.values()) index.add(r);
        cache.clear();
    }
}
