package yadi.samuraiai.ai.cognition.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import yadi.samuraiai.ai.cognition.catalog.ExperienceCatalog;
import yadi.samuraiai.ai.cognition.catalog.ExperienceProfile;
import yadi.samuraiai.ai.cognition.catalog.KnowledgeHint;
import yadi.samuraiai.ai.cognition.catalog.Selector;
import yadi.samuraiai.ai.cognition.events.PersonalityEvolvedEvent;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.EntityKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.PersonalityView;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.cognition.model.Stamp;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.cognition.personality.PersonalityLedger;
import yadi.samuraiai.ai.cognition.personality.TemporaryModifiers;
import yadi.samuraiai.ai.cognition.storage.CognitionStorage;
import yadi.samuraiai.ai.cognition.trace.CognitiveTrace;
import yadi.samuraiai.ai.cognition.trace.TraceStage;
import yadi.samuraiai.ai.emotion.engine.EmotionEngine;
import yadi.samuraiai.ai.emotion.engine.EmotionRuntime;
import yadi.samuraiai.ai.emotion.engine.EmotionSettings;
import yadi.samuraiai.ai.emotion.engine.TriggerResult;
import yadi.samuraiai.ai.emotion.model.Activity;
import yadi.samuraiai.ai.emotion.model.EmotionEffect;
import yadi.samuraiai.ai.emotion.model.EmotionTrigger;
import yadi.samuraiai.ai.emotion.model.RecoverySource;
import yadi.samuraiai.ai.emotion.model.Technique;
import yadi.samuraiai.ai.emotion.model.TriggerSource;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeEngine;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeRuntime;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings;
import yadi.samuraiai.ai.knowledge.engine.LearnResult;
import yadi.samuraiai.ai.knowledge.engine.SourceView;
import yadi.samuraiai.ai.knowledge.history.HistoricalEvent;
import yadi.samuraiai.ai.knowledge.model.KnowledgeCategory;
import yadi.samuraiai.ai.knowledge.model.KnowledgeEvidence;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;
import yadi.samuraiai.ai.knowledge.model.LearnMethod;
import yadi.samuraiai.ai.knowledge.model.Predicate;
import yadi.samuraiai.ai.knowledge.model.ValidationState;
import yadi.samuraiai.ai.knowledge.places.PlaceKnowledge;
import yadi.samuraiai.ai.knowledge.rumors.RumorClaim;
import yadi.samuraiai.ai.knowledge.rumors.RumorRecord;
import yadi.samuraiai.ai.knowledge.society.SocietyEngine;
import yadi.samuraiai.ai.memory.engine.MemoryEngine;
import yadi.samuraiai.ai.memory.engine.MemoryRuntime;
import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.memory.engine.Observation;
import yadi.samuraiai.ai.memory.model.EmotionalSignature;
import yadi.samuraiai.ai.memory.model.Experience;
import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.memory.model.Origin;
import yadi.samuraiai.ai.memory.pipeline.EvaluationContext;
import yadi.samuraiai.ai.memory.retrieval.RetrievalContext;
import yadi.samuraiai.ai.memory.retrieval.RetrievalResult;
import yadi.samuraiai.ai.relationship.engine.RelationshipEngine;
import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.ai.relationship.engine.Update;
import yadi.samuraiai.ai.relationship.honor.HonorSystem;
import yadi.samuraiai.ai.relationship.model.FriendshipStage;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;
import yadi.samuraiai.ai.relationship.model.ReputationHearsay;
import yadi.samuraiai.ai.relationship.model.ReputationLabel;
import yadi.samuraiai.ai.relationship.model.ReputationRecord;
import yadi.samuraiai.ai.relationship.model.ReputationScope;
import yadi.samuraiai.ai.relationship.model.SocialEvidence;
import yadi.samuraiai.ai.relationship.model.TrustLevel;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.event.NpcEvent;

/**
 * The cognitive layer: the one place where a world fact becomes a life. An experience enters here and flows through
 * memory (kept or not), emotion (what it makes the NPC feel, and how that feeling reinforces the memory), relationships
 * (what it means for the person involved), knowledge (what it teaches), society (rumours, history, collective knowledge) and
 * personality (a slow drift). The four engines never call one another; this class translates between them through explicit
 * contracts (evidence, triggers, hearsay), which is what keeps the dependencies acyclic and each engine testable alone.
 */
