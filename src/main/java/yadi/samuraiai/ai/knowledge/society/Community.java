package yadi.samuraiai.ai.knowledge.society;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.knowledge.culture.TraditionState;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeRuntime;
import yadi.samuraiai.ai.knowledge.history.HistoricalEvent;
import yadi.samuraiai.ai.knowledge.model.AccessLevel;

/**
 * A community (village, temple, market, clan, faction, guard post): its members and their ranks, its culture, its collective
 * knowledge (what enough of its members know), its history, the state of its traditions and the standing it gives people. It is
 * the society runtime; its collective knowledge is a {@link KnowledgeRuntime} of its own.
 */
public final class Community {
    private final String id;
    private String name, cultureId;
    private final CommunityKind kind;
    private PlaceRef center;
    private double radius;
    private UUID leader;
    private final Map<UUID, AccessLevel> members = new LinkedHashMap<>();
    private final KnowledgeRuntime collective;
    private final List<HistoricalEvent> history = new ArrayList<>();
    private final Map<String, TraditionState> traditions = new LinkedHashMap<>();
    private final Map<String, Set<UUID>> support = new HashMap<>();
    private final Map<UUID, Map<String, Double>> standing = new HashMap<>();
    private final Map<UUID, EntityRef> standingSubjects = new HashMap<>();
    private boolean dirty;

    public Community(String id, String name, CommunityKind kind, String cultureId, PlaceRef center, double radius, int cellSize) {
        this.id = id; this.name = name; this.kind = kind; this.cultureId = cultureId; this.center = center == null ? PlaceRef.unknown() : center; this.radius = radius;
        this.collective = new KnowledgeRuntime(UUID.nameUUIDFromBytes(("community:" + id).getBytes(java.nio.charset.StandardCharsets.UTF_8)), cellSize);
    }

    public String id() { return id; }
    public UUID uuid() { return collective.ownerId(); }
    public String name() { return name; }
    public void name(String v) { name = v; }
    public CommunityKind kind() { return kind; }
    public String cultureId() { return cultureId; }
    public void cultureId(String v) { cultureId = v; }
    public PlaceRef center() { return center; }
    public void center(PlaceRef v) { center = v; }
    public double radius() { return radius; }
    public void radius(double v) { radius = v; }
    public UUID leader() { return leader; }
    public void leader(UUID v) { leader = v; }
    public Map<UUID, AccessLevel> members() { return members; }
    public KnowledgeRuntime collective() { return collective; }
    public List<HistoricalEvent> history() { return history; }
    public Map<String, TraditionState> traditions() { return traditions; }
    public Map<String, Set<UUID>> support() { return support; }
    public Map<UUID, Map<String, Double>> standing() { return standing; }
    public Map<UUID, EntityRef> standingSubjects() { return standingSubjects; }
    public boolean dirty() { return dirty; }
    public void markDirty() { dirty = true; }
    public void clean() { dirty = false; }
    public boolean member(UUID npc) { return members.containsKey(npc); }
    public AccessLevel rankOf(UUID npc) { return members.getOrDefault(npc, AccessLevel.PUBLIC); }
    public Set<UUID> memberIds() { return new LinkedHashSet<>(members.keySet()); }
}
