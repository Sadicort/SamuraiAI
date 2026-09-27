package yadi.samuraiai.ai.knowledge.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.PersonalityView;
import yadi.samuraiai.ai.knowledge.discovery.DiscoveryEngine;
import yadi.samuraiai.ai.knowledge.encyclopedia.Encyclopedia;
import yadi.samuraiai.ai.knowledge.events.DiscoveryEvent;
import yadi.samuraiai.ai.knowledge.events.KnowledgeCreatedEvent;
import yadi.samuraiai.ai.knowledge.events.KnowledgeLearnedEvent;
import yadi.samuraiai.ai.knowledge.events.KnowledgeTaughtEvent;
import yadi.samuraiai.ai.knowledge.events.KnowledgeUpdatedEvent;
import yadi.samuraiai.ai.knowledge.events.KnowledgeValidatedEvent;
import yadi.samuraiai.ai.knowledge.graph.KnowledgeGraph;
import yadi.samuraiai.ai.knowledge.learning.LearningEngine;
import yadi.samuraiai.ai.knowledge.metrics.KnowledgeMetrics;
import yadi.samuraiai.ai.knowledge.model.AccessLevel;
import yadi.samuraiai.ai.knowledge.model.KnowledgeCategory;
import yadi.samuraiai.ai.knowledge.model.KnowledgeEvidence;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;
import yadi.samuraiai.ai.knowledge.model.LearnMethod;
import yadi.samuraiai.ai.knowledge.model.Predicate;
import yadi.samuraiai.ai.knowledge.model.Revision;
import yadi.samuraiai.ai.knowledge.model.ValidationEvidence;
import yadi.samuraiai.ai.knowledge.model.ValidationState;
import yadi.samuraiai.ai.knowledge.teaching.TeachingEngine;
import yadi.samuraiai.ai.knowledge.teaching.TeachingResult;
import yadi.samuraiai.ai.knowledge.validation.ValidationEngine;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.event.NpcEvent;

/**
 * What NPCs believe about the world. It owns one {@link KnowledgeRuntime} per NPC (communities keep theirs in the society
 * engine and use the same learning routine) and coordinates learning, validation, teaching and discovery. Beliefs come from
 * evidence, keep their source, can be wrong, are validated rather than assumed, fade when unused and are versioned. A rumour
 * is never a verified fact merely because it was repeated.
 */
public final class KnowledgeEngine {
    private final Supplier<KnowledgeSettings> settings;
    private EventSink events;
    private final Map<UUID, KnowledgeRuntime> runtimes = new HashMap<>();
    private final LearningEngine learning = new LearningEngine();
    private final ValidationEngine validation = new ValidationEngine();
    private final TeachingEngine teaching = new TeachingEngine();
    private final DiscoveryEngine discovery = new DiscoveryEngine();
    private final KnowledgeMetrics metrics = new KnowledgeMetrics();
    private Function<UUID, PersonalityView> personalities = id -> PersonalityView.NEUTRAL;
    private int cellSize = 32;

    public KnowledgeEngine(Supplier<KnowledgeSettings> settings, EventSink events) {
        this.settings = Objects.requireNonNull(settings);
        this.events = Objects.requireNonNull(events);
    }

    public KnowledgeSettings settings() { return settings.get(); }
    public KnowledgeMetrics metrics() { return metrics; }
    public ValidationEngine validation() { return validation; }
    public LearningEngine learning() { return learning; }
    public void useEventSink(EventSink sink) { this.events = Objects.requireNonNull(sink); }
    public void usePersonalities(Function<UUID, PersonalityView> source) { this.personalities = Objects.requireNonNull(source); }
    public void cellSize(int size) { this.cellSize = size; }
    public int cellSize() { return cellSize; }
    void publish(NpcEvent event) { events.publish(event); }

    // ------------------------------------------------------------------ runtimes

    public KnowledgeRuntime runtime(UUID npc) { return runtimes.computeIfAbsent(npc, id -> new KnowledgeRuntime(id, cellSize)); }
    public Optional<KnowledgeRuntime> peek(UUID npc) { return Optional.ofNullable(runtimes.get(npc)); }
    public void install(KnowledgeRuntime runtime) { runtimes.put(runtime.ownerId(), runtime); }
    public void unload(UUID npc) { runtimes.remove(npc); }
    public void reset() { runtimes.clear(); }
    public List<UUID> tracked() { return new ArrayList<>(runtimes.keySet()); }
    public int totalRecords() { int n = 0; for (KnowledgeRuntime rt : runtimes.values()) n += rt.size(); return n; }

