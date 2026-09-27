package yadi.samuraiai.ai.relationship.engine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import yadi.samuraiai.ai.cognition.model.Cause;
import yadi.samuraiai.ai.cognition.model.EntityKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.PersonalityView;
import yadi.samuraiai.ai.relationship.affinity.AffinityEngine;
import yadi.samuraiai.ai.relationship.events.FriendshipLevelChangedEvent;
import yadi.samuraiai.ai.relationship.events.HonorChangedEvent;
import yadi.samuraiai.ai.relationship.events.LoyaltyChangedEvent;
import yadi.samuraiai.ai.relationship.events.PromiseBrokenEvent;
import yadi.samuraiai.ai.relationship.events.PromiseCreatedEvent;
import yadi.samuraiai.ai.relationship.events.PromiseFulfilledEvent;
import yadi.samuraiai.ai.relationship.events.RelationshipCreatedEvent;
import yadi.samuraiai.ai.relationship.events.RespectChangedEvent;
import yadi.samuraiai.ai.relationship.events.ReputationUpdatedEvent;
import yadi.samuraiai.ai.relationship.events.TrustChangedEvent;
import yadi.samuraiai.ai.relationship.evolution.RelationshipEvolutionEngine;
import yadi.samuraiai.ai.relationship.friendship.FriendshipEngine;
import yadi.samuraiai.ai.relationship.honor.HonorSystem;
import yadi.samuraiai.ai.relationship.loyalty.LoyaltyEngine;
import yadi.samuraiai.ai.relationship.memorylink.MemoryLinkEngine;
import yadi.samuraiai.ai.relationship.metrics.RelationshipMetrics;
import yadi.samuraiai.ai.relationship.model.Dimension;
import yadi.samuraiai.ai.relationship.model.FriendshipStage;
import yadi.samuraiai.ai.relationship.model.HonorCategory;
import yadi.samuraiai.ai.relationship.model.PromiseRecord;
import yadi.samuraiai.ai.relationship.model.PromiseStatus;
import yadi.samuraiai.ai.relationship.model.RelationType;
import yadi.samuraiai.ai.relationship.model.RelationState;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;
import yadi.samuraiai.ai.relationship.model.RespectLevel;
import yadi.samuraiai.ai.relationship.model.ReputationHearsay;
import yadi.samuraiai.ai.relationship.model.ReputationLabel;
import yadi.samuraiai.ai.relationship.model.ReputationRecord;
import yadi.samuraiai.ai.relationship.model.SocialEffect;
import yadi.samuraiai.ai.relationship.model.SocialEvent;
import yadi.samuraiai.ai.relationship.model.SocialEvidence;
import yadi.samuraiai.ai.relationship.model.TrustLevel;
import yadi.samuraiai.ai.relationship.respect.RespectEngine;
import yadi.samuraiai.ai.relationship.reputation.ReputationEngine;
import yadi.samuraiai.ai.relationship.rivalry.RivalryEngine;
import yadi.samuraiai.ai.relationship.socialgraph.SocialGraph;
import yadi.samuraiai.ai.relationship.trust.TrustEngine;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.event.NpcEvent;

/**
 * Turns social evidence into living relationships. It owns one {@link RelationshipRuntime} per NPC and the world's
 * {@link SocialGraph}, and delegates each axis to its own engine. Relationships are directional and evolve gradually; every
 * change records its cause, so {@code explain} can answer "why does this NPC distrust me?". Memory feeds it (through evidence)
 * but it never reads memory, and it never decides behavior.
 */
public final class RelationshipEngine {
    private final Supplier<RelationshipSettings> settings;
    private EventSink events;
    private final Map<UUID, RelationshipRuntime> runtimes = new HashMap<>();
    private final SocialGraph graph = new SocialGraph();
    private final EnumMap<Dimension, DimensionEngine> dimensions = new EnumMap<>(Dimension.class);
    private final FriendshipEngine friendship = new FriendshipEngine();
    private final ReputationEngine reputation = new ReputationEngine();
    private final RelationshipEvolutionEngine evolution = new RelationshipEvolutionEngine();
    private final MemoryLinkEngine memoryLink = new MemoryLinkEngine();
    private final RelationshipMetrics metrics = new RelationshipMetrics();
    private List<String> rulesSource = List.of();
    private List<TraitRule> rules = List.of();
    private Function<UUID, PersonalityView> personalities = id -> PersonalityView.NEUTRAL;
    private Function<UUID, Double> moods = id -> 0.0D;

