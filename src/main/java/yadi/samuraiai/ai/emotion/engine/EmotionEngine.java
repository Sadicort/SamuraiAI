package yadi.samuraiai.ai.emotion.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import yadi.samuraiai.ai.cognition.model.Cause;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.PersonalityView;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.cognition.model.Stamp;
import yadi.samuraiai.ai.emotion.dialogue.DialogueEmotionEngine;
import yadi.samuraiai.ai.emotion.events.EmotionBlendedEvent;
import yadi.samuraiai.ai.emotion.events.EmotionContagionEvent;
import yadi.samuraiai.ai.emotion.events.EmotionCreatedEvent;
import yadi.samuraiai.ai.emotion.events.EmotionRecoveredEvent;
import yadi.samuraiai.ai.emotion.events.EmotionUpdatedEvent;
import yadi.samuraiai.ai.emotion.events.MoodChangedEvent;
import yadi.samuraiai.ai.emotion.events.TraumaCreatedEvent;
import yadi.samuraiai.ai.emotion.events.TraumaRecoveredEvent;
import yadi.samuraiai.ai.emotion.expressions.ExpressionEngine;
import yadi.samuraiai.ai.emotion.metrics.EmotionMetrics;
import yadi.samuraiai.ai.emotion.model.Activity;
import yadi.samuraiai.ai.emotion.model.Blend;
import yadi.samuraiai.ai.emotion.model.DecayCurve;
import yadi.samuraiai.ai.emotion.model.DialogueTone;
import yadi.samuraiai.ai.emotion.model.EmotionOrigin;
import yadi.samuraiai.ai.emotion.model.EmotionPhase;
import yadi.samuraiai.ai.emotion.model.EmotionRecord;
import yadi.samuraiai.ai.emotion.model.EmotionTrigger;
import yadi.samuraiai.ai.emotion.model.Expression;
import yadi.samuraiai.ai.emotion.model.HistoryEntry;
import yadi.samuraiai.ai.emotion.model.MoodKind;
import yadi.samuraiai.ai.emotion.model.Physiology;
import yadi.samuraiai.ai.emotion.model.RecoverySource;
import yadi.samuraiai.ai.emotion.model.Technique;
import yadi.samuraiai.ai.emotion.model.TraumaRecord;
import yadi.samuraiai.ai.emotion.model.TriggerSource;
import yadi.samuraiai.ai.emotion.mood.MoodEngine;
import yadi.samuraiai.ai.emotion.personality.EmotionRule;
import yadi.samuraiai.ai.emotion.personality.PersonalityEmotionModel;
import yadi.samuraiai.ai.emotion.physiology.PhysiologyEngine;
import yadi.samuraiai.ai.emotion.propagation.ContagionEngine;
import yadi.samuraiai.ai.emotion.propagation.EmotionNeighbor;
import yadi.samuraiai.ai.emotion.recovery.DecayEngine;
import yadi.samuraiai.ai.emotion.recovery.RecoveryEngine;
import yadi.samuraiai.ai.emotion.regulation.RegulationAdvice;
import yadi.samuraiai.ai.emotion.regulation.RegulationEngine;
import yadi.samuraiai.ai.emotion.regulation.ResilienceModel;
import yadi.samuraiai.ai.emotion.states.EmotionalBlender;
import yadi.samuraiai.ai.emotion.trauma.TraumaEngine;
import yadi.samuraiai.ai.emotion.triggers.TriggerEngine;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.event.NpcEvent;

/**
 * The inner life of every NPC. It owns one {@link EmotionRuntime} per NPC and coordinates the specialised engines: triggers
 * turn events into emotions, decay and regulation fade them, the mood engine keeps the dominant state, the blender reads the
 * mixture, trauma and recovery handle lasting wounds, and contagion, expression, physiology and dialogue derive what the rest
 * of the system can use. It never moves an entity or picks a behavior: it reports how the NPC feels and how that colours it.
 */
public final class EmotionEngine {
    public record TickResult(boolean changed, MoodKind previousMood) { }