    // ------------------------------------------------------------------ learning

    public LearnResult learn(KnowledgeEvidence e) { return learnInto(runtime(e.npc()), e, personalities.apply(e.npc())); }

    /** The shared learning routine (used for NPCs and for communities' collective knowledge). */
    public LearnResult learnInto(KnowledgeRuntime rt, KnowledgeEvidence e, PersonalityView p) {
        long started = System.nanoTime();
        KnowledgeSettings s = settings.get();
        try {
            if (e.subject() == null || e.predicate() == null || e.type() == null) return LearnResult.rejected("incomplete evidence");
            String key = KnowledgeRecord.key(e.type(), e.subject().id(), e.predicate(), e.object() == null ? null : e.object().id());
            KnowledgeRecord existing = rt.byKey(key);
            double confidence = learning.confidence(e, p, s);
            if (existing == null) return create(rt, e, key, confidence, s);
            return update(rt, existing, e, confidence, s);
        } catch (RuntimeException error) {
            metrics.errors.incrementAndGet();
            return LearnResult.rejected(error.toString());
        } finally { metrics.learnNanos.addAndGet(System.nanoTime() - started); }
    }

    private LearnResult create(KnowledgeRuntime rt, KnowledgeEvidence e, String key, double confidence, KnowledgeSettings s) {
        if (e.predicate().singleValued()) supersede(rt, e, s);
        if (rt.size() >= s.maxKnowledge()) makeRoom(rt);
        KnowledgeRecord r = new KnowledgeRecord(UUID.randomUUID(), rt.ownerId(), e.type(), e.category(), e.subject(), e.predicate(), e.object(), e.method(), confidence, e.at());
        r.attributes().putAll(e.attributes());
        r.source(e.source());
        r.place(e.place());
        r.tags().addAll(e.tags());
        r.importance(e.importance());
        r.access(e.access());
        r.rumorId(e.rumorId());
        r.traceId(e.traceId());
        if (e.memoryId() != null) r.memoryLinks().add(e.memoryId());
        if (e.source() != null) r.supporters().add(e.source().id());
        r.state(validation.initial(r, s));
        rt.add(r);
        if (e.rumorId() != null) rt.rumors().add(e.rumorId());
        if (r.importance() >= 0.7D || r.state() == ValidationState.VERIFIED && r.type() == KnowledgeType.DANGER) rt.markUrgent();
        metrics.created.incrementAndGet(); metrics.learned.incrementAndGet();
        publish(new KnowledgeCreatedEvent(rt.ownerId(), e.traceId(), r.id(), r.type().name(), r.predicate().name(), r.subject().label(), r.state().name(), r.confidence()));
        publish(new KnowledgeLearnedEvent(rt.ownerId(), e.traceId(), r.id(), e.method().name(), e.source() == null ? null : e.source().id()));
        return new LearnResult(r, true, ValidationState.UNKNOWN, r.state(), false, "");
    }

    private LearnResult update(KnowledgeRuntime rt, KnowledgeRecord r, KnowledgeEvidence e, double confidence, KnowledgeSettings s) {
        ValidationState before = r.state();
        double oldConfidence = r.confidence();
        ValidationEngine.Outcome[] outcome = new ValidationEngine.Outcome[1];
        rt.reindex(r, () -> {
            r.attributes().putAll(e.attributes());
            if (e.place().known() && (e.method().direct() || !r.place().known())) r.place(e.place());
            r.tags().addAll(e.tags());
            r.importance(Math.max(r.importance(), e.importance()));
            if (e.memoryId() != null) r.memoryLinks().add(e.memoryId());
            if (e.rumorId() != null && r.rumorId() == null) r.rumorId(e.rumorId());
            if (before == ValidationState.FORGOTTEN) r.state(ValidationState.UNKNOWN);
            outcome[0] = validation.apply(r, evidenceFor(e, confidence, s), s);
        });
        if (e.rumorId() != null) rt.rumors().add(e.rumorId());
        rt.markDirty();
        metrics.updated.incrementAndGet();
        if (outcome[0].changed()) publish(new KnowledgeUpdatedEvent(rt.ownerId(), e.traceId(), r.id(), e.method().name(), oldConfidence, r.confidence()));
        if (outcome[0].before() != outcome[0].after()) { metrics.validated.incrementAndGet(); publish(new KnowledgeValidatedEvent(rt.ownerId(), e.traceId(), r.id(), before.name(), r.state().name(), e.method().name())); }
        return new LearnResult(r, false, before, r.state(), false, "");
    }

