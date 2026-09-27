package yadi.samuraiai.ai.knowledge.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.PlaceRef;

/**
 * One thing an NPC (or a community) believes: a subject, how it relates to an object or value, how sure the holder is, where
 * the belief came from and whether it has been validated. Beliefs can be wrong; a rumour is not a verified fact. Every belief
 * has a source and keeps a revision history, so it can be explained and evolves rather than being overwritten.
 */
public final class KnowledgeRecord {
    private final UUID id, owner;
    private final KnowledgeType type;
    private KnowledgeCategory category;
    private final EntityRef subject;
    private final Predicate predicate;
    private final EntityRef object;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private LearnMethod origin;
    private EntityRef source;
    private double confidence, importance;
    private long learned, updated, lastDecay, lastUsed;
    private PlaceRef place = PlaceRef.unknown();
    private final Set<String> tags = new LinkedHashSet<>();
    private final Set<UUID> links = new LinkedHashSet<>(), supporters = new LinkedHashSet<>(), contradictors = new LinkedHashSet<>(), memoryLinks = new LinkedHashSet<>();
    private ValidationState state = ValidationState.UNKNOWN;
    private AccessLevel access = AccessLevel.PUBLIC;
    private final List<Revision> revisions = new ArrayList<>();
    private int version = 1, uses;
    private boolean directEvidence, publicEvidence, trustedEvidence;
    private UUID rumorId, traceId;

    public KnowledgeRecord(UUID id, UUID owner, KnowledgeType type, KnowledgeCategory category, EntityRef subject, Predicate predicate, EntityRef object, LearnMethod origin,
                           double confidence, long learned) {
        this.id = id; this.owner = owner; this.type = type; this.category = category; this.subject = subject; this.predicate = predicate; this.object = object;
        this.origin = origin; this.confidence = confidence; this.learned = learned; this.updated = learned; this.lastDecay = learned; this.lastUsed = learned;
    }

    public static String key(KnowledgeType type, UUID subject, Predicate predicate, UUID object) { return type + "|" + subject + "|" + predicate + "|" + (object == null ? "-" : object); }
    public String key() { return key(type, subject.id(), predicate, object == null ? null : object.id()); }

    public UUID id() { return id; }
    public UUID owner() { return owner; }
    public KnowledgeType type() { return type; }
    public KnowledgeCategory category() { return category; }
    public void category(KnowledgeCategory v) { category = v; }
    public EntityRef subject() { return subject; }
    public Predicate predicate() { return predicate; }
    public EntityRef object() { return object; }
    public Map<String, String> attributes() { return attributes; }
    public LearnMethod origin() { return origin; }
    public void origin(LearnMethod v) { origin = v; }
    public EntityRef source() { return source; }
    public void source(EntityRef v) { source = v; }
    public double confidence() { return confidence; }
    public void confidence(double v) { confidence = Math.max(0.0D, Math.min(1.0D, Double.isFinite(v) ? v : 0.0D)); }
    public double importance() { return importance; }
    public void importance(double v) { importance = Math.max(0.0D, Math.min(1.0D, v)); }
    public long learned() { return learned; }
    public long updated() { return updated; }
    public void updated(long v) { updated = v; }
    public long lastDecay() { return lastDecay; }
    public void lastDecay(long v) { lastDecay = v; }
    public long lastUsed() { return lastUsed; }
    public PlaceRef place() { return place; }
    public void place(PlaceRef v) { place = v == null ? PlaceRef.unknown() : v; }
    public Set<String> tags() { return tags; }
    public Set<UUID> links() { return links; }
    public Set<UUID> supporters() { return supporters; }
    public Set<UUID> contradictors() { return contradictors; }
    public Set<UUID> memoryLinks() { return memoryLinks; }
    public ValidationState state() { return state; }
    public void state(ValidationState v) { state = v; }
    public AccessLevel access() { return access; }
    public void access(AccessLevel v) { access = v; }
    public List<Revision> revisions() { return revisions; }
    public int version() { return version; }
    public void version(int v) { version = Math.max(1, v); }
    public void bump() { version++; }
    public int uses() { return uses; }
    public void uses(int v) { uses = Math.max(0, v); }
    public boolean directEvidence() { return directEvidence; }
    public void directEvidence(boolean v) { directEvidence = v; }
    public boolean publicEvidence() { return publicEvidence; }
    public void publicEvidence(boolean v) { publicEvidence = v; }
    public boolean trustedEvidence() { return trustedEvidence; }
    public void trustedEvidence(boolean v) { trustedEvidence = v; }
    public UUID rumorId() { return rumorId; }
    public void rumorId(UUID v) { rumorId = v; }
    public UUID traceId() { return traceId; }
    public void traceId(UUID v) { traceId = v; }

    public void used(long now) { uses++; lastUsed = now; }
    public boolean alive() { return state != ValidationState.FORGOTTEN; }

    public String name() {
        String n = attributes.get("name");
        return n != null && !n.isEmpty() ? n : subject.label();
    }

    public String summary() {
        return predicate + " " + subject.label() + (object == null ? "" : " -> " + object.label()) + " [" + state + " " + Math.round(confidence * 100) + "%]";
    }
}