    private final Supplier<EmotionSettings> settings;
    private EventSink events;
    private final Map<UUID, EmotionRuntime> runtimes = new HashMap<>();
    private final PersonalityEmotionModel personalityModel = new PersonalityEmotionModel();
    private final TriggerEngine triggers = new TriggerEngine(personalityModel);
    private final DecayEngine decay = new DecayEngine();
    private final MoodEngine moodEngine = new MoodEngine();
    private final EmotionalBlender blender = new EmotionalBlender();
    private final RegulationEngine regulation = new RegulationEngine();
    private final ResilienceModel resilience = new ResilienceModel();
    private final TraumaEngine trauma = new TraumaEngine();
    private final RecoveryEngine recovery = new RecoveryEngine();
    private final ContagionEngine contagion = new ContagionEngine();
    private final ExpressionEngine expressions = new ExpressionEngine();
    private final PhysiologyEngine physiology = new PhysiologyEngine();
    private final DialogueEmotionEngine dialogue = new DialogueEmotionEngine();
    private final EmotionMetrics metrics = new EmotionMetrics();
    private Function<UUID, PersonalityView> personalities = id -> PersonalityView.NEUTRAL;

    public EmotionEngine(Supplier<EmotionSettings> settings, EventSink events) {
        this.settings = Objects.requireNonNull(settings);
        this.events = Objects.requireNonNull(events);
    }

    public EmotionSettings settings() { return settings.get(); }
    public EmotionMetrics metrics() { return metrics; }
    public void useEventSink(EventSink sink) { this.events = Objects.requireNonNull(sink); }
    public void usePersonalities(Function<UUID, PersonalityView> source) { this.personalities = Objects.requireNonNull(source); }
    public TraumaEngine traumaEngine() { return trauma; }
    private void publish(NpcEvent event) { events.publish(event); }
    private PersonalityView personality(UUID npc) { return personalities.apply(npc); }

    // ------------------------------------------------------------------ runtimes

    public EmotionRuntime runtime(UUID npc) { return runtimes.computeIfAbsent(npc, EmotionRuntime::new); }
    public Optional<EmotionRuntime> peek(UUID npc) { return Optional.ofNullable(runtimes.get(npc)); }
    public void install(EmotionRuntime runtime) { runtimes.put(runtime.npcId(), runtime); }
    public void unload(UUID npc) { runtimes.remove(npc); }
    public void reset() { runtimes.clear(); }
    public List<UUID> tracked() { return new ArrayList<>(runtimes.keySet()); }

    public double resilienceOf(UUID npc) { return resilience.factor(personality(npc), runtime(npc), settings.get()); }

    // ------------------------------------------------------------------ triggers

    /** An event makes the NPC feel something. Same-kind emotions from the same origin fuse; a strong enough unpleasant one may leave a trauma. */
    public TriggerResult trigger(EmotionTrigger t) {
        long started = System.nanoTime();
        EmotionSettings s = settings.get();
        EmotionRuntime rt = runtime(t.npc());
        try {
            metrics.triggers.incrementAndGet();
            PersonalityView p = personality(t.npc());
            List<EmotionRule> rules = personalityModel.rules(s.effectiveRules());
            double res = resilience.factor(p, rt, s);
            List<EmotionRecord> changed = new ArrayList<>();
            for (TriggerEngine.Initial initial : triggers.evaluate(t, p, rules, res, s)) changed.add(apply(rt, t, initial, p, res, s));
            TraumaRecord created = null;
            boolean reinforced = false;
            Optional<TraumaEngine.Result> traumaResult = trauma.consider(t, changed, rt, res, s);
            if (traumaResult.isPresent()) {
                created = traumaResult.get().trauma();
                reinforced = traumaResult.get().reinforced();
                for (EmotionRecord r : changed) if (r.kind().unpleasant()) { r.traumaId(created.id()); r.curve(DecayCurve.TRAUMA); r.persistence(Math.max(r.persistence(), 0.8D)); }
                metrics.traumas.incrementAndGet();
                rt.markUrgent();
                publish(new TraumaCreatedEvent(t.npc(), t.traceId(), created.id(), created.originKind(), created.intensity(), reinforced));
            }
            boolean pleasant = false;
            for (EmotionRecord r : changed) if (!r.kind().unpleasant() && r.intensity() >= 30.0D) pleasant = true;
            if (pleasant && t.source() != TriggerSource.CONTAGION && !t.fromEcho()) recover(t.npc(), RecoverySource.POSITIVE_EVENT, t.at());
            if (!changed.isEmpty()) { rt.trigger(t.source() + (t.note().isEmpty() ? "" : " " + t.note()), t.at()); rt.markDirty(); }
            return new TriggerResult(changed, created, reinforced);
        } catch (RuntimeException error) {
            metrics.errors.incrementAndGet();
            return new TriggerResult(List.of(), null, false);
        } finally { metrics.triggerNanos.addAndGet(System.nanoTime() - started); }
    }

