package yadi.samuraiai.ai.knowledge.engine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;
import yadi.samuraiai.ai.knowledge.model.Predicate;
import yadi.samuraiai.ai.knowledge.model.ValidationState;

/**
 * What one holder (an NPC or a community) believes, with indexes by subject, type, predicate, place cell, validation state, name
 * and tag, so that queries never walk the whole graph. Also remembers which rumours the holder has heard and, for an NPC, which
 * communities it belongs to and its rank in each. Never shared between holders.
 */
public final class KnowledgeRuntime {
    private static final Set<UUID> NONE = Set.of();
    private final UUID ownerId;
    private final Map<UUID, KnowledgeRecord> records = new LinkedHashMap<>();
    private final Map<String, UUID> byKey = new HashMap<>();
    private final Map<UUID, Set<UUID>> bySubject = new HashMap<>(), byObject = new HashMap<>();
    private final Map<String, Set<UUID>> byName = new HashMap<>(), byTag = new HashMap<>(), byCell = new HashMap<>();
    private final EnumMap<KnowledgeType, Set<UUID>> byType = new EnumMap<>(KnowledgeType.class);
    private final EnumMap<Predicate, Set<UUID>> byPredicate = new EnumMap<>(Predicate.class);
    private final EnumMap<ValidationState, Set<UUID>> byState = new EnumMap<>(ValidationState.class);
    private final Set<UUID> rumors = new LinkedHashSet<>();
    private final Map<String, yadi.samuraiai.ai.knowledge.model.AccessLevel> ranks = new LinkedHashMap<>();
    private boolean dirty, urgent;
    long lastDecay = Long.MIN_VALUE / 2;
    int cursor;
    private String storageState = "NEW";
    private long lastSaved;
    private final int cellSize;

    public KnowledgeRuntime(UUID ownerId, int cellSize) { this.ownerId = ownerId; this.cellSize = cellSize; }

    public UUID ownerId() { return ownerId; }
    public int size() { return records.size(); }
    public KnowledgeRecord get(UUID id) { return records.get(id); }
    public KnowledgeRecord byKey(String key) { UUID id = byKey.get(key); return id == null ? null : records.get(id); }
    public List<KnowledgeRecord> all() { return new ArrayList<>(records.values()); }
    public List<UUID> ids() { return new ArrayList<>(records.keySet()); }
    public Set<UUID> rumors() { return rumors; }
    public Map<String, yadi.samuraiai.ai.knowledge.model.AccessLevel> ranks() { return ranks; }
    public int cellSize() { return cellSize; }

    public void add(KnowledgeRecord r) {
        records.put(r.id(), r);
        byKey.put(r.key(), r.id());
        index(r);
        dirty = true;
    }

    public KnowledgeRecord remove(UUID id) {
        KnowledgeRecord r = records.remove(id);
        if (r == null) return null;
        byKey.remove(r.key(), id);
        unindex(r);
        dirty = true;
        return r;
    }

    public void reindex(KnowledgeRecord r, Runnable change) {
        if (records.get(r.id()) != r) { change.run(); return; }
        unindex(r);
        change.run();
        index(r);
        dirty = true;
    }

    private void index(KnowledgeRecord r) {
        UUID id = r.id();
        put(bySubject, r.subject().id(), id);
        if (r.object() != null) put(byObject, r.object().id(), id);
        put(byName, normalize(r.name()), id);
        if (r.object() != null) put(byName, normalize(r.object().label()), id);
        r.tags().forEach(t -> put(byTag, t, id));
        if (r.place().known()) put(byCell, r.place().cell(cellSize), id);
        byType.computeIfAbsent(r.type(), k -> new HashSet<>()).add(id);
        byPredicate.computeIfAbsent(r.predicate(), k -> new HashSet<>()).add(id);
        byState.computeIfAbsent(r.state(), k -> new HashSet<>()).add(id);
    }

    private void unindex(KnowledgeRecord r) {
        UUID id = r.id();
        drop(bySubject, r.subject().id(), id);
        if (r.object() != null) drop(byObject, r.object().id(), id);
        drop(byName, normalize(r.name()), id);
        if (r.object() != null) drop(byName, normalize(r.object().label()), id);
        r.tags().forEach(t -> drop(byTag, t, id));
        if (r.place().known()) drop(byCell, r.place().cell(cellSize), id);
        remove(byType, r.type(), id); remove(byPredicate, r.predicate(), id); remove(byState, r.state(), id);
    }

    public static String normalize(String name) { return name == null ? "" : name.toLowerCase(Locale.ROOT).trim(); }

    private static <K> void put(Map<K, Set<UUID>> map, K key, UUID id) { map.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(id); }
    private static <K> void drop(Map<K, Set<UUID>> map, K key, UUID id) {
        Set<UUID> set = map.get(key);
        if (set == null) return;
        set.remove(id);
        if (set.isEmpty()) map.remove(key);
    }
    private static <K extends Enum<K>> void remove(EnumMap<K, Set<UUID>> map, K key, UUID id) {
        Set<UUID> set = map.get(key);
        if (set != null) { set.remove(id); if (set.isEmpty()) map.remove(key); }
    }

    public Set<UUID> aboutSubject(UUID subject) { return bySubject.getOrDefault(subject, NONE); }
    public Set<UUID> aboutObject(UUID object) { return byObject.getOrDefault(object, NONE); }
    public Set<UUID> named(String name) { return byName.getOrDefault(normalize(name), NONE); }
    public Set<UUID> tagged(String tag) { return byTag.getOrDefault(tag, NONE); }
    public Set<UUID> inCell(String cell) { return byCell.getOrDefault(cell, NONE); }
    public Set<UUID> ofType(KnowledgeType type) { return byType.getOrDefault(type, NONE); }
    public Set<UUID> ofPredicate(Predicate p) { return byPredicate.getOrDefault(p, NONE); }
    public Set<UUID> inState(ValidationState s) { return byState.getOrDefault(s, NONE); }
    public int countIn(ValidationState s) { return inState(s).size(); }
    public Set<String> names() { return byName.keySet(); }
    public int subjects() { return bySubject.size(); }

    public void markDirty() { dirty = true; }
    public void markUrgent() { dirty = true; urgent = true; }
    public boolean dirty() { return dirty; }
    public boolean urgent() { return urgent; }
    public void saved(long now) { dirty = false; urgent = false; lastSaved = now; storageState = "SAVED"; }
    public long lastSaved() { return lastSaved; }
    public String storageState() { return storageState; }
    public void storageState(String v) { storageState = v; }
}