public final class CognitionEngine {
    private final Supplier<CognitionSettings> settings;
    private EventSink events;
    private final MemoryEngine memory;
    private final RelationshipEngine relationships;
    private final EmotionEngine emotions;
    private final KnowledgeEngine knowledge;
    private final SocietyEngine society;
    private final CognitiveTrace trace;
    private final CognitionMetrics metrics = new CognitionMetrics();
    private final Map<UUID, PersonalityLedger> ledgers = new HashMap<>();
    private final Set<UUID> loaded = new HashSet<>();
    private WorldFacts facts = WorldFacts.NONE;
    private Function<UUID, double[]> traitSource = id -> null;
    private CognitionStorage storage;
    private ExperienceCatalog catalog;
    private List<String> catalogSource;
    private long lastSaveSweep = Long.MIN_VALUE / 2;

    private final SourceView sourceView = new SourceView() {
        @Override public double trust(UUID listener, UUID source) { return relationships.find(listener, source).map(RelationshipRecord::trust).orElse(relationships.settings().initialTrust()); }
        @Override public double respect(UUID listener, UUID source) { return relationships.find(listener, source).map(RelationshipRecord::respect).orElse(relationships.settings().initialRespect()); }
        @Override public double reputation(UUID listener, UUID source) {
            double good = 0, bad = 0;
            for (ReputationRecord r : relationships.reputationOf(listener, source)) for (var e : r.scores().entrySet()) { if (RelationshipEngine.positive(e.getKey())) good = Math.max(good, e.getValue()); else if (e.getKey() != ReputationLabel.UNKNOWN) bad = Math.max(bad, e.getValue()); }
            return Math.max(0.0D, Math.min(1.0D, 0.5D + (good - bad) / 2.0D));
        }
        @Override public PersonalityView personality(UUID npc) { return CognitionEngine.this.personality(npc); }
    };

    public CognitionEngine(Supplier<CognitionSettings> settings, Supplier<MemorySettings> memorySettings, Supplier<RelationshipSettings> relationshipSettings, Supplier<EmotionSettings> emotionSettings,
                           Supplier<KnowledgeSettings> knowledgeSettings, EventSink events) {
        this.settings = Objects.requireNonNull(settings);
        this.events = Objects.requireNonNull(events);
        this.memory = new MemoryEngine(memorySettings, events);
        this.relationships = new RelationshipEngine(relationshipSettings, events);
        this.emotions = new EmotionEngine(emotionSettings, events);
        this.knowledge = new KnowledgeEngine(knowledgeSettings, events);
        this.society = new SocietyEngine(knowledgeSettings, events, knowledge);
        this.trace = new CognitiveTrace(settings.get().traceCapacity());
        this.knowledge.cellSize(memorySettings.get().cellSize());
        Function<UUID, PersonalityView> personalities = this::personality;
        memory.usePersonalities(personalities);
        relationships.usePersonalities(personalities);
        relationships.useMoods(emotions::moodValence);
        emotions.usePersonalities(personalities);
        knowledge.usePersonalities(personalities);
    }

    public MemoryEngine memory() { return memory; }
    public RelationshipEngine relationships() { return relationships; }
    public EmotionEngine emotions() { return emotions; }
    public KnowledgeEngine knowledge() { return knowledge; }
    public SocietyEngine society() { return society; }
    public CognitiveTrace trace() { return trace; }
    public CognitionMetrics metrics() { return metrics; }
    public CognitionSettings settings() { return settings.get(); }
    public SourceView sourceView() { return sourceView; }
    public void useFacts(WorldFacts facts) { this.facts = Objects.requireNonNull(facts); }
    public WorldFacts facts() { return facts; }
    public void useTraitSource(Function<UUID, double[]> source) { this.traitSource = Objects.requireNonNull(source); }
    public void useStorage(CognitionStorage storage) { this.storage = storage; }
    public CognitionStorage storage() { return storage; }
    public void useEventSink(EventSink sink) {
        this.events = Objects.requireNonNull(sink);
        memory.useEventSink(sink); relationships.useEventSink(sink); emotions.useEventSink(sink); knowledge.useEventSink(sink); society.useEventSink(sink);
    }
    private void publish(NpcEvent event) { events.publish(event); }

    private ExperienceCatalog catalog() {
        List<String> lines = settings.get().experienceOverrides();
        if (catalog == null || lines != catalogSource) { catalog = new ExperienceCatalog(lines); catalogSource = lines; }
        return catalog;
    }

    public ExperienceCatalog experienceCatalog() { return catalog(); }