    public RelationshipEngine(Supplier<RelationshipSettings> settings, EventSink events) {
        this.settings = Objects.requireNonNull(settings);
        this.events = Objects.requireNonNull(events);
        dimensions.put(Dimension.TRUST, new TrustEngine());
        dimensions.put(Dimension.RESPECT, new RespectEngine());
        dimensions.put(Dimension.AFFINITY, new AffinityEngine());
        dimensions.put(Dimension.FEAR, new FearEngine());
        dimensions.put(Dimension.LOYALTY, new LoyaltyEngine());
        dimensions.put(Dimension.RIVALRY, new RivalryEngine());
        dimensions.put(Dimension.HONOR, new HonorSystem());
    }

    public RelationshipSettings settings() { return settings.get(); }
    public RelationshipMetrics metrics() { return metrics; }
    public SocialGraph graph() { return graph; }
    public ReputationEngine reputationEngine() { return reputation; }
    public FriendshipEngine friendshipEngine() { return friendship; }
    public void useEventSink(EventSink sink) { this.events = Objects.requireNonNull(sink); }
    public void usePersonalities(Function<UUID, PersonalityView> source) { this.personalities = Objects.requireNonNull(source); }
    /** The NPC's current mood as a value from -1 (dark) to +1 (bright); it only tilts how evidence is read, within a configured cap. */
    public void useMoods(Function<UUID, Double> source) { this.moods = Objects.requireNonNull(source); }

    private void publish(NpcEvent event) { events.publish(event); }

    private List<TraitRule> rules(RelationshipSettings s) {
        List<String> lines = s.effectiveTraitRules();
        if (lines != rulesSource) { rules = TraitRule.parse(lines); rulesSource = lines; }
        return rules;
    }

    // ------------------------------------------------------------------ runtimes

    public RelationshipRuntime runtime(UUID npc) { return runtimes.computeIfAbsent(npc, RelationshipRuntime::new); }
    public Optional<RelationshipRuntime> peek(UUID npc) { return Optional.ofNullable(runtimes.get(npc)); }

    public void install(RelationshipRuntime runtime, long now) {
        runtimes.put(runtime.npcId(), runtime);
        for (RelationshipRecord r : runtime.all()) graph.update(r, now);
    }

    public void unload(UUID npc) {
        RelationshipRuntime rt = runtimes.remove(npc);
        if (rt != null) for (RelationshipRecord r : rt.all()) graph.remove(npc, r.target().id());
    }

    /** The NPC left the world for good: forget it as a subject and as an object of others' relationships. */
    public void forgetNpc(UUID npc) {
        runtimes.remove(npc);
        graph.removeNode(npc);
        for (RelationshipRuntime rt : runtimes.values()) if (rt.remove(npc) != null) rt.markDirty();
    }

    public void reset() { runtimes.clear(); graph.clear(); }
    public List<UUID> tracked() { return new ArrayList<>(runtimes.keySet()); }

    public Optional<RelationshipRecord> find(UUID npc, UUID target) {
        RelationshipRuntime rt = runtimes.get(npc);
        return rt == null ? Optional.empty() : Optional.ofNullable(rt.get(target));
    }

    public List<RelationshipRecord> relationships(UUID npc) { RelationshipRuntime rt = runtimes.get(npc); return rt == null ? List.of() : rt.all(); }

    private double[] initial(RelationshipSettings s) {
        double[] v = new double[Dimension.values().length];
        v[Dimension.TRUST.ordinal()] = s.initialTrust(); v[Dimension.RESPECT.ordinal()] = s.initialRespect(); v[Dimension.AFFINITY.ordinal()] = s.initialAffinity();
        v[Dimension.FEAR.ordinal()] = s.initialFear(); v[Dimension.LOYALTY.ordinal()] = s.initialLoyalty(); v[Dimension.RIVALRY.ordinal()] = s.initialRivalry();
        v[Dimension.HONOR.ordinal()] = s.initialHonor();
        return v;
    }

