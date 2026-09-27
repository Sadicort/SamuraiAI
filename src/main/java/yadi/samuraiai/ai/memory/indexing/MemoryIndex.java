package yadi.samuraiai.ai.memory.indexing;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.memory.model.Category;
import yadi.samuraiai.ai.memory.model.Importance;
import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.memory.model.MemoryType;

/**
 * Secondary indexes over one NPC's memories: person, place (cell and zone), emotion, day, importance, category, kind, type, tag
 * and event. A lookup reads a small set instead of scanning the whole history, which is what lets thousands of memories coexist
 * with per-decision retrieval.
 */
public final class MemoryIndex {
    private static final Set<UUID> NONE = Set.of();
    private final int cellSize;
    private final Map<UUID, Set<UUID>> byEntity = new HashMap<>();
    private final Map<String, Set<UUID>> byCell = new HashMap<>(), byZone = new HashMap<>(), byTag = new HashMap<>(), byEvent = new HashMap<>();
    private final Map<Long, Set<UUID>> byDay = new HashMap<>();
    private final EnumMap<EmotionKind, Set<UUID>> byEmotion = new EnumMap<>(EmotionKind.class);
    private final EnumMap<Importance, Set<UUID>> byImportance = new EnumMap<>(Importance.class);
    private final EnumMap<Category, Set<UUID>> byCategory = new EnumMap<>(Category.class);
    private final EnumMap<ExperienceKind, Set<UUID>> byKind = new EnumMap<>(ExperienceKind.class);
    private final EnumMap<MemoryType, Set<UUID>> byType = new EnumMap<>(MemoryType.class);
    private int size;

    public MemoryIndex(int cellSize) { this.cellSize = cellSize; }

    public int cellSize() { return cellSize; }
    public int size() { return size; }

    public void add(MemoryRecord r) {
        UUID id = r.id();
        size++;
        if (r.actor() != null) put(byEntity, r.actor().id(), id);
        if (r.target() != null) put(byEntity, r.target().id(), id);
        r.entities().forEach(e -> put(byEntity, e.id(), id));
        String cell = r.place().cell(cellSize);
        if (!cell.isEmpty()) put(byCell, cell, id);
        if (!r.place().zone().isEmpty()) put(byZone, r.place().zone(), id);
        r.tags().forEach(t -> put(byTag, t, id));
        r.events().forEach(e -> put(byEvent, e, id));
        put(byDay, r.stamp().day(), id);
        put(byEmotion, r.emotion().primary(), id);
        r.emotion().secondary().forEach(s -> put(byEmotion, s, id));
        put(byImportance, r.importance(), id);
        put(byCategory, r.category(), id);
        put(byKind, r.kind(), id);
        put(byType, r.type(), id);
    }

    public void remove(MemoryRecord r) {
        UUID id = r.id();
        size = Math.max(0, size - 1);
        if (r.actor() != null) drop(byEntity, r.actor().id(), id);
        if (r.target() != null) drop(byEntity, r.target().id(), id);
        r.entities().forEach(e -> drop(byEntity, e.id(), id));
        String cell = r.place().cell(cellSize);
        if (!cell.isEmpty()) drop(byCell, cell, id);
        if (!r.place().zone().isEmpty()) drop(byZone, r.place().zone(), id);
        r.tags().forEach(t -> drop(byTag, t, id));
        r.events().forEach(e -> drop(byEvent, e, id));
        drop(byDay, r.stamp().day(), id);
        drop(byEmotion, r.emotion().primary(), id);
        r.emotion().secondary().forEach(s -> drop(byEmotion, s, id));
        drop(byImportance, r.importance(), id);
        drop(byCategory, r.category(), id);
        drop(byKind, r.kind(), id);
        drop(byType, r.type(), id);
    }

    /** Applies a change to a record that affects its index keys (tags, importance, emotion, entities, type) without leaving stale entries. */
    public void reindex(MemoryRecord r, Runnable change) {
        remove(r);
        change.run();
        add(r);
    }

    public Set<UUID> entity(UUID id) { return byEntity.getOrDefault(id, NONE); }
    public Set<UUID> cell(String cell) { return byCell.getOrDefault(cell, NONE); }
    public Set<UUID> zone(String zone) { return byZone.getOrDefault(zone, NONE); }
    public Set<UUID> tag(String tag) { return byTag.getOrDefault(tag, NONE); }
    public Set<UUID> event(String event) { return byEvent.getOrDefault(event, NONE); }
    public Set<UUID> day(long day) { return byDay.getOrDefault(day, NONE); }
    public Set<UUID> emotion(EmotionKind kind) { return byEmotion.getOrDefault(kind, NONE); }
    public Set<UUID> importance(Importance importance) { return byImportance.getOrDefault(importance, NONE); }
    public Set<UUID> category(Category category) { return byCategory.getOrDefault(category, NONE); }
    public Set<UUID> kind(ExperienceKind kind) { return byKind.getOrDefault(kind, NONE); }
    public Set<UUID> type(MemoryType type) { return byType.getOrDefault(type, NONE); }

    /** Every memory of at least the given importance. */
    public Set<UUID> atLeast(Importance minimum) {
        Set<UUID> result = new HashSet<>();
        for (Importance level : Importance.values()) if (level.atLeast(minimum)) result.addAll(importance(level));
        return result;
    }

    public Set<Long> days() { return byDay.keySet(); }
    public int entityCount() { return byEntity.size(); }
    public int tagCount() { return byTag.size(); }
    public int cellCount() { return byCell.size(); }

    private static <K> void put(Map<K, Set<UUID>> map, K key, UUID id) { map.computeIfAbsent(key, k -> new HashSet<>()).add(id); }
    private static <K> void drop(Map<K, Set<UUID>> map, K key, UUID id) {
        Set<UUID> set = map.get(key);
        if (set == null) return;
        set.remove(id);
        if (set.isEmpty()) map.remove(key);
    }
    private static <K extends Enum<K>> void put(EnumMap<K, Set<UUID>> map, K key, UUID id) { map.computeIfAbsent(key, k -> new HashSet<>()).add(id); }
    private static <K extends Enum<K>> void drop(EnumMap<K, Set<UUID>> map, K key, UUID id) {
        Set<UUID> set = map.get(key);
        if (set == null) return;
        set.remove(id);
        if (set.isEmpty()) map.remove(key);
    }
}