    // ------------------------------------------------------------------ personality

    public PersonalityLedger ledger(UUID npc) {
        PersonalityLedger ledger = ledgers.computeIfAbsent(npc, PersonalityLedger::new);
        if (!ledger.seeded()) {
            double[] seed = traitSource.apply(npc);
            if (seed != null) ledger.seed(seed);
        }
        return ledger;
    }

    /** The effective personality: base + long-term evolution + temporary emotional modifiers. */
    public PersonalityView personality(UUID npc) {
        PersonalityLedger ledger = ledger(npc);
        double cap = settings.get().temporaryCap();
        return ledger.view(trait -> TemporaryModifiers.points(trait, kind -> emotions.intensity(npc, kind), cap));
    }

    /** The long-term personality (no temporary modifiers), the one the scheduler should carry. */
    public double[] longTermTraits(UUID npc) {
        PersonalityLedger ledger = ledger(npc);
        double[] values = new double[Trait.values().length];
        for (Trait t : Trait.values()) values[t.ordinal()] = ledger.longTerm(t);
        return values;
    }

    public void installLedger(PersonalityLedger ledger) { ledgers.put(ledger.npcId(), ledger); }

    // ------------------------------------------------------------------ experiences

    private double relationshipWeight(UUID npc, EntityRef actor) {
        if (actor == null) return 0.0D;
        return relationships.find(npc, actor.id()).map(r -> Math.max(0.0D, Math.min(1.0D, Math.max(Math.max(r.trust(), r.fear()), Math.max(r.respect(), r.rivalry())) / 100.0D))).orElse(0.0D);
    }

    private EntityRef placeEntity(PlaceRef place, Map<String, String> context) {
        String key = context.getOrDefault("placeKey", place.zone().isEmpty() ? place.dimension() + place.cell(32) : place.zone());
        String name = context.getOrDefault("placeName", place.zone().isEmpty() ? "lugar" : place.zone());
        return new EntityRef(EntityRef.nameId("place", key), EntityKind.PLACE, name);
    }

    private EntityRef select(Selector selector, ExperienceInput in, EntityRef self, PlaceRef place) {
        return switch (selector) {
            case ACTOR -> in.actor; case TARGET -> in.target; case SELF -> self; case PLACE -> place.known() ? placeEntity(place, in.context) : null; default -> null;
        };
    }

