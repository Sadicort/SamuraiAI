package yadi.samuraiai.ai.memory.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.PersonalityView;
import yadi.samuraiai.ai.memory.compression.CompressionEngine;
import yadi.samuraiai.ai.memory.consolidation.ConsolidationEngine;
import yadi.samuraiai.ai.memory.consolidation.MemoryMerger;
import yadi.samuraiai.ai.memory.events.EmotionMemoryActivatedEvent;
import yadi.samuraiai.ai.memory.events.MemoryCompressedEvent;
import yadi.samuraiai.ai.memory.events.MemoryConsolidatedEvent;
import yadi.samuraiai.ai.memory.events.MemoryCreatedEvent;
import yadi.samuraiai.ai.memory.events.MemoryForgottenEvent;
import yadi.samuraiai.ai.memory.events.MemoryMergedEvent;
import yadi.samuraiai.ai.memory.events.MemoryRetrievedEvent;
import yadi.samuraiai.ai.memory.events.MemoryUpdatedEvent;
import yadi.samuraiai.ai.memory.evolution.MemoryEvolutionEngine;
import yadi.samuraiai.ai.memory.forgetting.ForgettingEngine;
import yadi.samuraiai.ai.memory.metrics.MemoryMetrics;
import yadi.samuraiai.ai.memory.model.Experience;
import yadi.samuraiai.ai.memory.model.Importance;
import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.memory.model.MemoryState;
import yadi.samuraiai.ai.memory.model.MemoryType;
import yadi.samuraiai.ai.memory.pipeline.Evaluation;
import yadi.samuraiai.ai.memory.pipeline.EvaluationContext;
import yadi.samuraiai.ai.memory.pipeline.MemoryClassifier;
import yadi.samuraiai.ai.memory.pipeline.MemoryEvaluator;
import yadi.samuraiai.ai.memory.pipeline.ProtectionPolicy;
import yadi.samuraiai.ai.memory.procedural.SkillKind;
import yadi.samuraiai.ai.memory.retrieval.ContextRetrieval;
import yadi.samuraiai.ai.memory.retrieval.RetrievalContext;
import yadi.samuraiai.ai.memory.retrieval.RetrievalEngine;
import yadi.samuraiai.ai.memory.retrieval.RetrievalQuery;
import yadi.samuraiai.ai.memory.retrieval.RetrievalResult;
import yadi.samuraiai.ai.memory.retrieval.SimilarityEngine;
import yadi.samuraiai.ai.memory.retrieval.TagSimilarityEngine;
import yadi.samuraiai.ai.memory.spatial.LandmarkKind;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.event.NpcEvent;

/**
 * The central memory manager. It owns one {@link MemoryRuntime} per NPC and coordinates the specialised services (evaluation,
 * classification, consolidation, forgetting, evolution, compression, indexing, retrieval, cache) without doing their work
 * itself. Experiences enter through {@link #observe}; everything else is maintenance driven by {@link #tick}, sleep and retrieval.
 * Storage is deliberately elsewhere ({@code MemoryStorage}): the engine never touches a file.
 */
public final class MemoryEngine {
    public record MaintenanceReport(int promoted, int merged, int forgotten, int compressedGroups, int detailReduced) {
        public boolean idle() { return promoted + merged + forgotten + compressedGroups + detailReduced == 0; }
    }

    private final Supplier<MemorySettings> settings;
    private EventSink events;
    private final Map<UUID, MemoryRuntime> runtimes = new HashMap<>();
    private final MemoryEvaluator evaluator = new MemoryEvaluator();
    private final MemoryClassifier classifier = new MemoryClassifier();
    private final ProtectionPolicy protection = new ProtectionPolicy();
    private final SimilarityEngine similarity = new TagSimilarityEngine();
    private final RetrievalEngine retrieval = new RetrievalEngine(similarity);
    private final ContextRetrieval contextRetrieval = new ContextRetrieval();
    private final MemoryMerger merger = new MemoryMerger();
    private final ConsolidationEngine consolidation = new ConsolidationEngine(similarity, merger);
    private final ForgettingEngine forgetting = new ForgettingEngine();
    private final MemoryEvolutionEngine evolution = new MemoryEvolutionEngine();
    private final CompressionEngine compression = new CompressionEngine();
    private final MemoryMetrics metrics = new MemoryMetrics();
    private Function<UUID, PersonalityView> personalities = id -> PersonalityView.NEUTRAL;

