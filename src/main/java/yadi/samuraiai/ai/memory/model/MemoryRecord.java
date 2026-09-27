package yadi.samuraiai.ai.memory.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.cognition.model.Stamp;

/**
 * One structured memory. It is never free text: it has a type and category, a where/when/with-whom, an emotional signature,
 * importance and confidence, an origin that can be traced back to the world event, and links to what it caused. Owned by one
 * NPC's memory runtime; the runtime's engines mutate it, so all mutators are public but callers outside the engines should not.
 */
public final class MemoryRecord {
    private final UUID id, npcId;
    private MemoryType type;
    private final Category category;
    private final EpisodeKind episode;
    private final ExperienceKind kind;
    private final Stamp stamp;
    private long endTime;
    private final PlaceRef place;
    private final EntityRef actor, target;
    private final List<EntityRef> entities = new ArrayList<>();
    private EmotionalSignature emotion;
    private Importance importance;
    private double confidence = 1.0D;
    private long duration;
    private MemoryState state = MemoryState.TEMPORARY;
    private final Set<String> tags = new LinkedHashSet<>();
    private final Map<String, String> context = new LinkedHashMap<>();
    private final Origin origin;
    private final List<String> events = new ArrayList<>();
    private final List<Consequence> consequences = new ArrayList<>();
    private final List<Chapter> chapters = new ArrayList<>();
    private final List<Impression> impressions = new ArrayList<>();
    private final Set<UUID> socialLinks = new LinkedHashSet<>(), knowledgeLinks = new LinkedHashSet<>();
    private int version = 1;
    private double strength = 1.0D, emotionalWeight;
    private int accessCount, repeatCount = 1;
    private long lastAccess, lastDecay, lastReinforced, lastEcho = Long.MIN_VALUE / 2;
    private boolean protectedMemory, detailed = true, witnessed, semanticApplied;

    public MemoryRecord(UUID id, UUID npcId, MemoryType type, Category category, EpisodeKind episode, ExperienceKind kind, Stamp stamp, PlaceRef place,
                        EntityRef actor, EntityRef target, EmotionalSignature emotion, Importance importance, Origin origin) {
        this.id = id; this.npcId = npcId; this.type = type; this.category = category; this.episode = episode; this.kind = kind; this.stamp = stamp;
        this.endTime = stamp.gameTime(); this.place = place == null ? PlaceRef.unknown() : place; this.actor = actor; this.target = target;
        this.emotion = emotion == null ? EmotionalSignature.neutral() : emotion; this.importance = importance; this.origin = origin == null ? new Origin("", "", null) : origin;
        this.lastAccess = stamp.gameTime(); this.lastDecay = stamp.gameTime(); this.lastReinforced = stamp.gameTime();
    }

    public UUID id() { return id; }
    public UUID npcId() { return npcId; }
    public MemoryType type() { return type; }
    public void type(MemoryType v) { type = v; }
    public Category category() { return category; }
    public EpisodeKind episode() { return episode; }
    public ExperienceKind kind() { return kind; }
    public Stamp stamp() { return stamp; }
    public long endTime() { return endTime; }
    public void endTime(long v) { endTime = Math.max(endTime, v); }
    public PlaceRef place() { return place; }
    public EntityRef actor() { return actor; }
    public EntityRef target() { return target; }
    public List<EntityRef> entities() { return entities; }
    public EmotionalSignature emotion() { return emotion; }
    public void emotion(EmotionalSignature v) { emotion = v; }
    public Importance importance() { return importance; }
    public void importance(Importance v) { importance = v; }
    public double confidence() { return confidence; }
    public void confidence(double v) { confidence = Math.max(0.0D, Math.min(1.0D, v)); }
    public long duration() { return duration; }
    public void duration(long v) { duration = Math.max(0, v); }
    public MemoryState state() { return state; }
    public void state(MemoryState v) { state = v; }
    public Set<String> tags() { return tags; }
    public Map<String, String> context() { return context; }
    public Origin origin() { return origin; }
    public List<String> events() { return events; }
    public List<Consequence> consequences() { return consequences; }
    public List<Chapter> chapters() { return chapters; }
    public List<Impression> impressions() { return impressions; }
    public Set<UUID> socialLinks() { return socialLinks; }
    public Set<UUID> knowledgeLinks() { return knowledgeLinks; }
    public int version() { return version; }
    public void bumpVersion() { version++; }
    public void version(int v) { version = Math.max(1, v); }
    public double strength() { return strength; }
    public void strength(double v) { strength = Math.max(0.0D, Math.min(1.0D, v)); }
    public double emotionalWeight() { return emotionalWeight; }
    public void emotionalWeight(double v) { emotionalWeight = Math.max(0.0D, Math.min(1.0D, v)); }
    public int accessCount() { return accessCount; }
    public void accessCount(int v) { accessCount = Math.max(0, v); }
    public int repeatCount() { return repeatCount; }
    public void repeatCount(int v) { repeatCount = Math.max(1, v); }
    public long lastAccess() { return lastAccess; }
    public void lastAccess(long v) { lastAccess = v; }
    public long lastDecay() { return lastDecay; }
    public void lastDecay(long v) { lastDecay = v; }
    public long lastReinforced() { return lastReinforced; }
    public void lastReinforced(long v) { lastReinforced = v; }
    public long lastEcho() { return lastEcho; }
    public void lastEcho(long v) { lastEcho = v; }
    public boolean isProtected() { return protectedMemory; }
    public void protect(boolean v) { protectedMemory = v; }
    public boolean detailed() { return detailed; }
    public void detailed(boolean v) { detailed = v; }
    public boolean witnessed() { return witnessed; }
    public void witnessed(boolean v) { witnessed = v; }
    public boolean semanticApplied() { return semanticApplied; }
    public void semanticApplied(boolean v) { semanticApplied = v; }

    public boolean involves(UUID entity) {
        if (actor != null && actor.id().equals(entity)) return true;
        if (target != null && target.id().equals(entity)) return true;
        for (EntityRef e : entities) if (e.id().equals(entity)) return true;
        return false;
    }

    public void touch(long now) { accessCount++; lastAccess = now; }
    public boolean alive() { return state != MemoryState.FORGOTTEN; }

    /** A one-line description for logs and the debugger. */
    public String summary() {
        StringBuilder sb = new StringBuilder(kind.name()).append(" [").append(importance).append('/').append(state).append(']');
        if (actor != null) sb.append(" by ").append(actor.label());
        if (target != null) sb.append(" to ").append(target.label());
        if (!place.zone().isEmpty()) sb.append(" @").append(place.zone());
        if (repeatCount > 1) sb.append(" x").append(repeatCount);
        return sb.toString();
    }
}