    /**
     * An NPC lived something. The experience is judged by memory; if kept, it drives emotion, relationships, knowledge, society and
     * personality, each through its own contract, and everything it caused carries the same trace id. Returns what happened.
     */
    public CognitionOutcome experience(ExperienceInput in) {
        long started = System.nanoTime();
        CognitionSettings cs = settings.get();
        UUID traceId = in.trace != null ? in.trace : UUID.randomUUID();
        UUID experienceId = UUID.randomUUID();
        ExperienceProfile p = catalog().get(in.kind);
        metrics.experiences.incrementAndGet();
        if (p == null) return CognitionOutcome.rejected(traceId, experienceId);
        try {
            UUID npc = in.npc;
            EntityRef self = ref(npc);
            boolean witnessed = in.witnessed != null ? in.witnessed : p.witnessedByDefault;
            boolean publicEvent = in.publicEvent != null ? in.publicEvent : p.publicEvent;
            boolean traumatic = in.traumatic != null ? in.traumatic : p.traumatic;
            PlaceRef place = in.place != null ? in.place : facts.placeOf(npc);
            double scale = in.magnitudeScale;
            trace.record(traceId, npc, TraceStage.EXPERIENCE, in.kind + (in.actor == null ? "" : " by " + in.actor.label()), in.at);

            // ---- memory
            long t0 = System.nanoTime();
            Experience exp = buildExperience(experienceId, traceId, in, p, place, witnessed, publicEvent, traumatic, scale);
            Observation observation = memory.observe(exp, new EvaluationContext(facts.goalTags(npc), relationshipWeight(npc, in.actor), personality(npc), dominantIntensity(npc)), in.at);
            metrics.memoryNanos.addAndGet(System.nanoTime() - t0);
            if (!observation.kept()) { metrics.discarded.incrementAndGet(); return CognitionOutcome.rejected(traceId, experienceId); }
            MemoryRecord mem = observation.record();
            trace.record(traceId, npc, TraceStage.MEMORY, observation.result() + " " + mem.summary(), in.at);

            // ---- emotion
            t0 = System.nanoTime();
            List<EmotionEffect> effects = new ArrayList<>();
            for (var e : p.emotions.entrySet()) effects.add(new EmotionEffect(e.getKey(), e.getValue() * (witnessed ? 0.85D : 1.0D) * Math.min(1.5D, scale)));
            TriggerResult emotion = emotions.trigger(new EmotionTrigger(npc, TriggerSource.MEMORY, effects, mem.id(), in.actor == null ? in.kind.name() : in.actor.id().toString(), traceId, in.at, place,
                    in.actor != null ? in.actor : in.target, traumatic, mem.importance().weight(), false, in.kind.name(), in.note));
            metrics.emotionNanos.addAndGet(System.nanoTime() - t0);
            trace.record(traceId, npc, TraceStage.EMOTION, emotion.records().size() + " emotions" + (emotion.trauma() != null ? ", trauma" : ""), in.at);
            double weight = emotions.weightOf(emotion);
            if (weight >= cs.emotionMemoryThreshold() || emotion.trauma() != null) memory.reinforceByEmotion(npc, mem.id(), weight, emotion.trauma() != null, in.at, traceId);
            var blend = emotions.blend(npc);
            memory.snapshotEmotion(npc, blend.dominant(), blend.intensity() / 100.0D, emotions.mood(npc).name());

            // ---- relationship
            t0 = System.nanoTime();
            Update update = null;
            EntityRef other = p.focus == ExperienceProfile.Focus.TARGET ? in.target : p.focus == ExperienceProfile.Focus.ACTOR ? in.actor : null;
            if (other != null && !other.id().equals(npc) && !p.social.isNone()) {
                Set<String> tags = new java.util.LinkedHashSet<>(p.socialTags);
                tags.addAll(in.tags);
                update = relationships.apply(new SocialEvidence(npc, other, in.kind.name(), p.social, mem.importance().weight(), publicEvent, witnessed, mem.id(), traceId, in.at, place,
                        society.honorScale(npc), p.role, p.loyalty, tags, in.note));
                memory.linkSocial(npc, mem.id(), other.id());
                trace.record(traceId, npc, TraceStage.RELATIONSHIP, other.label() + " trust " + Math.round(update.record().trust()) + " " + update.trustAfter(), in.at);
                relationshipToEmotion(npc, update, other, place, traceId, in.at);
            }
            metrics.relationshipNanos.addAndGet(System.nanoTime() - t0);

            // ---- knowledge
            t0 = System.nanoTime();
            List<LearnResult> learned = new ArrayList<>();
            KnowledgeRecord claimRecord = null;
            for (KnowledgeHint hint : p.knowledge) {
                EntityRef subject = select(hint.subject(), in, self, place), object = select(hint.object(), in, self, place);
                if (subject == null || (hint.object() != Selector.NONE && object == null)) continue;
                if (mem.importance().weight() < cs.extractMinImportance() && !hint.discovery()) continue;
                Map<String, String> attrs = new LinkedHashMap<>();
                if (subject.kind() == EntityKind.PLACE || (object != null && object.kind() == EntityKind.PLACE)) attrs.put("name", in.context.getOrDefault("placeName", place.zone().isEmpty() ? "lugar" : place.zone()));
                if (in.context.containsKey("category")) attrs.put("category", in.context.get("category"));
                KnowledgeCategory category = hint.category() != KnowledgeCategory.GENERAL ? hint.category() : categoryOf(in.context.get("category"));
                KnowledgeEvidence evidence = new KnowledgeEvidence(npc, hint.type(), category, subject, hint.predicate(), object, attrs, hint.method(), null, -1.0D, place, Set.of(in.kind.name().toLowerCase(Locale.ROOT)),
                        mem.id(), traceId, in.at, hint.method().direct() ? 1.0D : 0.9D, Math.max(hint.importance(), mem.importance().weight() * 0.8D), hint.access(), null);
                LearnResult result = hint.discovery() ? knowledge.discover(evidence, p.danger) : knowledge.learn(evidence);
                if (result.rejected()) continue;
                learned.add(result);
                memory.linkKnowledge(npc, mem.id(), result.record().id());
                for (String community : society.communitiesOf(npc)) society.noteKnown(community, result.record(), npc, in.at);
                if (p.rumor != null && hint.predicate() == p.rumor.predicate()) claimRecord = result.record();
            }
            if (!learned.isEmpty()) trace.record(traceId, npc, TraceStage.KNOWLEDGE, learned.size() + " facts", in.at);
            metrics.knowledgeNanos.addAndGet(System.nanoTime() - t0);

            // ---- society: rumour and history
            RumorRecord rumor = null;
            String community = firstCommunity(npc);
            if (p.rumor != null && (publicEvent || witnessed) && in.actor != null && !in.actor.id().equals(npc) && p.rumor.magnitude() * scale >= cs.rumorMinMagnitude()) {
                RumorClaim claim = new RumorClaim(in.actor, p.rumor.predicate(), p.rumor.predicate() == Predicate.IS_HONORABLE ? null : (in.target != null ? in.target : self),
                        p.rumor.label(), Math.min(1.0D, p.rumor.magnitude() * scale), place, in.kind.name());
                rumor = society.createRumor(self, mem.id(), traceId, claim, community, in.at);
                metrics.rumorsStarted.incrementAndGet();
                if (claimRecord != null) { claimRecord.rumorId(rumor.id()); knowledge.runtime(npc).rumors().add(rumor.id()); knowledge.runtime(npc).markDirty(); }
                trace.record(traceId, npc, TraceStage.SOCIETY, "rumor " + rumor.id().toString().substring(0, 8), in.at);
            }
            HistoricalEvent history = null;
            if (p.history != null && (publicEvent || witnessed || p.publicEvent)) {
                List<EntityRef> participants = new ArrayList<>();
                if (in.actor != null) participants.add(in.actor);
                if (in.target != null && !participants.contains(in.target)) participants.add(in.target);
                if (participants.stream().noneMatch(x -> x.id().equals(npc))) participants.add(self);
                history = new HistoricalEvent(UUID.randomUUID(), p.history, in.at, place, participants, Set.of(in.kind.name().toLowerCase(Locale.ROOT)), Math.min(1.0D, mem.importance().weight() * (0.5D + p.magnitude)), in.kind.name(), traceId, community, npc);
                society.record(community, history);
                if (p.history == yadi.samuraiai.ai.knowledge.history.HistoryType.BETRAYAL && in.actor != null) society.adjustStanding(community, in.actor, "TRAITOR", 0.5D);
                if (p.history == yadi.samuraiai.ai.knowledge.history.HistoryType.HERO_ACT) society.adjustStanding(community, self, "PROTECTOR", 0.4D);
            }

            // ---- personality
            evolvePersonality(npc, p, mem, in, scale, traceId);
            return new CognitionOutcome(traceId, experienceId, true, observation, emotion, update, learned, rumor, history);
        } catch (RuntimeException error) {
            metrics.errors.incrementAndGet();
            metrics.lastError = error.toString() + (error.getStackTrace().length > 0 ? " at " + error.getStackTrace()[0] : "");
            return CognitionOutcome.rejected(traceId, experienceId);
        } finally { metrics.experienceNanos.addAndGet(System.nanoTime() - started); }
    }

