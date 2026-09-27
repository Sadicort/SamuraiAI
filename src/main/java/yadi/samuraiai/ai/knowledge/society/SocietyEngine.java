package yadi.samuraiai.ai.knowledge.society;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import yadi.samuraiai.ai.cognition.model.EntityKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.PersonalityView;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.cognition.model.Stamp;
import yadi.samuraiai.ai.knowledge.culture.Culture;
import yadi.samuraiai.ai.knowledge.culture.CultureCatalog;
import yadi.samuraiai.ai.knowledge.culture.Tradition;
import yadi.samuraiai.ai.knowledge.culture.TraditionState;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeEngine;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeRuntime;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings;
import yadi.samuraiai.ai.knowledge.engine.LearnResult;
import yadi.samuraiai.ai.knowledge.engine.SourceView;
import yadi.samuraiai.ai.knowledge.events.CommunityChangedEvent;
import yadi.samuraiai.ai.knowledge.events.CultureUpdatedEvent;
import yadi.samuraiai.ai.knowledge.events.HistoryRecordedEvent;
import yadi.samuraiai.ai.knowledge.events.RumorConfirmedEvent;
import yadi.samuraiai.ai.knowledge.events.RumorCreatedEvent;
import yadi.samuraiai.ai.knowledge.events.RumorSpreadEvent;
import yadi.samuraiai.ai.knowledge.history.HistoricalEvent;
import yadi.samuraiai.ai.knowledge.history.HistoryEngine;
import yadi.samuraiai.ai.knowledge.metrics.KnowledgeMetrics;
import yadi.samuraiai.ai.knowledge.model.AccessLevel;
import yadi.samuraiai.ai.knowledge.model.KnowledgeEvidence;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;
import yadi.samuraiai.ai.knowledge.model.LearnMethod;
import yadi.samuraiai.ai.knowledge.model.ValidationEvidence;
import yadi.samuraiai.ai.knowledge.model.ValidationState;
import yadi.samuraiai.ai.knowledge.propagation.PropagationEngine;
import yadi.samuraiai.ai.knowledge.propagation.PropagationQueue;
import yadi.samuraiai.ai.knowledge.propagation.PropagationTask;
import yadi.samuraiai.ai.knowledge.rumors.RumorClaim;
import yadi.samuraiai.ai.knowledge.rumors.RumorEngine;
import yadi.samuraiai.ai.knowledge.rumors.RumorRecord;
import yadi.samuraiai.ai.knowledge.rumors.RumorState;
import yadi.samuraiai.ai.knowledge.worldmemory.WorldMemoryEngine;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.event.NpcEvent;

/**
 * Turns individuals into communities. It owns the communities (members, ranks, culture, collective knowledge, history,
 * traditions), the rumours travelling through them and the queue of tellings, and keeps the world memory. What a community
 * "knows" is what enough of its members believe; its history is what its members witnessed of public events; its culture is
 * data. Information moves between members one budgeted telling at a time, weighted by trust and distance, and a rumour is
 * never treated as a verified fact because it was repeated.
 */
public final class SocietyEngine {
    /** What one delivered telling did. */
    public record Delivery(PropagationTask task, boolean delivered, RumorRecord rumor, KnowledgeRecord record, boolean transformed, double credibility, String note) { }

    private final Supplier<KnowledgeSettings> settings;
    private EventSink events;
    private final KnowledgeEngine knowledge;
    private final Map<String, Community> communities = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> membership = new HashMap<>();
    private final Map<UUID, RumorRecord> rumors = new LinkedHashMap<>();
    private final List<HistoricalEvent> timeline = new ArrayList<>();
    private final HistoryEngine historyEngine = new HistoryEngine();
    private final WorldMemoryEngine worldMemory = new WorldMemoryEngine();
    private final RumorEngine rumorEngine = new RumorEngine();
    private final PropagationEngine propagation = new PropagationEngine();
    private final PropagationQueue queue = new PropagationQueue();
    private final Map<String, Long> gossiped = new HashMap<>();
    private CultureCatalog catalog;
    private List<String> catalogSource;
    private long lastMaintenance = Long.MIN_VALUE / 2;