    private EmotionRecord apply(EmotionRuntime rt, EmotionTrigger t, TriggerEngine.Initial initial, PersonalityView p, double res, EmotionSettings s) {
        EmotionRecord existing = rt.active().get(initial.originKey());
        Cause cause = new Cause(t.source().name(), t.memoryId() == null ? t.ref() : t.memoryId().toString(), t.note(), initial.intensity(), t.at());
        if (existing != null) {
            double old = existing.intensity();
            existing.intensity(Math.max(old, initial.intensity()) + s.fusionGain() * Math.min(old, initial.intensity()));
            existing.reinforcements(existing.reinforcements() + 1);
            existing.updated(t.at());
            existing.phase(EmotionPhase.ACTIVE);
            existing.influences().add(cause);
            if (t.traceId() != null && existing.relatedEvents().size() < s.maxRelated()) existing.relatedEvents().add(t.traceId());
            existing.bump();
            metrics.fused.incrementAndGet();
            publish(new EmotionUpdatedEvent(t.npc(), t.traceId(), existing.id(), existing.kind().name(), old, existing.intensity(), t.source().name()));
            return existing;
        }
        if (rt.active().size() >= s.maxActive()) dropWeakest(rt, t.at(), s);
        EmotionOrigin origin = new EmotionOrigin(t.source(), t.memoryId(), t.ref(), t.traceId());
        EmotionRecord r = new EmotionRecord(UUID.randomUUID(), t.npc(), initial.kind(), origin, initial.originKey(), initial.intensity(), t.at(), s.maxCauses());
        r.curve(DecayEngine.curveFor(initial.kind(), t.traumatic() && initial.kind().unpleasant()));
        r.control(Math.max(0.0D, Math.min(1.0D, 0.3D + 0.4D * (res - 0.5D))));
        r.stability(Math.max(0.0D, Math.min(1.0D, 1.0D - r.arousal() * 0.5D - initial.intensity() / 200.0D)));
        r.persistence(persistence(initial.kind()));
        r.influences().add(cause);
        if (t.traceId() != null) r.relatedEvents().add(t.traceId());
        rt.active().put(r.key(), r);
        metrics.created.incrementAndGet();
        publish(new EmotionCreatedEvent(t.npc(), t.traceId(), r.id(), r.kind().name(), r.intensity(), t.source().name(), t.memoryId()));
        return r;
    }

    private static double persistence(EmotionKind kind) {
        return switch (kind) {
            case SADNESS, LONELINESS, GUILT, EMOTIONAL_FATIGUE -> 0.7D;
            case PRIDE, GRATITUDE, HOPE, RESPECT, DISTRUST -> 0.6D;
            case SURPRISE, CURIOSITY -> 0.15D;
            default -> 0.4D;
        };
    }

    private void dropWeakest(EmotionRuntime rt, long now, EmotionSettings s) {
        EmotionRecord weakest = null;
        for (EmotionRecord r : rt.active().values()) if (weakest == null || r.intensity() < weakest.intensity()) weakest = r;
        if (weakest != null) retire(rt, weakest, now, "REPLACED", s);
    }