    private static ValidationEvidence evidenceFor(KnowledgeEvidence e, double confidence, KnowledgeSettings s) {
        UUID source = e.source() == null ? null : e.source().id();
        if ("true".equals(e.attributes().get("contradicts")))
            return new ValidationEvidence(e.method().direct() ? ValidationEvidence.Kind.DIRECT_CONTRADICTS : ValidationEvidence.Kind.CONTRADICTING_SOURCE, source, confidence, e.at(), "contradiction");
        return switch (e.method()) {
            case OBSERVATION, EXPERIENCE, ADMIN -> new ValidationEvidence(ValidationEvidence.Kind.DIRECT_CONFIRMS, source, confidence, e.at(), "observed");
            case PUBLIC_EVENT -> new ValidationEvidence(ValidationEvidence.Kind.PUBLIC_EVENT, source, confidence, e.at(), "public event");
            case TEACHING -> new ValidationEvidence(confidence >= s.trustedSource() ? ValidationEvidence.Kind.TRUSTED_SOURCE : ValidationEvidence.Kind.INDEPENDENT_SOURCE, source, confidence, e.at(), "taught");
            default -> new ValidationEvidence(ValidationEvidence.Kind.INDEPENDENT_SOURCE, source, confidence, e.at(), e.method().name().toLowerCase());
        };
    }

    /** A new value for a one-value predicate (where someone lives, who leads) makes the old belief less certain, unless the old one was never firmly held either. */
    private void supersede(KnowledgeRuntime rt, KnowledgeEvidence e, KnowledgeSettings s) {
        for (UUID id : new ArrayList<>(rt.aboutSubject(e.subject().id()))) {
            KnowledgeRecord old = rt.get(id);
            if (old == null || old.predicate() != e.predicate() || old.type() != e.type()) continue;
            if (old.object() != null && e.object() != null && old.object().id().equals(e.object().id())) continue;
            rt.reindex(old, () -> {
                double before = old.confidence();
                ValidationState was = old.state();
                old.confidence(old.confidence() * 0.4D);
                if (old.state() == ValidationState.VERIFIED) old.state(validation.classify(old, ValidationState.LIKELY, false, s));
                else old.state(validation.classify(old, old.state(), false, s));
                old.revisions().add(new Revision(e.at(), before, old.confidence(), was, old.state(), "superseded by " + (e.object() == null ? "?" : e.object().label())));
                while (old.revisions().size() > s.maxRevisions()) old.revisions().remove(0);
                old.bump();
            });
        }
    }

    private void makeRoom(KnowledgeRuntime rt) {
        KnowledgeRecord weakest = null;
        double lowest = Double.MAX_VALUE;
        for (KnowledgeRecord r : rt.all()) {
            if (r.state() == ValidationState.VERIFIED && r.importance() >= 0.6D) continue;
            double value = r.confidence() * (0.3D + r.importance()) + Math.min(r.uses(), 10) * 0.02D;
            if (value < lowest) { lowest = value; weakest = r; }
        }
        if (weakest != null) forget(rt, weakest);
    }

    private void forget(KnowledgeRuntime rt, KnowledgeRecord r) {
        r.state(ValidationState.FORGOTTEN);
        rt.remove(r.id());
        metrics.forgotten.incrementAndGet();
    }

    public boolean forget(UUID npc, UUID knowledgeId) {
        KnowledgeRuntime rt = runtimes.get(npc);
        KnowledgeRecord r = rt == null ? null : rt.get(knowledgeId);
        if (r == null) return false;
        forget(rt, r);
        return true;
    }

    // ------------------------------------------------------------------ validation

    public Optional<ValidationEngine.Outcome> validate(UUID holder, UUID recordId, ValidationEvidence ev) {
        KnowledgeRuntime rt = runtimes.get(holder);
        return rt == null ? Optional.empty() : validateIn(rt, recordId, ev);
    }

    public Optional<ValidationEngine.Outcome> validateIn(KnowledgeRuntime rt, UUID recordId, ValidationEvidence ev) {
        KnowledgeRecord r = rt.get(recordId);
        if (r == null) return Optional.empty();
        ValidationEngine.Outcome[] outcome = new ValidationEngine.Outcome[1];
        rt.reindex(r, () -> outcome[0] = validation.apply(r, ev, settings.get()));
        if (outcome[0].changed()) {
            metrics.validated.incrementAndGet();
            publish(new KnowledgeValidatedEvent(rt.ownerId(), null, r.id(), outcome[0].before().name(), outcome[0].after().name(), ev.kind().name()));
            rt.markDirty();
        }
        return Optional.of(outcome[0]);
    }