    public SocietyEngine(Supplier<KnowledgeSettings> settings, EventSink events, KnowledgeEngine knowledge) {
        this.settings = Objects.requireNonNull(settings);
        this.events = Objects.requireNonNull(events);
        this.knowledge = Objects.requireNonNull(knowledge);
    }

    public KnowledgeSettings settings() { return settings.get(); }
    public void useEventSink(EventSink sink) { this.events = Objects.requireNonNull(sink); }
    private void publish(NpcEvent event) { events.publish(event); }
    public KnowledgeMetrics metrics() { return knowledge.metrics(); }
    public WorldMemoryEngine worldMemory() { return worldMemory; }
    public PropagationEngine propagation() { return propagation; }
    public PropagationQueue queue() { return queue; }
    public RumorEngine rumorEngine() { return rumorEngine; }
    public List<HistoricalEvent> worldTimeline() { return List.copyOf(timeline); }

    // ------------------------------------------------------------------ cultures

    public CultureCatalog cultures() {
        List<String> lines = settings.get().cultures();
        if (catalog == null || lines != catalogSource) { catalog = new CultureCatalog(lines); catalogSource = lines; }
        return catalog;
    }

    public double norm(String communityId, String key, double fallback) {
        Community c = communities.get(communityId);
        return c == null ? fallback : cultures().orDefault(c.cultureId()).norm(key, fallback);
    }

    /** How this NPC's communities read honor: the strongest cultural scale among them (1.0 when it belongs to none). */
    public double honorScale(UUID npc) {
        double scale = 1.0D;
        boolean any = false;
        for (String id : membership.getOrDefault(npc, Set.of())) {
            double v = norm(id, "HONOR_SCALE", 1.0D);
            if (!any || Math.abs(v - 1.0D) > Math.abs(scale - 1.0D)) scale = v;
            any = true;
        }
        return scale;
    }

    public double oathWeight(UUID npc) {
        double weight = 1.0D;
        for (String id : membership.getOrDefault(npc, Set.of())) weight = Math.max(weight, norm(id, "OATH_WEIGHT", 1.0D));
        return weight;
    }

    /** The tradition due for an NPC in this period and zone kind, strongest first. */
    public Optional<Tradition> dueTradition(UUID npc, String period, String zoneKind) {
        Tradition best = null;
        double bestStrength = 0.0D;
        for (String id : membership.getOrDefault(npc, Set.of())) {
            Community c = communities.get(id);
            if (c == null) continue;
            for (Tradition t : cultures().orDefault(c.cultureId()).traditions()) {
                if (t.kind() == Tradition.Kind.GREETING || !t.matches(period, zoneKind) || t.period().equals("ANY") && t.zoneKind().equals("ANY")) continue;
                TraditionState state = c.traditions().get(t.id());
                double strength = state == null ? t.strength() : state.strength();
                if (strength > bestStrength) { best = t; bestStrength = strength; }
            }
        }
        return Optional.ofNullable(best);
    }

    /** The NPC took part in a tradition: the tradition strengthens in each of its communities that holds it. */
    public boolean observeTradition(UUID npc, Tradition tradition, long now) {
        KnowledgeSettings s = settings.get();
        boolean any = false;
        for (String id : membership.getOrDefault(npc, Set.of())) {
            Community c = communities.get(id);
            if (c == null || cultures().orDefault(c.cultureId()).traditions().stream().noneMatch(t -> t.id().equals(tradition.id()))) continue;
            TraditionState state = c.traditions().computeIfAbsent(tradition.id(), k -> new TraditionState(tradition.strength(), now));
            state.strength(state.strength() + s.traditionReinforce() * (1.0D - state.strength()));
            state.observed(state.observed() + 1);
            state.lastObserved(now);
            c.markDirty();
            publish(new CultureUpdatedEvent(c.uuid(), null, c.id(), tradition.name(), state.strength(), "observed"));
            any = true;
        }
        return any;
    }