    // ------------------------------------------------------------------ evidence

    /** Applies one piece of evidence to the NPC's relationship with the other entity, creating it when it does not exist yet. */
    public Update apply(SocialEvidence e) {
        long started = System.nanoTime();
        RelationshipSettings s = settings.get();
        try {
            RelationshipRuntime rt = runtime(e.npc());
            RelationshipRecord r = rt.get(e.other().id());
            boolean created = r == null;
            if (created) {
                if (rt.size() >= s.maxRelationships()) prune(rt, s);
                r = new RelationshipRecord(UUID.randomUUID(), e.npc(), e.other(), e.at(), initial(s), s.maxCauses());
                rt.put(r);
                metrics.created.incrementAndGet();
                publish(new RelationshipCreatedEvent(e.npc(), e.traceId(), e.other().id(), e.other().name(), e.kind()));
            }
            Update update = applyTo(rt, r, created, e, s);
            metrics.applied.incrementAndGet();
            return update;
        } finally { metrics.applyNanos.addAndGet(System.nanoTime() - started); }
    }

    private Update applyTo(RelationshipRuntime rt, RelationshipRecord r, boolean created, SocialEvidence e, RelationshipSettings s) {
        PersonalityView personality = personalities.apply(e.npc());
        double mood = moods.apply(e.npc());
        Modulation modulation = new Modulation(personality, mood, e, rules(s));
        TrustLevel trustBefore = TrustEngine.level(r, s);
        FriendshipStage stageBefore = r.stage();
        HonorCategory honorBefore = HonorSystem.category(r, s);
        RespectLevel respectBefore = RespectEngine.level(r, s);
        double weight = 0.5D + 0.5D * e.weight();
        double observed = e.observedOnly() ? s.observedScale() : 1.0D;
        Map<Dimension, Double> deltas = new EnumMap<>(Dimension.class);
        double[] before = new double[Dimension.values().length];
        for (Dimension d : Dimension.values()) before[d.ordinal()] = r.get(d);
        boolean loyaltyBroken = false;
        for (Dimension d : Dimension.values()) {
            double raw = e.effect().get(d);
            if (raw == 0) continue;
            // What was only witnessed says less about trust, closeness and fear than what was lived; honor and respect are judged by watching.
            double scaled = raw * weight * (d == Dimension.HONOR || d == Dimension.RESPECT ? Math.max(observed, 0.8D) : observed);
            double delta = dimensions.get(d).modulate(scaled, r, modulation, s);
            if (Math.abs(delta) < 1e-9) continue;
            double old = r.get(d);
            r.set(d, old + delta);
            double applied = r.get(d) - old;
            deltas.put(d, applied);
            String note = String.format(java.util.Locale.ROOT, "raw %.1f -> %.1f%s", raw, applied, e.observedOnly() ? " (observed)" : "");
            r.causes(d).add(new Cause(e.kind(), e.memoryId() == null ? "" : e.memoryId().toString(), note + (e.note().isEmpty() ? "" : " " + e.note()), applied, e.at()));
            if (d == Dimension.LOYALTY && applied < 0 && LoyaltyEngine.breaks(old, -applied, s)) loyaltyBroken = true;
        }
        if (loyaltyBroken) r.loyaltyBroken(true);
        else if (r.loyaltyBroken() && r.loyalty() >= 30.0D) r.loyaltyBroken(false);
        r.interactions(r.interactions() + 1);
        double net = deltas.getOrDefault(Dimension.TRUST, 0.0D) + deltas.getOrDefault(Dimension.AFFINITY, 0.0D) + deltas.getOrDefault(Dimension.RESPECT, 0.0D) * 0.5D
                - deltas.getOrDefault(Dimension.FEAR, 0.0D) * 0.5D - deltas.getOrDefault(Dimension.RIVALRY, 0.0D) * 0.5D;
        if (net > 0.5D) r.positive(r.positive() + 1); else if (net < -0.5D) r.negative(r.negative() + 1);
        if (e.tags().contains("shared_danger")) r.sharedDanger(r.sharedDanger() + 1);
        if (e.tags().contains("oath_kept")) r.oathsKept(r.oathsKept() + 1);
        if (e.tags().contains("oath_broken")) r.oathsBroken(r.oathsBroken() + 1);
        if (e.loyaltyKind() != null) r.loyaltyKind(e.loyaltyKind());
        r.lastInteraction(e.at());
        r.lastDecay(Math.max(r.lastDecay(), e.at()));
        r.state(deriveState(r, s));
        r.history().add(new SocialEvent(e.at(), e.kind(), e.memoryId(), deltas.getOrDefault(Dimension.TRUST, 0.0D), deltas.getOrDefault(Dimension.RESPECT, 0.0D),
                deltas.getOrDefault(Dimension.HONOR, 0.0D), e.note()));
        while (r.history().size() > s.maxHistory()) r.history().remove(0);
        memoryLink.link(r, e, s);
        FriendshipStage stage = friendship.step(r, e.at(), s);
        r.stage(stage);
        r.type(deriveType(r, e.roleHint(), s));
        r.bump();
        rt.markDirty();
        rt.updated(e.at(), e.kind());
        graph.update(r, e.at());
        // Events: only real changes, so the bus is not flooded by every nudge.
        TrustLevel trustAfter = TrustEngine.level(r, s);
        double dTrust = deltas.getOrDefault(Dimension.TRUST, 0.0D);
        if (Math.abs(dTrust) >= s.eventMinDelta() || trustBefore != trustAfter)
            publish(new TrustChangedEvent(e.npc(), e.traceId(), r.target().id(), before[Dimension.TRUST.ordinal()], r.trust(), trustBefore.name(), trustAfter.name(), e.kind()));
        double dRespect = deltas.getOrDefault(Dimension.RESPECT, 0.0D);
        RespectLevel respectAfter = RespectEngine.level(r, s);
        if (Math.abs(dRespect) >= s.eventMinDelta() || respectBefore != respectAfter)
            publish(new RespectChangedEvent(e.npc(), e.traceId(), r.target().id(), before[Dimension.RESPECT.ordinal()], r.respect(), respectBefore.name(), respectAfter.name(), e.kind()));
        HonorCategory honorAfter = HonorSystem.category(r, s);
        if (Math.abs(deltas.getOrDefault(Dimension.HONOR, 0.0D)) >= s.eventMinDelta() || honorBefore != honorAfter)
            publish(new HonorChangedEvent(e.npc(), e.traceId(), r.target().id(), before[Dimension.HONOR.ordinal()], r.honor(), honorAfter.name(), e.kind()));
        if (Math.abs(deltas.getOrDefault(Dimension.LOYALTY, 0.0D)) >= s.eventMinDelta() || loyaltyBroken)
            publish(new LoyaltyChangedEvent(e.npc(), e.traceId(), r.target().id(), before[Dimension.LOYALTY.ordinal()], r.loyalty(), loyaltyBroken, e.kind()));
        if (stage != stageBefore) {
            metrics.friendshipChanges.incrementAndGet();
            publish(new FriendshipLevelChangedEvent(e.npc(), e.traceId(), r.target().id(), stageBefore.name(), stage.name()));
        }
        if (trustAfter == TrustLevel.SUSPICIOUS && trustBefore != TrustLevel.SUSPICIOUS || loyaltyBroken || honorAfter == HonorCategory.OATH_BREAKER && honorBefore != honorAfter) rt.markUrgent();
        return new Update(r, created, deltas, trustBefore, trustAfter, honorAfter, stageBefore, stage, loyaltyBroken);
    }