    public MemoryEngine(Supplier<MemorySettings> settings, EventSink events) {
        this.settings = Objects.requireNonNull(settings);
        this.events = Objects.requireNonNull(events);
    }

    public MemorySettings settings() { return settings.get(); }
    public MemoryMetrics metrics() { return metrics; }
    public ForgettingEngine forgetting() { return forgetting; }
    public SimilarityEngine similarity() { return similarity; }
    public void useEventSink(EventSink sink) { this.events = Objects.requireNonNull(sink); }
    public void usePersonalities(Function<UUID, PersonalityView> source) { this.personalities = Objects.requireNonNull(source); }

    // ------------------------------------------------------------------ runtimes

    public MemoryRuntime runtime(UUID npc) { return runtimes.computeIfAbsent(npc, id -> new MemoryRuntime(id, settings.get())); }
    public Optional<MemoryRuntime> peek(UUID npc) { return Optional.ofNullable(runtimes.get(npc)); }
    public void install(MemoryRuntime runtime) {
        if (runtime.index().cellSize() != settings.get().cellSize()) runtime.rebuildIndex(settings.get().cellSize());
        runtimes.put(runtime.npcId(), runtime);
    }
    public List<UUID> tracked() { return new ArrayList<>(runtimes.keySet()); }
    public void unload(UUID npc) { runtimes.remove(npc); }
    public void reset() { runtimes.clear(); }
    public int totalMemories() { int n = 0; for (MemoryRuntime r : runtimes.values()) n += r.size(); return n; }

    private void publish(NpcEvent event) { events.publish(event); }

    // ------------------------------------------------------------------ intake

    /** The pipeline: evaluate, then either reinforce an existing memory, create a temporary one, or let the experience go. */
    public Observation observe(Experience e, EvaluationContext context, long now) {
        long started = System.nanoTime();
        MemorySettings s = settings.get();
        MemoryRuntime rt = runtime(e.npcId());
        try {
            Evaluation evaluation = evaluator.evaluate(e, rt, context == null ? EvaluationContext.none() : context, s);
            if (!evaluation.keep()) { metrics.discarded.incrementAndGet(); return new Observation(Observation.Result.DISCARDED, null, evaluation); }
            if (e.repeatable()) {
                MemoryRecord repeat = findRepeat(rt, e, now, s);
                if (repeat != null) { reinforce(rt, repeat, e, evaluation, now, s); return new Observation(Observation.Result.REINFORCED, repeat, evaluation); }
            }
            MemoryRecord record = create(rt, e, evaluation, now, s);
            return new Observation(Observation.Result.CREATED, record, evaluation);
        } catch (RuntimeException error) {
            metrics.errors.incrementAndGet();
            metrics.lastError = error + (error.getStackTrace().length > 0 ? " at " + error.getStackTrace()[0] : "");
            return new Observation(Observation.Result.DISCARDED, null, new Evaluation(0, Importance.TRIVIAL, false, Map.of("error", 1.0D)));
        } finally { metrics.observeNanos.addAndGet(System.nanoTime() - started); }
    }