    // ------------------------------------------------------------------ communities

    public Community create(String id, String name, CommunityKind kind, String cultureId, PlaceRef center, double radius) {
        String key = id.toLowerCase(Locale.ROOT);
        Community existing = communities.get(key);
        if (existing != null) return existing;
        if (communities.size() >= settings.get().maxCommunities()) throw new IllegalStateException("Too many communities");
        Community c = new Community(key, name == null || name.isEmpty() ? id : name, kind, cultureId == null || cultureId.isEmpty() ? defaultCulture(kind) : cultureId, center, radius, knowledge.cellSize());
        communities.put(key, c);
        c.markDirty();
        publish(new CommunityChangedEvent(c.uuid(), null, c.id(), null, "created"));
        return c;
    }

    private static String defaultCulture(CommunityKind kind) {
        return switch (kind) { case TEMPLE -> "temple"; case MARKET -> "market"; case GUARD_POST -> "guard"; default -> "village"; };
    }

    public boolean remove(String id) {
        Community c = communities.remove(id.toLowerCase(Locale.ROOT));
        if (c == null) return false;
        for (UUID member : c.memberIds()) membership.getOrDefault(member, Set.of()).remove(c.id());
        publish(new CommunityChangedEvent(c.uuid(), null, c.id(), null, "removed"));
        return true;
    }

    public Optional<Community> community(String id) { return Optional.ofNullable(communities.get(id == null ? "" : id.toLowerCase(Locale.ROOT))); }
    public List<Community> communities() { return List.copyOf(communities.values()); }
    public void put(Community c) { communities.put(c.id(), c); for (UUID m : c.members().keySet()) membership.computeIfAbsent(m, k -> new java.util.LinkedHashSet<>()).add(c.id()); }

    public boolean join(UUID npc, String communityId, AccessLevel rank) {
        Community c = communities.get(communityId.toLowerCase(Locale.ROOT));
        if (c == null) return false;
        boolean fresh = !c.members().containsKey(npc);
        c.members().put(npc, rank == null ? AccessLevel.MEMBERS : rank);
        membership.computeIfAbsent(npc, k -> new java.util.LinkedHashSet<>()).add(c.id());
        knowledge.runtime(npc).ranks().put(c.id(), c.rankOf(npc));
        if (c.leader() == null && rank == AccessLevel.LEADERS) c.leader(npc);
        if (fresh) { c.markDirty(); publish(new CommunityChangedEvent(c.uuid(), null, c.id(), npc, "joined")); }
        return fresh;
    }

    public boolean leave(UUID npc, String communityId) {
        Community c = communities.get(communityId.toLowerCase(Locale.ROOT));
        if (c == null || c.members().remove(npc) == null) return false;
        membership.getOrDefault(npc, Set.of()).remove(c.id());
        knowledge.runtime(npc).ranks().remove(c.id());
        if (npc.equals(c.leader())) c.leader(null);
        c.markDirty();
        publish(new CommunityChangedEvent(c.uuid(), null, c.id(), npc, "left"));
        return true;
    }

    /** The NPC left the world for good. */
    public void forgetNpc(UUID npc) {
        for (String id : new ArrayList<>(membership.getOrDefault(npc, Set.of()))) leave(npc, id);
        membership.remove(npc);
        queue.removeInvolving(npc);
    }

    public Set<String> communitiesOf(UUID npc) { return Set.copyOf(membership.getOrDefault(npc, Set.of())); }
    public AccessLevel rankOf(UUID npc, String communityId) { Community c = communities.get(communityId); return c == null ? AccessLevel.PUBLIC : c.rankOf(npc); }