    private static RelationState deriveState(RelationshipRecord r, RelationshipSettings s) {
        if (r.trust() < 10.0D && (r.negative() >= 3 || r.oathsBroken() > 0)) return RelationState.BROKEN;
        return RelationState.ACTIVE;
    }

    private RelationType deriveType(RelationshipRecord r, RelationType hint, RelationshipSettings s) {
        if (RivalryEngine.level(r, s).ordinal() >= yadi.samuraiai.ai.relationship.model.RivalryLevel.MAJOR.ordinal()) return RelationType.RIVAL;
        if (hint != null && hint != RelationType.STRANGER && hint != RelationType.ACQUAINTANCE) return hint;
        RelationType current = r.type();
        if (current == RelationType.MASTER || current == RelationType.STUDENT || current == RelationType.GUARD || current == RelationType.MERCHANT
                || current == RelationType.NEIGHBOR || current == RelationType.FACTION_MEMBER) return current;
        if (r.stage().ordinal() >= FriendshipStage.FRIEND.ordinal()) return RelationType.FRIEND;
        if (r.trust() >= s.trustTrusting() && r.respect() >= s.respectModerate()) return RelationType.ALLY;
        return r.interactions() >= s.knownAfterInteractions() ? RelationType.ACQUAINTANCE : RelationType.STRANGER;
    }