    // ------------------------------------------------------------------ teaching and discovery

    /**
     * A lesson: the teacher passes topics to the student. The quality depends on the student's respect for and trust in the
     * teacher, how well the teacher knows the topic and how attentively the student listens; below the minimum nothing is
     * learned. What is taught is limited by the access rank the student holds ({@code access} answers whether the student may know a level).
     */
    public TeachingResult teach(UUID teacher, UUID student, List<UUID> topics, double respect01, double trust01, double attention, BiPredicate<UUID, AccessLevel> access, long now, UUID trace) {
        KnowledgeSettings s = settings.get();
        KnowledgeRuntime teacherRt = runtime(teacher);
        List<TeachingResult.Topic> outcomes = new ArrayList<>();
        double lastQuality = 0;
        PersonalityView studentPersonality = personalities.apply(student);
        for (UUID topic : topics) {
            KnowledgeRecord r = teacherRt.get(topic);
            if (r == null || r.state() == ValidationState.FALSE || r.state() == ValidationState.FORGOTTEN) { outcomes.add(new TeachingResult.Topic(topic, false, "unknown to the teacher")); continue; }
            if (access != null && !access.test(student, r.access())) { outcomes.add(new TeachingResult.Topic(topic, false, "restricted")); continue; }
            double quality = teaching.quality(respect01, trust01, r.confidence(), attention, s);
            lastQuality = quality;
            if (!teaching.succeeds(quality, s)) {
                metrics.teachFailed.incrementAndGet();
                publish(new KnowledgeTaughtEvent(teacher, trace, student, r.id(), quality, false));
                outcomes.add(new TeachingResult.Topic(topic, false, "quality too low"));
                continue;
            }
            KnowledgeEvidence evidence = new KnowledgeEvidence(student, r.type(), r.category(), r.subject(), r.predicate(), r.object(), r.attributes(), LearnMethod.TEACHING,
                    EntityRef.npc(teacher, ""), r.confidence() * quality * s.teachConfidenceScale(), r.place(), r.tags(), null, trace, now, attention, r.importance(), r.access(), r.rumorId());
            LearnResult result = learnInto(runtime(student), evidence, studentPersonality);
            boolean learned = !result.rejected();
            if (learned) metrics.taught.incrementAndGet();
            publish(new KnowledgeTaughtEvent(teacher, trace, student, r.id(), quality, learned));
            outcomes.add(new TeachingResult.Topic(topic, learned, learned ? "" : result.note()));
        }
        return new TeachingResult(teacher, student, lastQuality, outcomes);
    }

    /**
     * The NPC found something: it is filed as knowledge only if it seems important enough; how important is judged from what it is,
     * whether it is new to the NPC and how dangerous it looks.
     */
    public LearnResult discover(KnowledgeEvidence base, double danger) {
        KnowledgeSettings s = settings.get();
        KnowledgeRuntime rt = runtime(base.npc());
        String key = KnowledgeRecord.key(base.type(), base.subject().id(), base.predicate(), base.object() == null ? null : base.object().id());
        boolean novel = rt.byKey(key) == null;
        double importance = discovery.importance(base.type(), base.category(), novel, danger, s);
        if (!discovery.worthRecording(importance, s)) return LearnResult.rejected("not important enough");
        Map<String, String> attrs = new java.util.LinkedHashMap<>(base.attributes());
        attrs.put("discovery", "true");
        if (danger > 0) attrs.put("danger", String.format(java.util.Locale.ROOT, "%.2f", danger));
        KnowledgeEvidence evidence = new KnowledgeEvidence(base.npc(), base.type(), base.category(), base.subject(), base.predicate(), base.object(), attrs, base.method(), base.source(), base.confidence(),
                base.place(), base.tags(), base.memoryId(), base.traceId(), base.at(), base.attention(), importance, base.access(), base.rumorId());
        LearnResult result = learnInto(rt, evidence, personalities.apply(base.npc()));
        if (!result.rejected() && result.created()) {
            metrics.discoveries.incrementAndGet();
            publish(new DiscoveryEvent(base.npc(), base.traceId(), result.record().id(), base.category().name(), base.subject().label(), importance));
        }
        return result;
    }

    // ------------------------------------------------------------------ upkeep and queries