    /** The highest rank the NPC holds in any community (what it may read of shared knowledge whose community is unknown). */
    public AccessLevel bestRank(UUID npc) {
        AccessLevel best = AccessLevel.PUBLIC;
        for (String id : membership.getOrDefault(npc, Set.of())) { AccessLevel r = rankOf(npc, id); if (r.ordinal() > best.ordinal()) best = r; }
        return best;
    }

    /** The rank two NPCs share for information passing between them: the lower of their best ranks in a community both belong to, or PUBLIC when they share none. */
    public AccessLevel sharedRank(UUID teller, UUID listener) {
        AccessLevel best = AccessLevel.PUBLIC;
        for (String id : membership.getOrDefault(listener, Set.of())) if (membership.getOrDefault(teller, Set.of()).contains(id)) { AccessLevel r = rankOf(listener, id); if (r.ordinal() > best.ordinal()) best = r; }
        return best;
    }

    public Optional<Community> nearestCommunity(PlaceRef place, double maxDistance) {
        Community best = null;
        double bestDistance = maxDistance;
        for (Community c : communities.values()) {
            double d = c.center().distance(place);
            if (d <= Math.max(maxDistance, c.radius()) && d < bestDistance + c.radius()) { if (best == null || d < best.center().distance(place)) best = c; }
        }
        return Optional.ofNullable(best);
    }

    public int communityCount() { return communities.size(); }
    public int totalMembers() { int n = 0; for (Community c : communities.values()) n += c.members().size(); return n; }

    // ------------------------------------------------------------------ collective knowledge

    /** An NPC now believes this firmly enough: once enough of a community's members do, the community adopts it as collective knowledge. */
    public boolean noteKnown(String communityId, KnowledgeRecord r, UUID npc, long now) {
        Community c = communities.get(communityId);
        KnowledgeSettings s = settings.get();
        if (c == null || r.state() == ValidationState.FALSE || r.state() == ValidationState.UNKNOWN || r.state() == ValidationState.FORGOTTEN) return false;
        if (r.state() == ValidationState.RUMOR && r.confidence() < 0.5D) return false;
        String key = r.key();
        Set<UUID> supporters = c.support().computeIfAbsent(key, k -> new java.util.LinkedHashSet<>());
        if (!supporters.add(npc) && c.collective().byKey(key) != null) return false;
        int needed = Math.max(s.collectiveMin(), (int) Math.ceil(s.collectiveFraction() * c.members().size()));
        if (supporters.size() < needed || c.collective().byKey(key) != null) return false;
        KnowledgeEvidence evidence = new KnowledgeEvidence(c.uuid(), r.type(), r.category(), r.subject(), r.predicate(), r.object(), r.attributes(),
                r.publicEvidence() ? LearnMethod.PUBLIC_EVENT : LearnMethod.OBSERVATION, null, r.confidence(), r.place(), r.tags(), null, r.traceId(), now, 1.0D, r.importance(), r.access(), r.rumorId());
        LearnResult result = knowledge.learnInto(c.collective(), evidence, PersonalityView.NEUTRAL);
        if (result.created()) { knowledge.metrics().adopted.incrementAndGet(); c.markDirty(); return true; }
        return false;
    }

    // ------------------------------------------------------------------ history and world memory

    public boolean record(String communityId, HistoricalEvent event) {
        KnowledgeSettings s = settings.get();
        Community c = communities.get(communityId == null ? "" : communityId.toLowerCase(Locale.ROOT));
        boolean added = false;
        if (c != null && HistoryEngine.significant(event.significance(), s)) { added = historyEngine.add(c.history(), event, s.historyMax()); if (added) c.markDirty(); }
        if (HistoryEngine.significant(event.significance(), s) && timeline.stream().noneMatch(e -> e.id().equals(event.id()))) {
            historyEngine.add(timeline, event, s.worldTimelineMax());
            worldMemory.recorded(event);
            knowledge.metrics().history.incrementAndGet();
            publish(new HistoryRecordedEvent(c == null ? event.id() : c.uuid(), event.traceId(), event.id(), event.type().name(), event.community(), event.significance()));
            added = true;
        }
        return added;
    }