    private void prune(RelationshipRuntime rt, RelationshipSettings s) {
        RelationshipRecord weakest = null;
        double lowest = Double.MAX_VALUE;
        for (RelationshipRecord r : rt.all()) {
            double significance = r.interactions() + r.trust() * 0.2D + r.respect() * 0.2D + r.rivalry() * 0.3D + r.fear() * 0.3D + r.loyalty() * 0.5D + (r.state() == RelationState.DORMANT ? -20 : 0);
            if (significance < lowest) { lowest = significance; weakest = r; }
        }
        if (weakest != null) { rt.remove(weakest.target().id()); graph.remove(rt.npcId(), weakest.target().id()); metrics.pruned.incrementAndGet(); }
    }

    // ------------------------------------------------------------------ upkeep

    /** Slow, budgeted cooling of one NPC's relationships. */
    public int tick(UUID npc, long now) {
        RelationshipRuntime rt = runtimes.get(npc);
        if (rt == null) return 0;
        RelationshipSettings s = settings.get();
        if (now - rt.lastDecay < s.decayIntervalTicks()) return 0;
        long started = System.nanoTime();
        rt.lastDecay = now;
        List<RelationshipRecord> all = rt.all();
        int changed = 0;
        int batch = Math.min(s.decayBatch(), all.size());
        for (int i = 0; i < batch; i++) {
            rt.cursor = (rt.cursor + 1) % all.size();
            RelationshipRecord r = all.get(rt.cursor);
            if (evolution.decay(r, now, s)) { changed++; graph.update(r, now); }
        }
        for (ReputationRecord rep : rt.reputation().all()) reputation.decay(rep, now, s);
        expirePromises(rt, now, s);
        if (changed > 0) metrics.decayed.addAndGet(changed);
        metrics.decayNanos.addAndGet(System.nanoTime() - started);
        return changed;
    }

    // ------------------------------------------------------------------ promises

    public PromiseRecord promise(UUID npc, EntityRef promiser, EntityRef promisee, PromiseRecord.Kind kind, String subject, long now, long dueInTicks, boolean publicPromise, double weight, UUID trace) {
        RelationshipSettings s = settings.get();
        RelationshipRuntime rt = runtime(npc);
        if (rt.promises().size() >= s.maxPromises()) {
            UUID oldest = null; long at = Long.MAX_VALUE;
            for (PromiseRecord p : rt.promises().values()) if (p.status() != PromiseStatus.ACTIVE && p.madeAt() < at) { at = p.madeAt(); oldest = p.id(); }
            if (oldest != null) rt.promises().remove(oldest);
        }
        PromiseRecord p = new PromiseRecord(UUID.randomUUID(), promiser, promisee, kind, subject, now, now + (dueInTicks <= 0 ? s.promiseDefaultTicks() : dueInTicks), publicPromise, weight);
        rt.promises().put(p.id(), p);
        rt.markDirty();
        metrics.promisesMade.incrementAndGet();
        EntityRef other = promiser.id().equals(npc) ? promisee : promiser;
        publish(new PromiseCreatedEvent(npc, trace, other.id(), p.id(), kind.name(), publicPromise));
        return p;
    }

    public Optional<PromiseOutcome> fulfill(UUID npc, UUID promiseId, long now, UUID trace) { return resolve(npc, promiseId, PromiseStatus.FULFILLED, now, trace); }
    public Optional<PromiseOutcome> breakPromise(UUID npc, UUID promiseId, long now, UUID trace) { return resolve(npc, promiseId, PromiseStatus.BROKEN, now, trace); }