    private MemoryRecord create(MemoryRuntime rt, Experience e, Evaluation evaluation, long now, MemorySettings s) {
        MemoryType type = classifier.classify(e, s);
        MemoryRecord r = new MemoryRecord(e.id(), e.npcId(), type, e.category(), e.episode(), e.kind(), e.stamp(), e.place(), e.actor(), e.target(), e.emotion(), evaluation.importance(), e.origin());
        e.participants().forEach(p -> { if (r.entities().size() < 16) r.entities().add(p); });
        r.tags().addAll(e.tags());
        r.context().putAll(e.context());
        for (String ev : e.events()) if (r.events().size() < s.maxEventsPerRecord()) r.events().add(ev);
        for (var c : e.consequences()) if (r.consequences().size() < s.maxConsequences()) r.consequences().add(c);
        r.impressions().addAll(e.impressions());
        r.duration(e.duration());
        r.witnessed(e.witnessed());
        r.emotionalWeight(e.emotion().intensity());
        r.protect(protection.protects(r, s));
        if (r.isProtected()) r.tags().add("protected");
        rt.add(r);
        // Places and procedures the experience defines become part of the NPC's mental map and habits.
        String landmark = e.context().get("landmark");
        if (landmark != null && e.place().known())
            rt.spatial().visit(LandmarkKind.parse(landmark), e.context().getOrDefault("placeName", e.place().zone()), e.place(), now, s.nodeMergeRadius(), s.nodeConnectRadius(), 0.2D);
        if (e.place().known() && (e.danger() > 0 || e.outcome() == yadi.samuraiai.ai.memory.model.Outcome.NEGATIVE && e.emotion().intensity() > 0.5D))
            rt.spatial().impress(e.place(), s.nodeMergeRadius() * 2, Math.max(e.danger(), e.emotion().intensity() * 0.5D), 0.0D);
        else if (e.place().known() && e.outcome() == yadi.samuraiai.ai.memory.model.Outcome.POSITIVE)
            rt.spatial().impress(e.place(), s.nodeMergeRadius() * 2, 0.0D, 0.1D + e.emotion().intensity() * 0.2D);
        String skill = e.context().get("skill");
        if (skill != null) {
            SkillKind kind; try { kind = SkillKind.valueOf(e.context().getOrDefault("skillKind", "ROUTE")); } catch (IllegalArgumentException x) { kind = SkillKind.ROUTE; }
            rt.procedural().practice(skill, kind, e.place(), now, s.practiceGain(), s.protectSkillUses());
        }
        metrics.created.incrementAndGet();
        publish(new MemoryCreatedEvent(rt.npcId(), e.traceId(), r.id(), r.kind().name(), r.importance().name(), r.type().name()));
        for (MemoryRecord changed : evolution.reinterpret(rt, r, s)) {
            metrics.reinterpreted.incrementAndGet();
            publish(new MemoryUpdatedEvent(rt.npcId(), e.traceId(), changed.id(), "reinterpreted"));
        }
        if (r.importance().atLeast(Importance.CRITICAL) || r.emotion().traumatic()) rt.markUrgent();
        if (rt.size() > s.maxMemories()) enforceCapacity(rt, s);
        return r;
    }

    private MemoryRecord findRepeat(MemoryRuntime rt, Experience e, long now, MemorySettings s) {
        if (s.reinforceWindowTicks() <= 0) return null;
        var pool = e.actor() != null ? rt.index().entity(e.actor().id()) : rt.index().kind(e.kind());
        MemoryRecord best = null;
        double bestSim = s.reinforceSimilarity();
        int checked = 0;
        for (UUID id : pool) {
            if (checked++ >= 200) break;
            MemoryRecord c = rt.get(id);
            if (c == null || c.kind() != e.kind() || c.state() == MemoryState.FORGOTTEN || c.state() == MemoryState.COMPRESSED) continue;
            if (now - c.endTime() > s.reinforceWindowTicks()) continue;
            if (!sameEntity(c.target(), e.target()) || !sameEntity(c.actor(), e.actor())) continue;
            if (e.place().known() && c.place().known() && !c.place().cell(rt.index().cellSize()).equals(e.place().cell(rt.index().cellSize()))) continue;
            double sim = e.tags().isEmpty() ? 1.0D : similarity.similarity(e.tags(), c);
            if (sim >= bestSim || e.tags().isEmpty()) { best = c; bestSim = sim; }
        }
        return best;
    }

    private static boolean sameEntity(yadi.samuraiai.ai.cognition.model.EntityRef a, yadi.samuraiai.ai.cognition.model.EntityRef b) {
        return a == null ? b == null : b != null && a.id().equals(b.id());
    }

    private void reinforce(MemoryRuntime rt, MemoryRecord r, Experience e, Evaluation evaluation, long now, MemorySettings s) {
        rt.reindex(r, () -> {
            r.repeatCount(r.repeatCount() + 1);
            r.strength(r.strength() + s.reinforceGain() * (1.0D - r.strength()));
            if (evaluation.importance().ordinal() > r.importance().ordinal()) r.importance(evaluation.importance());
            r.importance(merger.promoted(r));
            double n = r.repeatCount();
            r.emotion(r.emotion().withIntensity((r.emotion().intensity() * (n - 1) + e.emotion().intensity()) / n));
            r.duration(r.duration() + e.duration());
            r.endTime(e.stamp().gameTime());
            r.lastReinforced(now);
            r.emotionalWeight(Math.max(r.emotionalWeight(), e.emotion().intensity()));
            for (String ev : e.events()) if (r.events().size() < s.maxEventsPerRecord() && !r.events().contains(ev)) r.events().add(ev);
            r.tags().addAll(e.tags());
            r.bumpVersion();
            evolution.confirm(r);
        });
        String skill = e.context().get("skill");
        if (skill != null) {
            SkillKind kind; try { kind = SkillKind.valueOf(e.context().getOrDefault("skillKind", "ROUTE")); } catch (IllegalArgumentException x) { kind = SkillKind.ROUTE; }
            rt.procedural().practice(skill, kind, e.place(), now, s.practiceGain(), s.protectSkillUses());
        }
        metrics.reinforced.incrementAndGet();
        publish(new MemoryUpdatedEvent(rt.npcId(), e.traceId(), r.id(), "reinforced x" + r.repeatCount()));
    }