    public List<HistoricalEvent> history(String communityId) { Community c = communities.get(communityId); return c == null ? List.of() : List.copyOf(c.history()); }
    public void loadTimeline(List<HistoricalEvent> events) { timeline.clear(); timeline.addAll(events); }

    /** A public event or a confirmed rumour changes what a community thinks of a person. */
    public void adjustStanding(String communityId, EntityRef subject, String label, double amount) {
        Community c = communities.get(communityId == null ? "" : communityId.toLowerCase(Locale.ROOT));
        if (c == null || subject == null) return;
        Map<String, Double> labels = c.standing().computeIfAbsent(subject.id(), k -> new LinkedHashMap<>());
        c.standingSubjects().put(subject.id(), subject);
        double before = labels.getOrDefault(label, 0.0D);
        labels.put(label, before + amount * (1.0D - before));
        c.markDirty();
    }

    // ------------------------------------------------------------------ rumours

    public RumorRecord createRumor(EntityRef origin, UUID originMemory, UUID traceId, RumorClaim claim, String communityId, long now) {
        KnowledgeSettings s = settings.get();
        if (rumors.size() >= s.maxRumors()) dropOldestRumor();
        RumorRecord r = rumorEngine.create(origin, originMemory, traceId, claim, communityId, now, 1.0D);
        rumors.put(r.id(), r);
        knowledge.metrics().rumorsCreated.incrementAndGet();
        publish(new RumorCreatedEvent(origin.id(), traceId, r.id(), originMemory, claim.subject().label(), claim.predicate() + (claim.object() == null ? "" : " " + claim.object().label()), r.strength()));
        return r;
    }

    private void dropOldestRumor() {
        RumorRecord oldest = null;
        for (RumorRecord r : rumors.values()) if (!r.open() && (oldest == null || r.lastSpread() < oldest.lastSpread())) oldest = r;
        if (oldest == null) for (RumorRecord r : rumors.values()) if (oldest == null || r.lastSpread() < oldest.lastSpread()) oldest = r;
        if (oldest != null) rumors.remove(oldest.id());
    }

    public Optional<RumorRecord> rumor(UUID id) { return Optional.ofNullable(rumors.get(id)); }
    public List<RumorRecord> rumors() { return List.copyOf(rumors.values()); }
    public void putRumor(RumorRecord r) { rumors.put(r.id(), r); }
    public int activeRumors() { int n = 0; for (RumorRecord r : rumors.values()) if (r.open()) n++; return n; }

    /** The evidence a rumour becomes for its holder: the claim as something heard, from the teller, with the rumour's provenance. */
    public KnowledgeEvidence evidenceFor(RumorRecord rumor, UUID listener, EntityRef teller, double credibility, long now) {
        RumorClaim claim = rumor.claim();
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("kind", claim.kind());
        attrs.put("magnitude", String.format(Locale.ROOT, "%.2f", claim.magnitude()));
        if (!claim.label().isEmpty()) attrs.put("label", claim.label());
        return new KnowledgeEvidence(listener, KnowledgeType.FACT, yadi.samuraiai.ai.knowledge.model.KnowledgeCategory.GENERAL, claim.subject(), claim.predicate(), claim.object(), attrs,
                LearnMethod.RUMOR, teller, credibility * 0.6D, claim.place(), Set.of("rumor"), null, rumor.traceId(), now, 0.8D,
                0.3D + 0.4D * claim.magnitude(), AccessLevel.PUBLIC, rumor.id());
    }