    private void retire(EmotionRuntime rt, EmotionRecord r, long now, String outcome, EmotionSettings s) {
        rt.active().remove(r.key());
        r.phase(EmotionPhase.RECOVERED);
        rt.addHistory(new HistoryEntry(r.kind(), r.peak(), r.created(), now, r.origin().source().name(), r.origin().memoryId(), outcome), s.historySize());
        if (outcome.equals("RECOVERED")) { metrics.recovered.incrementAndGet(); publish(new EmotionRecoveredEvent(rt.npcId(), r.origin().traceId(), r.id(), r.kind().name(), r.duration(now))); }
    }

    // ------------------------------------------------------------------ time

    /** Advances one NPC's inner life to {@code now}: decay, recovery, regulation, mood and blend. Cheap: it touches the few active emotions only. */
    public TickResult tick(UUID npc, long now, Activity activity) {
        EmotionRuntime rt = runtimes.get(npc);
        if (rt == null) return new TickResult(false, null);
        long started = System.nanoTime();
        EmotionSettings s = settings.get();
        if (rt.lastUpdate == Long.MIN_VALUE / 2) { rt.lastUpdate = now; rt.lastMoodUpdate = now; rt.moodSince = now; return new TickResult(false, null); }
        long elapsed = now - rt.lastUpdate;
        if (elapsed <= 0) return new TickResult(false, null);
        PersonalityView p = personality(npc);
        List<EmotionRule> rules = personalityModel.rules(s.effectiveRules());
        double res = resilience.factor(p, rt, s);
        Technique technique = now < rt.techniqueUntil() ? rt.technique() : Technique.NONE;
        double efficiency = regulation.efficiency(p);
        double boost = regulation.boost(technique, activity == null ? Activity.NONE : activity, s) * (1.0D + s.passiveRegulation() * efficiency);
        boolean changed = false;
        for (EmotionRecord r : rt.activeRecords()) {
            double speed = personalityModel.decay(rules, r.kind(), p) * (r.kind().unpleasant() ? boost : 1.0D);
            double traumaRemaining = 1.0D;
            if (r.traumaId() != null) for (TraumaRecord t : rt.traumas()) if (t.id().equals(r.traumaId())) traumaRemaining = t.remaining();
            double before = r.intensity();
            decay.decay(r, elapsed, speed, traumaRemaining, s);
            if (Math.abs(before - r.intensity()) > 1e-6) changed = true;
            if (r.intensity() < s.minIntensityKeep()) { retire(rt, r, now, "RECOVERED", s); changed = true; rt.markDirty(); }
            else r.phase(r.intensity() < r.peak() * 0.5D ? EmotionPhase.FADING : EmotionPhase.ACTIVE);
        }
        // Traumas heal with time, sleep and meditation, at the pace of the NPC's resilience.
        double days = (double) elapsed / Stamp.TICKS_PER_DAY;
        for (TraumaRecord t : new ArrayList<>(rt.traumas())) {
            if (!t.active()) continue;
            double amount = s.recoveryPerDay() * days;
            if (activity == Activity.SLEEPING) amount += s.recoverySleep() * elapsed / 8000.0D;
            else if (activity == Activity.MEDITATING) amount += s.recoveryMeditation() * elapsed / 2400.0D;
            if (recovery.advance(t, amount, res, now, s)) traumaRecovered(rt, t);
            changed = true;
        }
        if (now - rt.lastRegulation >= s.regulationInterval()) {
            rt.lastRegulation = now;
            RegulationAdvice advice = regulation.advise(rt.unpleasantLoad(), rt.intensityOf(EmotionKind.EMOTIONAL_FATIGUE), p, s);
            if (advice.technique() != rt.advice().technique()) metrics.regulations.incrementAndGet();
            rt.advice(advice);
        }
        MoodKind previous = null;
        if (now - rt.lastMoodUpdate >= s.moodUpdateTicks()) {
            long moodElapsed = now - rt.lastMoodUpdate;
            rt.lastMoodUpdate = now;
            Optional<MoodKind> old = moodEngine.update(rt, now, moodElapsed, s);
            if (old.isPresent()) {
                previous = old.get();
                metrics.moodChanges.incrementAndGet();
                rt.blend(blender.blend(rt, s));
                publish(new MoodChangedEvent(npc, null, previous.name(), rt.mood().name(), rt.blend().dominant().name()));
                rt.markUrgent();
            }
            Blend blend = blender.blend(rt, s);
            rt.blend(blend);
            if (!blend.label().equals(rt.lastBlendLabel())) {
                rt.lastBlendLabel(blend.label());
                if (blend.mixed()) publish(new EmotionBlendedEvent(npc, null, blend.label(), blend.valence(), blend.arousal()));
            }
        }
        metrics.inMood(rt.mood(), elapsed);
        rt.moodTicks().merge(rt.mood(), elapsed, Long::sum);
        rt.lastUpdate = now;
        metrics.ticks.incrementAndGet();
        metrics.tickNanos.addAndGet(System.nanoTime() - started);
        return new TickResult(changed, previous);
    }