    // ------------------------------------------------------------------ emotion and links

    /** Emotion strengthens memory: a strongly felt event is remembered better; a traumatic one becomes protected and slow to fade. */
    public boolean reinforceByEmotion(UUID npc, UUID memoryId, double weight, boolean traumatic, long now, UUID traceId) {
        MemoryRuntime rt = runtimes.get(npc);
        MemoryRecord r = rt == null ? null : rt.get(memoryId);
        if (r == null) return false;
        MemorySettings s = settings.get();
        rt.reindex(r, () -> {
            r.emotionalWeight(Math.max(r.emotionalWeight(), weight));
            r.strength(r.strength() + 0.3D * weight * (1.0D - r.strength()));
            if (weight >= 0.85D && r.importance().ordinal() < Importance.HIGH.ordinal()) r.importance(Importance.HIGH);
            if (traumatic) { r.emotion(r.emotion().asTraumatic(true)); r.tags().add("trauma"); r.protect(true); }
            r.bumpVersion();
        });
        publish(new MemoryUpdatedEvent(npc, traceId, memoryId, traumatic ? "trauma" : "emotional reinforcement"));
        return true;
    }

    /** Records that a relationship or a knowledge record was derived from this memory (bidirectional provenance without copying objects). */
    public void linkSocial(UUID npc, UUID memoryId, UUID other) { link(npc, memoryId, other, true); }
    public void linkKnowledge(UUID npc, UUID memoryId, UUID knowledgeId) { link(npc, memoryId, knowledgeId, false); }

    private void link(UUID npc, UUID memoryId, UUID other, boolean social) {
        MemoryRuntime rt = runtimes.get(npc);
        MemoryRecord r = rt == null ? null : rt.get(memoryId);
        if (r == null || other == null) return;
        boolean added = social ? r.socialLinks().add(other) : r.knowledgeLinks().add(other);
        if (added) rt.markDirty();
    }

    /** The NPC's current dominant feeling, kept with its memories as a snapshot. */
    public void snapshotEmotion(UUID npc, EmotionKind kind, double intensity, String mood) { runtime(npc).snapshotEmotion(kind, intensity, mood); }

    // ------------------------------------------------------------------ retrieval

    public List<RetrievalResult> retrieve(UUID npc, RetrievalQuery query, long now) {
        MemoryRuntime rt = runtimes.get(npc);
        if (rt == null) return List.of();
        long started = System.nanoTime();
        List<RetrievalResult> results = retrieval.retrieve(rt, query, now, settings.get());
        long elapsed = System.nanoTime() - started;
        metrics.retrievals.incrementAndGet(); metrics.retrieved.addAndGet(results.size()); metrics.retrievalNanos.addAndGet(elapsed);
        if (!results.isEmpty()) publish(new MemoryRetrievedEvent(npc, null, results.size(), query.mode().name(), elapsed / 1000.0));
        return results;
    }

    /** "Which memories matter right now?" for the Brain. */
    public List<RetrievalResult> relevant(UUID npc, RetrievalContext context, int limit, long now) {
        MemoryRuntime rt = runtimes.get(npc);
        if (rt == null) return List.of();
        long started = System.nanoTime();
        long hitsBefore = rt.cache().shortHits();
        List<RetrievalResult> results = contextRetrieval.relevant(rt, context, limit <= 0 ? settings.get().retrievalLimit() : limit, now, settings.get());
        metrics.retrievals.incrementAndGet(); metrics.retrieved.addAndGet(results.size()); metrics.retrievalNanos.addAndGet(System.nanoTime() - started);
        if (rt.cache().shortHits() > hitsBefore) metrics.cacheHits.incrementAndGet(); else metrics.cacheMisses.incrementAndGet();
        return results;
    }