    /**
     * A rumour is confirmed or rejected (by direct evidence or a public event). Every other holder's belief is updated: a
     * confirmation counts as an independent, credible report; a rejection as a credible contradiction. The result never lifts a
     * holder's belief to "verified" on its own, only what the holder itself can back with direct evidence.
     */
    public void resolveRumor(UUID rumorId, boolean confirmed, UUID by, long now) {
        RumorRecord rumor = rumors.get(rumorId);
        if (rumor == null || !rumor.open()) return;
        rumorEngine.resolve(rumor, confirmed, by == null ? "" : by.toString(), now);
        knowledge.metrics().rumorsConfirmed.incrementAndGet();
        if (!confirmed) knowledge.metrics().rumorsRejected.incrementAndGet();
        publish(new RumorConfirmedEvent(by == null ? rumorId : by, rumor.traceId(), rumorId, confirmed, by == null ? "" : by.toString()));
        for (UUID holder : rumor.holders()) {
            if (holder.equals(by)) continue;
            KnowledgeRuntime rt = knowledge.peek(holder).orElse(null);
            if (rt == null) continue;
            for (UUID id : rt.rumors().contains(rumorId) ? findByRumor(rt, rumorId) : List.<UUID>of())
                knowledge.validateIn(rt, id, new ValidationEvidence(confirmed ? ValidationEvidence.Kind.INDEPENDENT_SOURCE : ValidationEvidence.Kind.CONTRADICTING_SOURCE, by, 0.9D, now, "rumor " + (confirmed ? "confirmed" : "rejected")));
        }
    }

    private static List<UUID> findByRumor(KnowledgeRuntime rt, UUID rumorId) {
        List<UUID> ids = new ArrayList<>();
        for (KnowledgeRecord r : rt.all()) if (rumorId.equals(r.rumorId())) ids.add(r.id());
        return ids;
    }

    // ------------------------------------------------------------------ propagation

    /**
     * Two NPCs are close enough to talk: the teller chooses what to pass on (knowledge, then open rumours it holds) and each telling is
     * queued with a delay. Rate-limited per pair. Returns how many tellings were queued.
     */
    public int gossip(UUID teller, UUID listener, double distance, SourceView view, long now) {
        KnowledgeSettings s = settings.get();
        String pair = teller + ">" + listener;
        Long last = gossiped.get(pair);
        if (last != null && now - last < s.gossipCooldownTicks()) return 0;
        KnowledgeRuntime from = knowledge.peek(teller).orElse(null), to = knowledge.runtime(listener);
        if (from == null) return 0;
        gossiped.put(pair, now);
        double trust01 = view.trust(listener, teller) / 100.0D;
        AccessLevel rank = sharedRank(teller, listener);
        int members = 0;
        String shared = "";
        for (String id : membership.getOrDefault(listener, Set.of())) if (membership.getOrDefault(teller, Set.of()).contains(id)) { shared = id; members = Math.max(members, communities.get(id) == null ? 0 : communities.get(id).members().size()); }
        int queued = 0;
        for (KnowledgeRecord r : propagation.select(from, to, rank, s)) {
            UUID item = r.rumorId() != null ? r.rumorId() : r.id();
            PropagationTask.Kind kind = r.rumorId() != null && rumors.containsKey(r.rumorId()) ? PropagationTask.Kind.RUMOR : PropagationTask.Kind.KNOWLEDGE;
            PropagationTask task = propagation.task(kind, teller, listener, item, now, distance, trust01, 0.3D + 0.7D * r.importance(), members, kind == PropagationTask.Kind.RUMOR ? rumors.get(item).hops().size() + 1 : 1, shared, s);
            if (queue.enqueue(task, s.queueMax())) queued++; else knowledge.metrics().queueDropped.set(queue.dropped());
        }
        if (gossiped.size() > 5000) gossiped.entrySet().removeIf(e -> now - e.getValue() > s.gossipCooldownTicks() * 4L);
        return queued;
    }

    /** Delivers the tellings that are due (within the per-tick budget). */
    public List<Delivery> deliverDue(long now, SourceView view) {
        KnowledgeSettings s = settings.get();
        List<Delivery> deliveries = new ArrayList<>();
        for (PropagationTask task : queue.drain(now, s.propagationBudget())) deliveries.add(deliver(task, now, view));
        return deliveries;
    }