    private static KnowledgeCategory categoryOf(String name) {
        if (name == null) return KnowledgeCategory.GENERAL;
        try { return KnowledgeCategory.valueOf(name.trim().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { return KnowledgeCategory.GENERAL; }
    }

    private EntityRef ref(UUID id) {
        EntityRef r = facts.refOf(id);
        return r.kind() == EntityKind.UNKNOWN ? new EntityRef(id, EntityKind.NPC, r.name()) : r;
    }

    private String firstCommunity(UUID npc) {
        Set<String> all = society.communitiesOf(npc);
        return all.isEmpty() ? "" : all.iterator().next();
    }

    private double dominantIntensity(UUID npc) { return emotions.blend(npc).intensity() / 100.0D; }

    private Experience buildExperience(UUID id, UUID traceId, ExperienceInput in, ExperienceProfile p, PlaceRef place, boolean witnessed, boolean publicEvent, boolean traumatic, double scale) {
        List<Map.Entry<EmotionKind, Double>> ranked = new ArrayList<>(p.emotions.entrySet());
        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        EmotionKind primary = ranked.isEmpty() ? EmotionKind.CALM : ranked.get(0).getKey();
        List<EmotionKind> secondary = new ArrayList<>();
        for (int i = 1; i < ranked.size() && secondary.size() < 2; i++) secondary.add(ranked.get(i).getKey());
        double intensity = ranked.isEmpty() ? 0.0D : ranked.get(0).getValue() / 100.0D * Math.min(1.5D, scale);
        double valence = 0, total = 0;
        for (var e : ranked) { valence += e.getKey().valence() * e.getValue(); total += e.getValue(); }
        Experience.Builder b = Experience.builder(in.npc, in.kind).id(id).trace(traceId).category(p.category).episode(p.episode).actor(in.actor).target(in.target)
                .place(place).stamp(Stamp.of(in.at, facts.weather())).outcome(p.outcome).emotion(new EmotionalSignature(primary, secondary, intensity, total == 0 ? 0 : valence / total, traumatic))
                .event(in.kind.name()).origin(new Origin(in.source, in.kind.name(), traceId)).magnitude(p.magnitude * scale).danger(p.danger).duration(in.duration)
                .repeatable(p.repeatable).witnessed(witnessed).publicEvent(publicEvent);
        for (EntityRef e : in.participants) b.participant(e);
        in.context.forEach(b::context);
        if (p.landmark != null && !in.context.containsKey("landmark")) b.context("landmark", p.landmark);
        if (p.skill != null && !in.context.containsKey("skill")) { b.context("skill", p.skill); b.context("skillKind", p.skillKind == null ? "ROUTE" : p.skillKind); }
        p.tags.forEach(b::tag);
        in.tags.forEach(b::tag);
        p.impressions.forEach(b::impression);
        return b.build();
    }

    /** Relationship -> emotion: warming into a friendship is felt as joy and hope; falling into suspicion as distrust. */
    private void relationshipToEmotion(UUID npc, Update update, EntityRef other, PlaceRef place, UUID traceId, long at) {
        if (update.stageChanged() && update.stageAfter().ordinal() > update.stageBefore().ordinal()) {
            emotions.trigger(new EmotionTrigger(npc, TriggerSource.RELATIONSHIP, List.of(new EmotionEffect(EmotionKind.JOY, 25), new EmotionEffect(EmotionKind.HOPE, 15)), null, "friendship:" + other.id(), traceId, at, place, other,
                    false, 0.4D, false, "", "friendship " + update.stageAfter()));
            if (update.stageAfter().ordinal() >= FriendshipStage.FRIEND.ordinal()) emotions.recover(npc, RecoverySource.FRIENDSHIP, at);
        }
        if (update.trustAfter() == TrustLevel.SUSPICIOUS && update.trustBefore() != TrustLevel.SUSPICIOUS)
            emotions.trigger(new EmotionTrigger(npc, TriggerSource.RELATIONSHIP, List.of(new EmotionEffect(EmotionKind.DISTRUST, 25)), null, "distrust:" + other.id(), traceId, at, place, other, false, 0.4D, false, "", "trust lost"));
    }

    private void evolvePersonality(UUID npc, ExperienceProfile p, MemoryRecord mem, ExperienceInput in, double scale, UUID traceId) {
        if (p.drift.isEmpty()) return;
        CognitionSettings cs = settings.get();
        double magnitude = p.magnitude * scale * (0.5D + 0.5D * mem.importance().weight());
        if (magnitude < cs.evolutionMinMagnitude()) return;
        PersonalityLedger ledger = ledger(npc);
        long day = in.at / Stamp.TICKS_PER_DAY;
        for (var e : p.drift.entrySet()) {
            double moved = ledger.evolve(e.getKey(), cs.evolutionRate() * e.getValue() * magnitude, cs.evolutionDailyLimit(), cs.evolutionCap(), day);
            if (Math.abs(moved) < 1e-9) continue;
            metrics.evolutions.incrementAndGet();
            publish(new PersonalityEvolvedEvent(npc, traceId, e.getKey().name(), moved, ledger.evolution(e.getKey()), in.kind.name()));
        }
    }

    // ------------------------------------------------------------------ time

    /** One update of an NPC's mind: memory upkeep, relationship cooling, knowledge fading and the emotional tick. Cheap when nothing is due. */
    public EmotionEngine.TickResult tick(UUID npc, long now, Activity activity) {
        long started = System.nanoTime();
        memory.tick(npc, now);
        relationships.tick(npc, now);
        knowledge.tick(npc, now);
        EmotionEngine.TickResult result = emotions.tick(npc, now, activity);
        if (result.previousMood() != null) { var blend = emotions.blend(npc); memory.snapshotEmotion(npc, blend.dominant(), blend.intensity() / 100.0D, emotions.mood(npc).name()); }
        metrics.ticks.incrementAndGet();
        metrics.tickNanos.addAndGet(System.nanoTime() - started);
        return result;
    }

    /**
     * The NPC is at a place with people around: memories tied to them echo, and traumas tied to them flash back. Both are only feelings,
     * flagged as echoes so that they can never become new memories or reinforce the memories that caused them.
     * @return how many echoes were felt
     */
    public int context(UUID npc, RetrievalContext ctx, long now) {
        int count = 0;
        for (var echo : memory.echoes(npc, ctx, now)) {
            emotions.trigger(new EmotionTrigger(npc, TriggerSource.ECHO, List.of(new EmotionEffect(echo.emotion(), echo.intensity() * 100.0D)), echo.memoryId(), "echo:" + echo.memoryId(), null, now, ctx.place(), null,
                    false, 0.5D, true, "", "memory echo"));
            metrics.echoes.incrementAndGet();
            count++;
        }
        for (EmotionTrigger t : emotions.flashbacks(npc, ctx.place(), ctx.nearby(), now)) { emotions.trigger(t); metrics.echoes.incrementAndGet(); count++; }
        return count;
    }

    /** The NPC slept: fresh memories are settled and strengthened, and emotional wounds get their night's recovery. */
    public void sleep(UUID npc, long now) {
        memory.sleep(npc, now);
        emotions.recover(npc, RecoverySource.SLEEP, now);
        emotions.regulate(npc, Technique.SLEEP, now, 1200L);
    }

    /** Society upkeep and the tellings that have arrived. Returns the deliveries that reached a listener, so the adapter can apply their consequences. */
    public List<SocietyEngine.Delivery> society(long now) {
        society.tick(now);
        List<SocietyEngine.Delivery> deliveries = society.deliverDue(now, sourceView);
        for (SocietyEngine.Delivery d : deliveries) if (d.delivered()) onDelivered(d, now);
        return deliveries;
    }

    /** A telling arrived: what the listener now believes becomes reputation (if the claim bears on someone's standing) and counts towards the communities' collective knowledge. */
    private void onDelivered(SocietyEngine.Delivery d, long now) {
        UUID listener = d.task().to();
        KnowledgeRecord record = d.record();
        if (record != null) for (String community : society.communitiesOf(listener)) society.noteKnown(community, record, listener, now);
        RumorRecord rumor = d.rumor();
        if (rumor == null || rumor.claim().label().isEmpty()) return;
        ReputationLabel label;
        try { label = ReputationLabel.valueOf(rumor.claim().label()); } catch (IllegalArgumentException e) { return; }
        relationships.hear(new ReputationHearsay(listener, rumor.claim().subject(), label, rumor.claim().magnitude(), ReputationScope.LOCAL, "", EntityRef.npc(d.task().from(), ""), rumor.id(), false, false, now, rumor.traceId()),
                sourceView.trust(listener, d.task().from()) / 100.0D, sourceView.reputation(listener, d.task().from()), 0.4D);
        trace.record(rumor.traceId(), listener, TraceStage.SOCIETY, "heard rumor from " + d.task().from().toString().substring(0, 8), now);
    }

    // ------------------------------------------------------------------ advice for the Brain

    public CognitiveAdvice advice(UUID npc, RetrievalContext ctx, long now, String period, String zoneKind, double radius) {
        var blend = emotions.blend(npc);
        EmotionRuntime er = emotions.peek(npc).orElse(null);
        List<CognitiveAdvice.Familiar> familiars = new ArrayList<>();
        RelationshipSettings rs = relationships.settings();
        for (UUID id : ctx.nearby()) {
            var found = relationships.find(npc, id);
            if (found.isEmpty()) continue;
            RelationshipRecord r = found.get();
            boolean hostile = r.fear() >= 50 || r.trust() < rs.trustSuspicious() && r.rivalry() >= rs.rivalryGrowing();
            boolean friendly = r.trust() >= rs.trustTrusting() && r.affinity() >= rs.affinityComfortable();
            familiars.add(new CognitiveAdvice.Familiar(id, r.target().label(), r.trust(), r.fear(), r.respect(), r.rivalry(), HonorSystem.category(r, rs), r.stage(), hostile, friendly));
        }
        double danger = 0;
        KnowledgeRuntime kr = knowledge.peek(npc).orElse(null);
        if (kr != null && ctx.place().known()) for (KnowledgeRecord r : PlaceKnowledge.dangerous(kr, settings.get().dangerKnownThreshold())) {
            double d = r.place().distance(ctx.place());
            if (d <= radius) danger = Math.max(danger, PlaceKnowledge.danger(r) * (1.0D - d / radius * 0.5D));
        }
        String ritual = "", ritualZone = "";
        Optional<yadi.samuraiai.ai.knowledge.culture.Tradition> due = society.dueTradition(npc, period == null ? "" : period, zoneKind == null ? "ANY" : zoneKind);
        if (due.isPresent()) { ritual = due.get().name(); ritualZone = due.get().zoneKind(); }
        List<String> mem = new ArrayList<>();
        for (RetrievalResult r : memory.relevant(npc, ctx, 3, now)) mem.add(r.record().summary());
        return new CognitiveAdvice(npc, now, emotions.mood(npc), blend.dominant(), blend.intensity(), blend.label(), emotions.intensity(npc, EmotionKind.FEAR), emotions.intensity(npc, EmotionKind.ANGER),
                emotions.intensity(npc, EmotionKind.SADNESS), emotions.intensity(npc, EmotionKind.JOY), emotions.intensity(npc, EmotionKind.CALM), er == null ? Technique.NONE : er.advice().technique(),
                emotions.expression(npc), emotions.physiology(npc).speedScale(), familiars, danger, ritual, ritualZone, mem);
    }

    // ------------------------------------------------------------------ lifecycle and persistence

    public boolean isLoaded(UUID npc) { return loaded.contains(npc); }
    public int loadedCount() { return loaded.size(); }

    /** Brings an NPC's whole mind into memory: from disk when it has saved before. Safe to call repeatedly. */
    public void ensureLoaded(UUID npc, long now) {
        if (!loaded.add(npc)) return;
        if (storage != null) storage.loadNpc(this, npc, now);
        ledger(npc);
    }

    /** Saves and unloads an NPC (it became inactive, or the server is stopping). */
    public void unload(UUID npc, long now) {
        if (!loaded.remove(npc)) return;
        if (storage != null) storage.saveNpc(this, npc, now, false);
        memory.unload(npc); relationships.unload(npc); emotions.unload(npc); knowledge.unload(npc); ledgers.remove(npc);
    }

    /** The NPC is gone for good: its whole mind (and everyone's ties to it) is erased. */
    public void erase(UUID npc) {
        loaded.remove(npc);
        memory.unload(npc); relationships.forgetNpc(npc); emotions.unload(npc); knowledge.unload(npc); ledgers.remove(npc); society.forgetNpc(npc);
        if (storage != null) storage.deleteNpc(npc);
    }

    /** Periodic, budgeted write of what changed: important changes first, at most {@code maxFiles} files per call. Returns the files written. */
    public int saveDirty(long now, boolean force) {
        CognitionSettings cs = settings.get();
        if (storage == null) return 0;
        if (!force && now - lastSaveSweep < cs.saveIntervalTicks()) {
            // Between sweeps only the urgent (trauma, betrayal, critical memory...) are written.
            int written = 0;
            for (UUID npc : new ArrayList<>(loaded)) { if (written >= cs.maxSavesPerTick()) break; if (isUrgent(npc)) written += storage.saveNpc(this, npc, now, true); }
            return written;
        }
        lastSaveSweep = now;
        int written = 0;
        List<UUID> order = new ArrayList<>(loaded);
        order.sort(Comparator.comparing((UUID id) -> !isUrgent(id)));
        for (UUID npc : order) { if (!force && written >= cs.maxSavesPerTick()) break; written += storage.saveNpc(this, npc, now, true); }
        if (society.communityCount() > 0 || !society.rumors().isEmpty()) storage.saveSociety(this);
        return written;
    }

    private boolean isUrgent(UUID npc) {
        return memory.peek(npc).map(r -> r.urgent()).orElse(false) || relationships.peek(npc).map(r -> r.urgent()).orElse(false)
                || emotions.peek(npc).map(r -> r.urgent()).orElse(false) || knowledge.peek(npc).map(r -> r.urgent()).orElse(false);
    }

    /** Saves everything now (server stopping). */
    public int saveAll(long now) {
        if (storage == null) return 0;
        int written = 0;
        for (UUID npc : new ArrayList<>(loaded)) written += storage.saveNpc(this, npc, now, false);
        storage.saveSociety(this);
        return written;
    }

    public void reset() {
        memory.reset(); relationships.reset(); emotions.reset(); knowledge.reset(); society.reset(); ledgers.clear(); loaded.clear(); trace.clear(); lastSaveSweep = Long.MIN_VALUE / 2;
    }

    public List<UUID> loadedNpcs() { return new ArrayList<>(loaded); }
    public Map<UUID, PersonalityLedger> ledgers() { return ledgers; }
}