    /**
     * Emotional echoes: strongly felt memories tied to the people and place at hand reawaken their feeling. Each memory echoes at
     * most once per cooldown, and the caller must not turn an echo back into a new memory, so memory and emotion cannot feed each other in a loop.
     */
    public List<Echo> echoes(UUID npc, RetrievalContext context, long now) {
        MemoryRuntime rt = runtimes.get(npc);
        if (rt == null) return List.of();
        MemorySettings s = settings.get();
        Map<UUID, Double> candidates = new HashMap<>();
        for (UUID e : context.nearby()) for (UUID id : rt.index().entity(e)) candidates.merge(id, 1.0D, Double::sum);
        if (context.place().known()) for (UUID id : rt.index().cell(context.place().cell(rt.index().cellSize()))) candidates.merge(id, 0.6D, Double::sum);
        if (!context.place().zone().isEmpty()) for (UUID id : rt.index().zone(context.place().zone())) candidates.merge(id, 0.4D, Double::sum);
        Map<EmotionKind, Echo> best = new HashMap<>();
        for (UUID id : candidates.keySet()) {
            MemoryRecord r = rt.get(id);
            if (r == null || r.state() == MemoryState.FORGOTTEN || r.emotion().intensity() < s.echoMinIntensity() || now - r.lastEcho() < s.echoCooldownTicks()) continue;
            double intensity = r.emotion().intensity() * (0.4D + 0.6D * r.strength()) * s.echoScale() * Math.min(1.0D, candidates.get(id));
            if (intensity <= 0.02D) continue;
            Echo current = best.get(r.emotion().primary());
            if (current == null || intensity > current.intensity()) best.put(r.emotion().primary(), new Echo(r.id(), r.emotion().primary(), intensity, r.emotion().traumatic()));
        }
        List<Echo> result = new ArrayList<>(best.values());
        result.sort((a, b) -> Double.compare(b.intensity(), a.intensity()));
        if (result.size() > 3) result = new ArrayList<>(result.subList(0, 3));
        for (Echo echo : result) {
            MemoryRecord r = rt.get(echo.memoryId());
            r.lastEcho(now);
            metrics.echoes.incrementAndGet();
            publish(new EmotionMemoryActivatedEvent(npc, r.origin().traceId(), r.id(), echo.emotion().name(), echo.intensity()));
        }
        if (!result.isEmpty()) rt.markDirty();
        return result;
    }

    // ------------------------------------------------------------------ maintenance

    /** Budgeted upkeep for one NPC: consolidation, then forgetting/evolution over a batch, then compression. Cheap when nothing is due. */
    public MaintenanceReport tick(UUID npc, long now) {
        MemoryRuntime rt = runtimes.get(npc);
        if (rt == null) return new MaintenanceReport(0, 0, 0, 0, 0);
        MemorySettings s = settings.get();
        long started = System.nanoTime();
        int promoted = 0, merged = 0, forgotten = 0, groups = 0, reduced = 0;
        if (rt.temporaryCount() > 0 && now - rt.lastConsolidation >= Math.max(20, s.consolidationDelayTicks() / 2)) {
            rt.lastConsolidation = now;
            var report = consolidation.run(rt, now, s.consolidationBatch(), false, s);
            promoted = report.promoted(); merged = report.merged();
            publishConsolidation(rt, report, false);
        }
        if (now - rt.lastMaintenance >= s.maintenanceIntervalTicks()) {
            rt.lastMaintenance = now;
            forgotten = maintain(rt, now, s);
            rt.procedural().rust(now, 240000L, 0.01D);
        }
        if (now - rt.lastCompression >= s.compressionIntervalTicks()) {
            rt.lastCompression = now;
            var report = compression.run(rt, now, 8, s);
            if (report.changed()) {
                groups = report.groups(); reduced = report.detailReduced();
                metrics.compressed.addAndGet(groups + reduced);
                metrics.recordsBeforeCompression.addAndGet(report.before()); metrics.recordsAfterCompression.addAndGet(report.after());
                publish(new MemoryCompressedEvent(npc, null, report.before(), report.after(), reduced));
            }
        }
        metrics.maintenanceNanos.addAndGet(System.nanoTime() - started);
        return new MaintenanceReport(promoted, merged, forgotten, groups, reduced);
    }