    private Delivery deliver(PropagationTask task, long now, SourceView view) {
        KnowledgeSettings s = settings.get();
        try {
            KnowledgeRuntime from = knowledge.peek(task.from()).orElse(null);
            if (from == null) return new Delivery(task, false, null, null, false, 0, "teller gone");
            double credibility = Math.max(0.0D, Math.min(1.0D, (view.trust(task.to(), task.from()) / 100.0D * 0.55D) + view.reputation(task.to(), task.from()) * 0.25D + view.respect(task.to(), task.from()) / 100.0D * 0.2D));
            EntityRef teller = EntityRef.npc(task.from(), "");
            if (task.kind() == PropagationTask.Kind.RUMOR) {
                RumorRecord rumor = rumors.get(task.itemId());
                if (rumor == null) return new Delivery(task, false, null, null, false, credibility, "rumor gone");
                RumorEngine.Transfer transfer = rumorEngine.transfer(rumor, task.from(), task.to(), credibility, now, s);
                if (!transfer.accepted()) return new Delivery(task, false, rumor, null, false, credibility, "not believed or already known");
                knowledge.metrics().rumorsSpread.incrementAndGet();
                knowledge.metrics().rumorHops.incrementAndGet();
                publish(new RumorSpreadEvent(task.from(), rumor.traceId(), rumor.id(), task.to(), transfer.hop(), credibility, transfer.transformed()));
                LearnResult result = knowledge.learn(evidenceFor(rumor, task.to(), teller, credibility, now));
                return new Delivery(task, !result.rejected(), rumor, result.record(), transfer.transformed(), credibility, "");
            }
            KnowledgeRecord source = from.get(task.itemId());
            if (source == null) return new Delivery(task, false, null, null, false, credibility, "topic gone");
            KnowledgeEvidence evidence = new KnowledgeEvidence(task.to(), source.type(), source.category(), source.subject(), source.predicate(), source.object(), source.attributes(), LearnMethod.CONVERSATION,
                    teller, source.confidence() * (0.4D + 0.6D * credibility) * 0.75D, source.place(), source.tags(), null, source.traceId(), now, 0.8D,
                    source.importance(), source.access(), source.rumorId());
            LearnResult result = knowledge.learn(evidence);
            return new Delivery(task, !result.rejected(), null, result.record(), false, credibility, "");
        } catch (RuntimeException error) {
            knowledge.metrics().errors.incrementAndGet();
            return new Delivery(task, false, null, null, false, 0, error.toString());
        }
    }

    // ------------------------------------------------------------------ upkeep

    /** Slow society upkeep: traditions erode without observance, stale rumours are forgotten. Cheap; runs at its own interval. */
    public void tick(long now) {
        KnowledgeSettings s = settings.get();
        if (now - lastMaintenance < s.societyIntervalTicks()) return;
        long elapsed = lastMaintenance == Long.MIN_VALUE / 2 ? 0 : now - lastMaintenance;
        lastMaintenance = now;
        for (Community c : communities.values()) {
            for (Map.Entry<String, TraditionState> e : c.traditions().entrySet()) {
                TraditionState t = e.getValue();
                long dt = now - t.lastDecay();
                if (dt <= 0) continue;
                t.lastDecay(now);
                long idle = now - Math.max(t.lastObserved(), 0L);
                if (idle > Stamp.TICKS_PER_DAY) { t.strength(t.strength() - s.traditionDecayPerDay() * dt / Stamp.TICKS_PER_DAY); c.markDirty(); }
            }
        }
        for (RumorRecord r : rumorEngine.forgetStale(rumors.values(), now, s)) knowledge.metrics().rumorsForgotten.incrementAndGet();
        if (elapsed > 0 && rumors.size() > s.maxRumors()) dropOldestRumor();
    }

    public void reset() {
        communities.clear(); membership.clear(); rumors.clear(); timeline.clear(); worldMemory.clear(); queue.clear(); gossiped.clear();
    }

    public Map<UUID, Set<String>> membershipView() { return Map.copyOf(membership); }
}
