package yadi.samuraiai.ai.memory.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.cognition.model.Stamp;

/**
 * Something an NPC lived through, before it is (or is not) kept. Perception produces the evidence, the cognition catalogue
 * gives it a meaning, and the memory engine decides whether it deserves to become a {@link MemoryRecord}. Immutable.
 */
public final class Experience {
    private final UUID id, npcId, traceId;
    private final ExperienceKind kind;
    private final Category category;
    private final EpisodeKind episode;
    private final EntityRef actor, target;
    private final List<EntityRef> participants;
    private final PlaceRef place;
    private final Stamp stamp;
    private final Outcome outcome;
    private final EmotionalSignature emotion;
    private final List<String> events;
    private final Origin origin;
    private final Map<String, String> context;
    private final List<Consequence> consequences;
    private final List<Impression> impressions;
    private final Set<String> tags;
    private final double magnitude, danger;
    private final long duration;
    private final boolean repeatable, witnessed, publicEvent;

    private Experience(Builder b) {
        this.id = b.id; this.npcId = b.npcId; this.traceId = b.traceId; this.kind = b.kind; this.category = b.category; this.episode = b.episode;
        this.actor = b.actor; this.target = b.target; this.participants = List.copyOf(b.participants); this.place = b.place; this.stamp = b.stamp;
        this.outcome = b.outcome; this.emotion = b.emotion; this.events = List.copyOf(b.events); this.origin = b.origin;
        this.context = Map.copyOf(b.context); this.consequences = List.copyOf(b.consequences); this.impressions = List.copyOf(b.impressions);
        this.tags = Set.copyOf(b.tags); this.magnitude = b.magnitude; this.danger = b.danger; this.duration = b.duration;
        this.repeatable = b.repeatable; this.witnessed = b.witnessed; this.publicEvent = b.publicEvent;
    }

    public UUID id() { return id; }
    public UUID npcId() { return npcId; }
    public UUID traceId() { return traceId; }
    public ExperienceKind kind() { return kind; }
    public Category category() { return category; }
    public EpisodeKind episode() { return episode; }
    public EntityRef actor() { return actor; }
    public EntityRef target() { return target; }
    public List<EntityRef> participants() { return participants; }
    public PlaceRef place() { return place; }
    public Stamp stamp() { return stamp; }
    public Outcome outcome() { return outcome; }
    public EmotionalSignature emotion() { return emotion; }
    public List<String> events() { return events; }
    public Origin origin() { return origin; }
    public Map<String, String> context() { return context; }
    public List<Consequence> consequences() { return consequences; }
    public List<Impression> impressions() { return impressions; }
    public Set<String> tags() { return tags; }
    /** How much this kind of thing matters in itself, 0-1. */
    public double magnitude() { return magnitude; }
    public double danger() { return danger; }
    public long duration() { return duration; }
    /** Whether repeats of this experience should reinforce one memory rather than each becoming a new one. */
    public boolean repeatable() { return repeatable; }
    /** The NPC saw it happen rather than took part. */
    public boolean witnessed() { return witnessed; }
    public boolean publicEvent() { return publicEvent; }

    public static Builder builder(UUID npcId, ExperienceKind kind) { return new Builder(npcId, kind); }

    public static final class Builder {
        private UUID id = UUID.randomUUID(), traceId = UUID.randomUUID();
        private final UUID npcId;
        private final ExperienceKind kind;
        private Category category = Category.PERSONAL;
        private EpisodeKind episode = EpisodeKind.OBSERVATION;
        private EntityRef actor, target;
        private final List<EntityRef> participants = new ArrayList<>();
        private PlaceRef place = PlaceRef.unknown();
        private Stamp stamp = Stamp.of(0);
        private Outcome outcome = Outcome.NEUTRAL;
        private EmotionalSignature emotion = EmotionalSignature.neutral();
        private final List<String> events = new ArrayList<>();
        private Origin origin = new Origin("", "", null);
        private final Map<String, String> context = new LinkedHashMap<>();
        private final List<Consequence> consequences = new ArrayList<>();
        private final List<Impression> impressions = new ArrayList<>();
        private final Set<String> tags = new LinkedHashSet<>();
        private double magnitude = 0.3D, danger;
        private long duration;
        private boolean repeatable, witnessed, publicEvent;

        private Builder(UUID npcId, ExperienceKind kind) { this.npcId = Objects.requireNonNull(npcId); this.kind = Objects.requireNonNull(kind); }

        public Builder id(UUID v) { id = v; return this; }
        public Builder trace(UUID v) { traceId = v; return this; }
        public Builder category(Category v) { category = v; return this; }
        public Builder episode(EpisodeKind v) { episode = v; return this; }
        public Builder actor(EntityRef v) { actor = v; return this; }
        public Builder target(EntityRef v) { target = v; return this; }
        public Builder participant(EntityRef v) { if (v != null) participants.add(v); return this; }
        public Builder place(PlaceRef v) { place = v == null ? PlaceRef.unknown() : v; return this; }
        public Builder stamp(Stamp v) { stamp = v; return this; }
        public Builder outcome(Outcome v) { outcome = v; return this; }
        public Builder emotion(EmotionalSignature v) { emotion = v == null ? EmotionalSignature.neutral() : v; return this; }
        public Builder event(String v) { if (v != null && !v.isEmpty()) events.add(v); return this; }
        public Builder origin(Origin v) { origin = v; return this; }
        public Builder context(String key, String value) { context.put(key, value); return this; }
        public Builder consequence(Consequence v) { consequences.add(v); return this; }
        public Builder impression(Impression v) { impressions.add(v); return this; }
        public Builder tag(String v) { if (v != null && !v.isEmpty()) tags.add(v); return this; }
        public Builder magnitude(double v) { magnitude = Math.max(0, Math.min(1, v)); return this; }
        public Builder danger(double v) { danger = Math.max(0, Math.min(1, v)); return this; }
        public Builder duration(long v) { duration = Math.max(0, v); return this; }
        public Builder repeatable(boolean v) { repeatable = v; return this; }
        public Builder witnessed(boolean v) { witnessed = v; return this; }
        public Builder publicEvent(boolean v) { publicEvent = v; return this; }
        public Experience build() { return new Experience(this); }
    }
}