    private void traumaRecovered(EmotionRuntime rt, TraumaRecord t) {
        metrics.traumasRecovered.incrementAndGet();
        for (EmotionRecord r : rt.activeRecords()) if (t.id().equals(r.traumaId())) { r.traumaId(null); r.curve(DecayEngine.curveFor(r.kind(), false)); }
        rt.markUrgent();
        publish(new TraumaRecoveredEvent(rt.npcId(), null, t.id(), t.originKind()));
    }

    // ------------------------------------------------------------------ recovery, regulation

    /** A recovery source (friendship, a good event, a conversation...) eases every open trauma a little. */
    public int recover(UUID npc, RecoverySource source, long now) {
        EmotionRuntime rt = runtimes.get(npc);
        if (rt == null) return 0;
        EmotionSettings s = settings.get();
        double res = resilience.factor(personality(npc), rt, s);
        int advanced = 0;
        for (TraumaRecord t : new ArrayList<>(rt.traumas())) {
            if (!t.active()) continue;
            if (recovery.advance(t, RecoveryEngine.amount(source, s), res, now, s)) traumaRecovered(rt, t);
            advanced++;
        }
        if (advanced > 0) rt.markDirty();
        return advanced;
    }

    /** The NPC deliberately calms itself (or is made to) for a while. */
    public void regulate(UUID npc, Technique technique, long now, long durationTicks) {
        EmotionRuntime rt = runtime(npc);
        rt.technique(technique, now + Math.max(0, durationTicks));
        rt.markDirty();
    }

    public RegulationAdvice regulationAdvice(UUID npc) { EmotionRuntime rt = runtimes.get(npc); return rt == null ? RegulationAdvice.NONE : rt.advice(); }

    // ------------------------------------------------------------------ traumas, echoes

    /** Flashbacks the place and people at hand would cause: one echo trigger per matching trauma (limited by a per-trauma cooldown). Only feelings; nothing more. */
    public List<EmotionTrigger> flashbacks(UUID npc, PlaceRef place, List<UUID> nearby, long now) {
        EmotionRuntime rt = runtimes.get(npc);
        if (rt == null) return List.of();
        EmotionSettings s = settings.get();
        List<EmotionTrigger> result = new ArrayList<>();
        for (TraumaEngine.Flashback f : trauma.matching(rt, place, nearby, s)) {
            Long last = rt.lastFlashback.get(f.trauma().id());
            if (last != null && now - last < s.traumaEchoCooldown()) continue;
            rt.lastFlashback.put(f.trauma().id(), now);
            f.trauma().flashbacks(f.trauma().flashbacks() + 1);
            f.trauma().avoidance(Math.min(1.0D, f.trauma().avoidance() + 0.05D));
            metrics.echoes.incrementAndGet();
            result.add(new EmotionTrigger(npc, TriggerSource.ECHO, List.of(new yadi.samuraiai.ai.emotion.model.EmotionEffect(f.trauma().emotion(), f.intensity())),
                    f.trauma().memories().stream().findFirst().orElse(null), "trauma:" + f.trauma().id(), null, now, place, null, false, 0.6D, true, f.trauma().originKind(), "flashback"));
        }
        if (!result.isEmpty()) rt.markDirty();
        return result;
    }

    // ------------------------------------------------------------------ contagion