    private int maintain(MemoryRuntime rt, long now, MemorySettings s) {
        List<UUID> ids = rt.ids();
        if (ids.isEmpty()) return 0;
        PersonalityView personality = personalities.apply(rt.npcId());
        int forgotten = 0;
        int batch = Math.min(s.maintenanceBatch(), ids.size());
        for (int i = 0; i < batch; i++) {
            rt.cursor = (rt.cursor + 1) % ids.size();
            MemoryRecord r = rt.get(ids.get(rt.cursor));
            if (r == null || r.state() == MemoryState.TEMPORARY) continue;
            long elapsed = now - r.lastDecay();
            evolution.relax(r, elapsed, s);
            var result = forgetting.step(r, now, personality, s);
            if (result == ForgettingEngine.Result.FORGET) { forget(rt, r, "faded"); forgotten++; }
        }
        // Strength decay is a function of time (lastDecay is persisted), so only forgetting changes what must be written.
        return forgotten;
    }

    private void forget(MemoryRuntime rt, MemoryRecord r, String reason) {
        r.state(MemoryState.FORGOTTEN);
        rt.remove(r.id());
        metrics.forgotten.incrementAndGet();
        publish(new MemoryForgottenEvent(rt.npcId(), r.origin().traceId(), r.id(), r.kind().name(), reason));
    }

    private void enforceCapacity(MemoryRuntime rt, MemorySettings s) {
        List<MemoryRecord> candidates = new ArrayList<>();
        for (MemoryRecord r : rt.all()) if (!r.isProtected() && r.state() != MemoryState.TEMPORARY) candidates.add(r);
        candidates.sort((a, b) -> Double.compare(value(a), value(b)));
        int excess = rt.size() - s.maxMemories() + Math.max(1, s.maxMemories() / 20);
        for (int i = 0; i < Math.min(excess, candidates.size()); i++) forget(rt, candidates.get(i), "capacity");
    }

    private static double value(MemoryRecord r) { return r.strength() * (0.3D + r.importance().weight()); }

    /** The NPC slept: settle everything fresh (bounded by the sleep batch) and strengthen what mattered. */
    public MaintenanceReport sleep(UUID npc, long now) {
        MemoryRuntime rt = runtimes.get(npc);
        if (rt == null) return new MaintenanceReport(0, 0, 0, 0, 0);
        MemorySettings s = settings.get();
        long started = System.nanoTime();
        var report = consolidation.run(rt, now, s.sleepBatch(), true, s);
        consolidation.reinforceForSleep(rt, s.sleepBatch(), s);
        publishConsolidation(rt, report, true);
        metrics.consolidationNanos.addAndGet(System.nanoTime() - started);
        return new MaintenanceReport(report.promoted(), report.merged(), 0, 0, 0);
    }

    private void publishConsolidation(MemoryRuntime rt, ConsolidationEngine.Report report, boolean sleep) {
        if (report.promoted() + report.merged() == 0) return;
        metrics.consolidated.addAndGet(report.promoted()); metrics.merged.addAndGet(report.merged());
        if (!report.survivors().isEmpty()) publish(new MemoryMergedEvent(rt.npcId(), null, report.survivors().get(0), report.absorbed()));
        publish(new MemoryConsolidatedEvent(rt.npcId(), null, report.promoted(), report.merged(), sleep));
    }

    // ------------------------------------------------------------------ administration

    /** Removes one memory on purpose (an administrator, a command). Protected memories can be removed this way, and only this way. */
    public boolean remove(UUID npc, UUID memoryId, String reason) {
        MemoryRuntime rt = runtimes.get(npc);
        MemoryRecord r = rt == null ? null : rt.get(memoryId);
        if (r == null) return false;
        forget(rt, r, reason == null ? "removed" : reason);
        return true;
    }

    public void protect(UUID npc, UUID memoryId, boolean value) {
        MemoryRuntime rt = runtimes.get(npc);
        MemoryRecord r = rt == null ? null : rt.get(memoryId);
        if (r == null) return;
        rt.reindex(r, () -> { r.protect(value); if (value) r.tags().add("protected"); else r.tags().remove("protected"); });
        publish(new MemoryUpdatedEvent(npc, null, memoryId, value ? "protected" : "unprotected"));
    }
}