    private Optional<PromiseOutcome> resolve(UUID npc, UUID promiseId, PromiseStatus status, long now, UUID trace) {
        RelationshipRuntime rt = runtimes.get(npc);
        PromiseRecord p = rt == null ? null : rt.promises().get(promiseId);
        if (p == null || p.status() != PromiseStatus.ACTIVE) return Optional.empty();
        p.resolve(status, now);
        rt.markDirty();
        RelationshipSettings s = settings.get();
        boolean self = p.promiser().id().equals(npc);
        EntityRef other = self ? p.promisee() : p.promiser();
        Update update = null;
        double w = p.weight();
        switch (status) {
            case FULFILLED -> {
                metrics.promisesKept.incrementAndGet();
                publish(new PromiseFulfilledEvent(npc, trace, other.id(), p.id(), p.kind().name()));
                if (!self) update = apply(evidence(npc, other, "PROMISE_KEPT", new SocialEffect(s.promiseKeptTrust(), s.promiseKeptRespect(), 4, 0, 4, 0, s.promiseKeptHonor()), w, p, now, trace, p.oath() ? "oath_kept" : "promise_kept"));
            }
            case BROKEN -> {
                metrics.promisesBroken.incrementAndGet();
                publish(new PromiseBrokenEvent(npc, trace, other.id(), p.id(), p.kind().name(), status.name()));
                if (!self) update = apply(evidence(npc, other, "PROMISE_BROKEN", new SocialEffect(-s.promiseBrokenTrust(), -6, -8, 0, -s.promiseBrokenLoyalty(), 8, -s.promiseBrokenHonor()), w, p, now, trace, p.oath() ? "oath_broken" : "promise_broken"));
            }
            default -> { }
        }
        return Optional.of(new PromiseOutcome(p, update, self));
    }

    private void expirePromises(RelationshipRuntime rt, long now, RelationshipSettings s) {
        for (PromiseRecord p : new ArrayList<>(rt.promises().values())) {
            if (p.status() != PromiseStatus.ACTIVE || now < p.dueAt()) continue;
            p.resolve(PromiseStatus.EXPIRED, now);
            metrics.promisesExpired.incrementAndGet();
            boolean self = p.promiser().id().equals(rt.npcId());
            EntityRef other = self ? p.promisee() : p.promiser();
            publish(new PromiseBrokenEvent(rt.npcId(), null, other.id(), p.id(), p.kind().name(), PromiseStatus.EXPIRED.name()));
            if (!self) apply(evidence(rt.npcId(), other, "PROMISE_EXPIRED", new SocialEffect(-s.promiseExpiredTrust(), -2, -2, 0, 0, 0, -s.promiseExpiredTrust() * 0.3D), p.weight() * 0.6D, p, now, null, "promise_expired"));
            rt.markDirty();
        }
    }

    private static SocialEvidence evidence(UUID npc, EntityRef other, String kind, SocialEffect effect, double weight, PromiseRecord p, long now, UUID trace, String tag) {
        return new SocialEvidence(npc, other, kind, effect, weight, p.publicPromise(), false, null, trace, now, yadi.samuraiai.ai.cognition.model.PlaceRef.unknown(), 1.0D, null, null, Set.of(tag), p.kind().name() + " " + p.subject());
    }

    // ------------------------------------------------------------------ reputation

    /** Folds a report about someone's standing into the NPC's reputation book (and, mildly, its own view of that person). Returns whether it was believed. */
    public boolean hear(ReputationHearsay h, double sourceTrust, double sourceRepute, double evidenceQuality) {
        RelationshipSettings s = settings.get();
        RelationshipRuntime rt = runtime(h.npc());
        ReputationRecord rec = rt.reputation().getOrCreate(h.subject(), h.scope(), h.scopeId());
        UUID sourceId = h.source() == null ? null : h.source().id();
        double credibility = h.direct() ? 1.0D : reputation.credibility(sourceTrust, sourceRepute, h.publicEvent() ? Math.max(0.7D, evidenceQuality) : evidenceQuality,
                find(h.npc(), h.subject().id()).map(r -> r.trust() / 100.0D).orElse(0.3D), rec.sources().size(), s);
        boolean changed = reputation.observe(rec, h.label(), h.strength(), credibility, sourceId, h.direct(), h.at(), s);
        if (!changed) { metrics.hearsayIgnored.incrementAndGet(); return false; }
        metrics.hearsayApplied.incrementAndGet();
        rt.markDirty();
        publish(new ReputationUpdatedEvent(h.npc(), h.traceId(), h.subject().id(), h.label().name(), h.scope().name(), rec.score(h.label()), rec.confidence()));
        if (!h.direct() && find(h.npc(), h.subject().id()).isPresent()) {
            double sign = positive(h.label()) ? 1.0D : -1.0D;
            double magnitude = h.strength() * credibility * s.hearsayInfluence();
            apply(new SocialEvidence(h.npc(), h.subject(), "HEARSAY", new SocialEffect(sign * 8, sign * 6, 0, 0, 0, 0, sign * 10), magnitude, h.publicEvent(), true, null, h.traceId(), h.at(),
                    yadi.samuraiai.ai.cognition.model.PlaceRef.unknown(), 1.0D, null, null, Set.of("hearsay"), "rumor " + (h.rumorId() == null ? "" : h.rumorId())));
        }
        return true;
    }