    /** The transfers this NPC's feelings cause among the neighbours the adapter found (rate-limited per NPC). The caller applies them as triggers. */
    public List<ContagionEngine.Transfer> contagion(UUID npc, List<EmotionNeighbor> neighbors, long now) {
        EmotionRuntime rt = runtimes.get(npc);
        if (rt == null) return List.of();
        EmotionSettings s = settings.get();
        if (now - rt.lastContagion < s.contagionCooldown()) return List.of();
        List<ContagionEngine.Transfer> transfers = contagion.spread(rt, neighbors, s);
        if (transfers.isEmpty()) return transfers;
        rt.lastContagion = now;
        for (ContagionEngine.Transfer t : transfers) { metrics.contagions.incrementAndGet(); publish(new EmotionContagionEvent(npc, null, t.target(), t.kind().name(), t.intensity())); }
        return transfers;
    }

    // ------------------------------------------------------------------ views

    public MoodKind mood(UUID npc) { EmotionRuntime rt = runtimes.get(npc); return rt == null ? MoodKind.NEUTRAL : rt.mood(); }
    public Blend blend(UUID npc) { EmotionRuntime rt = runtimes.get(npc); return rt == null ? Blend.CALM : rt.blend(); }
    public double intensity(UUID npc, EmotionKind kind) { EmotionRuntime rt = runtimes.get(npc); return rt == null ? 0.0D : rt.intensityOf(kind); }
    /** The NPC's mood as a value from -1 to +1, used to tilt (within a cap) how it reads social evidence. */
    public double moodValence(UUID npc) { EmotionRuntime rt = runtimes.get(npc); return rt == null ? 0.0D : rt.mood().valence(); }
    public Expression expression(UUID npc) { EmotionRuntime rt = runtimes.get(npc); return rt == null ? Expression.NEUTRAL : expressions.expression(rt.blend(), settings.get()); }
    public Physiology physiology(UUID npc) { EmotionRuntime rt = runtimes.get(npc); return rt == null ? Physiology.NEUTRAL : physiology.physiology(rt, settings.get()); }
    public DialogueTone tone(UUID npc) { EmotionRuntime rt = runtimes.get(npc); return rt == null ? DialogueTone.NEUTRAL : dialogue.tone(rt, rt.blend()); }
    public List<TraumaRecord> traumas(UUID npc) { EmotionRuntime rt = runtimes.get(npc); return rt == null ? List.of() : List.copyOf(rt.traumas()); }

    /** The emotional weight of an experience for memory: how strongly the NPC felt it (0-1), and whether it left a trauma. */
    public double weightOf(TriggerResult result) {
        double weight = 0;
        for (EmotionRecord r : result.records()) weight = Math.max(weight, r.intensity() / 100.0D);
        return weight;
    }

    /** Why does the NPC feel this? Each active record of the kind with its origin, memory and causes. */
    public List<String> explain(UUID npc, EmotionKind kind) {
        EmotionRuntime rt = runtimes.get(npc);
        List<String> lines = new ArrayList<>();
        if (rt == null) return List.of("sin datos emocionales");
        for (EmotionRecord r : rt.activeRecords()) {
            if (r.kind() != kind) continue;
            lines.add(kind + " " + Math.round(r.intensity()) + " (pico " + Math.round(r.peak()) + ", " + r.curve() + ") origen " + r.origin().source()
                    + (r.origin().memoryId() == null ? "" : " memoria " + r.origin().memoryId().toString().substring(0, 8)));
            for (Cause c : r.influences().recent(5)) lines.add("  - " + c.kind() + " " + String.format(java.util.Locale.ROOT, "%+.0f", c.delta()) + (c.note().isEmpty() ? "" : " (" + c.note() + ")"));
            if (r.traumaId() != null) lines.add("  - ligada a un trauma");
        }
        if (lines.isEmpty()) lines.add("no siente " + kind + " ahora");
        return lines;
    }

    public int activeEmotions() { int n = 0; for (EmotionRuntime rt : runtimes.values()) n += rt.active().size(); return n; }
    public int activeTraumas() { int n = 0; for (EmotionRuntime rt : runtimes.values()) for (TraumaRecord t : rt.traumas()) if (t.active()) n++; return n; }
}