    /** Budgeted fading of unused beliefs: rumours fade quickly, verified facts slowly, well-used and important ones slower still. */
    public int tick(UUID npc, long now) {
        KnowledgeRuntime rt = runtimes.get(npc);
        if (rt == null) return 0;
        KnowledgeSettings s = settings.get();
        if (now - rt.lastDecay < s.decayIntervalTicks()) return 0;
        rt.lastDecay = now;
        List<UUID> ids = rt.ids();
        if (ids.isEmpty()) return 0;
        int forgotten = 0, batch = Math.min(s.decayBatch(), ids.size());
        for (int i = 0; i < batch; i++) {
            rt.cursor = (rt.cursor + 1) % ids.size();
            KnowledgeRecord r = rt.get(ids.get(rt.cursor));
            if (r == null) continue;
            long elapsed = now - r.lastDecay();
            if (elapsed <= 0) continue;
            r.lastDecay(now);
            double halfLife = r.state() == ValidationState.RUMOR ? s.rumorHalfLifeTicks() : s.halfLifeTicks() * (r.state() == ValidationState.VERIFIED ? s.verifiedDecayScale() : 1.0D);
            halfLife *= (1.0D + Math.min(r.uses(), 10) * 0.1D) * (1.0D + r.importance() * 2.0D);
            r.confidence(r.confidence() * Math.pow(0.5D, elapsed / halfLife));
            boolean gone = r.confidence() < s.forgetBelow() || (r.state() == ValidationState.FALSE && r.confidence() < 0.3D);
            if (gone) { forget(rt, r); forgotten++; }
        }
        if (forgotten > 0) rt.markDirty();
        return forgotten;
    }

    /** The belief about (subject, predicate[, object]) if the NPC holds one, at whatever confidence; using it counts as use. */
    public Optional<KnowledgeRecord> believes(UUID npc, KnowledgeType type, UUID subject, Predicate predicate, UUID object, long now) {
        long started = System.nanoTime();
        try {
            KnowledgeRuntime rt = runtimes.get(npc);
            if (rt == null) return Optional.empty();
            KnowledgeRecord r = rt.byKey(KnowledgeRecord.key(type, subject, predicate, object));
            if (r != null) r.used(now);
            return Optional.ofNullable(r);
        } finally { metrics.queryNanos.addAndGet(System.nanoTime() - started); metrics.queries.incrementAndGet(); }
    }

    public List<KnowledgeRecord> about(UUID npc, UUID subject) {
        KnowledgeRuntime rt = runtimes.get(npc);
        List<KnowledgeRecord> result = new ArrayList<>();
        if (rt != null) for (UUID id : rt.aboutSubject(subject)) { KnowledgeRecord r = rt.get(id); if (r != null && r.alive()) result.add(r); }
        return result;
    }

    public KnowledgeGraph graph(UUID npc) { return new KnowledgeGraph(runtime(npc), settings.get().queryLimit()); }
    public Encyclopedia encyclopedia(UUID npc) { return new Encyclopedia(runtime(npc)); }
    public void link(UUID npc, UUID knowledgeId, UUID memoryId) {
        KnowledgeRuntime rt = runtimes.get(npc);
        KnowledgeRecord r = rt == null ? null : rt.get(knowledgeId);
        if (r != null && memoryId != null && r.memoryLinks().add(memoryId)) rt.markDirty();
    }

    /** Why does the NPC believe this? Its state, confidence, source, evidence and revision history. */
    public List<String> explain(UUID npc, UUID knowledgeId) {
        KnowledgeRuntime rt = runtimes.get(npc);
        KnowledgeRecord r = rt == null ? null : rt.get(knowledgeId);
        if (r == null) return List.of("no hay ese conocimiento");
        List<String> lines = new ArrayList<>();
        lines.add(r.summary() + " aprendido por " + r.origin() + (r.source() == null ? "" : " de " + r.source().label()));
        lines.add("  evidencia: directa=" + r.directEvidence() + " pública=" + r.publicEvidence() + " fuente fiable=" + r.trustedEvidence() + " fuentes independientes=" + r.supporters().size() + " contradicciones=" + r.contradictors().size());
        for (Revision v : r.revisions()) lines.add("  - t=" + v.at() + " " + v.oldState() + "->" + v.newState() + " " + Math.round(v.oldConfidence() * 100) + "%->" + Math.round(v.newConfidence() * 100) + "% (" + v.reason() + ")");
        return lines;
    }

    public KnowledgeCategory categoryOf(String name) {
        try { return KnowledgeCategory.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT)); } catch (RuntimeException e) { return KnowledgeCategory.GENERAL; }
    }
}