    public static boolean positive(ReputationLabel label) { return label == ReputationLabel.PROTECTOR || label == ReputationLabel.SAMURAI || label == ReputationLabel.WISE || label == ReputationLabel.MERCHANT; }

    public List<ReputationRecord> reputationOf(UUID npc, UUID subject) { RelationshipRuntime rt = runtimes.get(npc); return rt == null ? List.of() : rt.reputation().about(subject); }

    // ------------------------------------------------------------------ explanation and statistics

    /** Why is this axis what it is? The value, its level and the causes that moved it, strongest first. */
    public List<String> explain(UUID npc, UUID target, Dimension dimension) {
        RelationshipSettings s = settings.get();
        Optional<RelationshipRecord> found = find(npc, target);
        if (found.isEmpty()) return List.of("no hay relación registrada");
        RelationshipRecord r = found.get();
        List<String> lines = new ArrayList<>();
        String level = switch (dimension) {
            case TRUST -> TrustEngine.level(r, s).name(); case RESPECT -> RespectEngine.level(r, s).name(); case HONOR -> HonorSystem.category(r, s).name();
            case AFFINITY -> AffinityEngine.level(r, s).name(); case RIVALRY -> RivalryEngine.level(r, s).name(); default -> "";
        };
        lines.add(dimension + " = " + Math.round(r.get(dimension)) + (level.isEmpty() ? "" : " (" + level + ")") + " porque:");
        for (Cause c : r.causes(dimension).strongest(6)) lines.add("  - " + c.kind() + " " + String.format(java.util.Locale.ROOT, "%+.1f", c.delta()) + (c.ref().isEmpty() ? "" : " memoria " + c.ref().substring(0, Math.min(8, c.ref().length()))) + " [" + c.note() + "]");
        if (r.causes(dimension).size() == 0) lines.add("  - valor inicial (sin causas registradas)");
        return lines;
    }

    public double averageTrust() {
        double sum = 0; int n = 0;
        for (RelationshipRuntime rt : runtimes.values()) for (RelationshipRecord r : rt.all()) { sum += r.trust(); n++; }
        return n == 0 ? 0 : sum / n;
    }

    public double averageRespect() {
        double sum = 0; int n = 0;
        for (RelationshipRuntime rt : runtimes.values()) for (RelationshipRecord r : rt.all()) { sum += r.respect(); n++; }
        return n == 0 ? 0 : sum / n;
    }

    public int totalRelationships() { int n = 0; for (RelationshipRuntime rt : runtimes.values()) n += rt.size(); return n; }
    public int activePromises() {
        int n = 0;
        for (RelationshipRuntime rt : runtimes.values()) for (PromiseRecord p : rt.promises().values()) if (p.status() == PromiseStatus.ACTIVE) n++;
        return n;
    }
    public Set<UUID> knownIds(UUID npc) { RelationshipRuntime rt = runtimes.get(npc); Set<UUID> ids = new HashSet<>(); if (rt != null) for (RelationshipRecord r : rt.all()) ids.add(r.target().id()); return ids; }
    public boolean isEntityKind(UUID npc, UUID target, EntityKind kind) { return find(npc, target).map(r -> r.target().kind() == kind).orElse(false); }
}
