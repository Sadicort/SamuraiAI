package yadi.samuraiai.living.quest.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.event.NpcEvent;
import yadi.samuraiai.living.core.Dice;
import yadi.samuraiai.living.core.LivingIds;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.core.WorldClock;
import yadi.samuraiai.living.quest.api.QuestPorts;
import yadi.samuraiai.living.quest.branching.BranchSpec;
import yadi.samuraiai.living.quest.branching.Path;
import yadi.samuraiai.living.quest.campaigns.Campaign;
import yadi.samuraiai.living.quest.campaigns.CampaignCatalog;
import yadi.samuraiai.living.quest.chains.QuestChain;
import yadi.samuraiai.living.quest.conditions.ConditionKind;
import yadi.samuraiai.living.quest.conditions.WorldCondition;
import yadi.samuraiai.living.quest.consequences.ConsequenceSpec;
import yadi.samuraiai.living.quest.dialogue.QuestDialogue;
import yadi.samuraiai.living.quest.events.CampaignAdvancedEvent;
import yadi.samuraiai.living.quest.events.CampaignEndedEvent;
import yadi.samuraiai.living.quest.events.CampaignStartedEvent;
import yadi.samuraiai.living.quest.events.QuestCompletedEvent;
import yadi.samuraiai.living.quest.events.QuestConsequenceAppliedEvent;
import yadi.samuraiai.living.quest.events.QuestCreatedEvent;
import yadi.samuraiai.living.quest.events.QuestFailedEvent;
import yadi.samuraiai.living.quest.events.QuestRewardGivenEvent;
import yadi.samuraiai.living.quest.events.QuestUpdatedEvent;
import yadi.samuraiai.living.quest.generator.QuestGenerator;
import yadi.samuraiai.living.quest.memory.QuestHistory;
import yadi.samuraiai.living.quest.metrics.QuestMetrics;
import yadi.samuraiai.living.quest.objectives.ObjectiveType;
import yadi.samuraiai.living.quest.objectives.QuestObjective;
import yadi.samuraiai.living.quest.rewards.RewardSpec;
import yadi.samuraiai.living.quest.runtime.Quest;
import yadi.samuraiai.living.quest.templates.Expr;
import yadi.samuraiai.living.quest.templates.QuestTemplate;
import yadi.samuraiai.living.quest.templates.QuestTemplates;

/**
 * The Dynamic Quest Engine: "what stories and problems emerge from the real state of the world". Quests are consequences:
 * the hub reports world conditions (a scarcity, a lost caravan, an attack, a festival coming, a grateful NPC...), and the
 * engine decides whether each deserves a quest, picks a fitting template, binds it to real places and people and offers
 * it. It tracks objectives from real signals (conversations, places reached, enemies defeated, goods delivered, caravans
 * arriving, events ending), lets players choose a path, evolves quests when the world changes (twists, merges, resolution by
 * others), pays rewards from real treasuries and stores, applies consequences to the engines that own what they change, and
 * keeps campaigns, chains and history. No quest logic lives in an NPC.
 */
public final class QuestEngine {
    public record Result(boolean ok, String message, Quest quest) {
        static Result no(String m) { return new Result(false, m, null); }
        static Result yes(String m, Quest q) { return new Result(true, m, q); }
    }

    private final Supplier<QuestSettings> settingsSupplier;
    private final WorldClock clock;
    private final QuestMetrics metrics = new QuestMetrics();
    private final QuestHistory history = new QuestHistory();
    private final QuestChain chains = new QuestChain();
    private final QuestGenerator generator = new QuestGenerator();
    private final Map<UUID, Quest> quests = new LinkedHashMap<>();
    private final Map<String, UUID> byKey = new HashMap<>();
    private final Map<String, Long> cooldown = new HashMap<>();
    private final Map<UUID, Campaign> campaigns = new LinkedHashMap<>();
    private final Map<String, UUID> campaignByKey = new HashMap<>();
    private final Map<UUID, Set<UUID>> defenders = new HashMap<>();
    private final Map<UUID, Set<String>> talkedTo = new HashMap<>();
    private EventSink bus;
    private QuestSettings settings;
    private QuestTemplates templates;
    private Dice dice;
    private long seed, tickCount;
    private QuestPorts.World world;
    private QuestPorts.Villages villages;
    private QuestPorts.Economy economy;
    private QuestPorts.Social social;
    private QuestPorts.Families families;
    private QuestPorts.Chronicle chronicle = (m, c, t, d, s, x, p) -> { };
    private QuestPorts.Notifier notifier = (p, t) -> { };
    private boolean dirty;

    public QuestEngine(Supplier<QuestSettings> settings, EventSink bus, WorldClock clock, long seed) {
        this.settingsSupplier = Objects.requireNonNull(settings);
        this.bus = Objects.requireNonNull(bus);
        this.clock = Objects.requireNonNull(clock);
        this.seed = seed;
        this.dice = new Dice(seed);
        rebuild(settings.get());
    }

    private void rebuild(QuestSettings s) { settings = s; templates = new QuestTemplates(s.templates()); history.configure(s.historyPerPlayer(), s.historyWorld()); }
    private void refresh() { QuestSettings s = settingsSupplier.get(); if (s != settings) rebuild(s); }

    public void useEventSink(EventSink sink) { bus = Objects.requireNonNull(sink); }
    public void useWorld(QuestPorts.World w) { world = w; }
    public void useVillages(QuestPorts.Villages v) { villages = v; }
    public void useEconomy(QuestPorts.Economy e) { economy = e; }
    public void useSocial(QuestPorts.Social s) { social = s; }
    public void useFamilies(QuestPorts.Families f) { families = f; }
    public void useChronicle(QuestPorts.Chronicle c) { chronicle = c; }
    public void useNotifier(QuestPorts.Notifier n) { notifier = n; }
    public void useSeed(long s) { seed = s; dice = new Dice(s); }
    private void publish(NpcEvent e) { bus.publish(e); }
    private long now() { return clock.now(); }
    private boolean wired() { return world != null && villages != null && economy != null && social != null; }

    // ------------------------------------------------------------------ conditions → quests

    /**
     * A world condition, reported by the hub. Returns the quest it produced or changed. The same condition twice evolves the
     * existing quest (a twist when it got worse); a similar condition in the same place is merged into an open quest; a
     * condition that fits a campaign starts one.
     */
    public Optional<Quest> report(WorldCondition c) {
        refresh();
        metrics.conditions.incrementAndGet();
        if (!wired() || c.severity() < settings.minSeverity()) { metrics.ignored.incrementAndGet(); return Optional.empty(); }
        long now = now();
        UUID existing = byKey.get(c.key());
        if (existing != null) {
            Quest q = quests.get(existing);
            if (q != null && q.open()) { evolve(q, c); return Optional.of(q); }
        }
        if (now < cooldown.getOrDefault(c.key(), Long.MIN_VALUE)) { metrics.ignored.incrementAndGet(); return Optional.empty(); }
        List<QuestTemplate> candidates = templates.triggeredBy(c.kind());
        for (Quest q : quests.values()) {
            if (!q.open() || c.settlement() == null || !c.settlement().equals(q.settlement()) || q.originKind() != c.kind()) continue;
            String resource = c.variables().getOrDefault("resource", c.subject());
            if (!resource.equals(q.variables().getOrDefault("resource", ""))) continue;   // only the same problem (the same resource) is merged
            QuestTemplate t = templates.get(q.template()).orElse(null);
            if (t != null && t.mergeable()) { merge(q, c); byKey.put(c.key(), q.id()); return Optional.of(q); }
        }
        if (quests.values().stream().filter(Quest::open).count() >= settings.maxOpenQuests()) { metrics.ignored.incrementAndGet(); return Optional.empty(); }
        if (c.settlement() != null && quests.values().stream().filter(q -> q.open() && c.settlement().equals(q.settlement())).count() >= settings.maxOffersPerSettlement()) {
            metrics.ignored.incrementAndGet(); return Optional.empty();
        }
        if (settings.campaigns()) {
            Optional<CampaignCatalog.Definition> def = CampaignCatalog.forCondition(c.kind());
            if (def.isPresent() && !campaignByKey.containsKey(c.key())) {
                Optional<Quest> first = templates.get(def.get().stages().get(0)).flatMap(t -> create(t, c));
                first.ifPresent(q -> startCampaign(def.get(), c, q));
                if (first.isPresent()) return first;
            }
        }
        String culture = c.settlement() == null ? null : villages.culture(c.settlement());
        Optional<QuestTemplate> template = generator.choose(candidates, c, world.seasonName(), culture, dice, now);
        if (template.isEmpty()) { metrics.ignored.incrementAndGet(); return Optional.empty(); }
        return create(template.get(), c);
    }

    private Optional<Quest> create(QuestTemplate t, WorldCondition c) {
        Optional<Quest> made = generator.instantiate(t, c, new QuestGenerator.Ports(world, villages, economy), now(), clock.minutesPerDay());
        if (made.isEmpty()) { metrics.ignored.incrementAndGet(); return Optional.empty(); }
        Quest q = made.get();
        quests.put(q.id(), q);
        byKey.put(c.key(), q.id());
        cooldown.put(c.key(), now() + settings.cooldownMinutes());
        metrics.generated.incrementAndGet();
        dirty = true;
        publish(new QuestCreatedEvent(now(), q.id(), t.id(), q.title(), c.kind().name(), q.settlement(), q.giver()));
        return Optional.of(q);
    }

    private void evolve(Quest q, WorldCondition c) {
        double before = q.severity();
        q.severity(Math.max(before, c.severity()));
        if (q.state() == Quest.State.ACTIVE && c.severity() - before >= settings.twistThreshold() && q.stage() != QuestTemplate.StoryStage.TWIST) {
            q.stage(QuestTemplate.StoryStage.TWIST);
            for (QuestObjective o : q.relevant()) if ((o.type() == ObjectiveType.DELIVER || o.type() == ObjectiveType.COMBAT) && !o.done()) o.required(Math.ceil(o.required() * 1.5D));
            metrics.twists.incrementAndGet();
            dirty = true;
            publish(new QuestUpdatedEvent(now(), q.id(), "TWIST", q.text()));
            for (UUID p : q.players().keySet()) notifier.tell(p, "Giro en «" + q.title() + "»: " + q.text());
        }
    }

    private void merge(Quest q, WorldCondition c) {
        double add = c.variables().containsKey("quantity") ? Expr.number(c.variables().get("quantity"), Map.of()) : Math.round(10 + c.severity() * 30);
        for (QuestObjective o : q.objectives()) if (o.type() == ObjectiveType.DELIVER && !o.done()) { o.required(o.required() + add); break; }
        q.severity(Math.max(q.severity(), c.severity()));
        q.mergedOnce();
        q.log("fusionada con " + c.key());
        metrics.merges.incrementAndGet();
        dirty = true;
        publish(new QuestUpdatedEvent(now(), q.id(), "MERGED", c.key()));
    }

    /** The condition behind a quest went away (the caravan arrived, the attack ended...): open quests evolve accordingly. */
    public void conditionResolved(String key) {
        UUID id = byKey.remove(key);
        Quest q = id == null ? null : quests.get(id);
        if (q == null || !q.open() || !q.originKey().equals(key)) return;   // a merged condition going away does not end the quest
        if (q.state() == Quest.State.ACTIVE && q.progressShare() >= 0.5D) {
            q.log("otros terminaron lo que empezaste");
            complete(q, 0.5D);
        } else fail(q, Quest.State.RESOLVED_BY_WORLD, "el problema se resolvió sin ayuda");
    }

    // ------------------------------------------------------------------ players

    public Result accept(UUID questId, UUID player, String name) {
        Quest q = quests.get(questId);
        if (q == null) return Result.no("No existe esa misión.");
        if (q.state() != Quest.State.OFFERED && !(q.state() == Quest.State.ACTIVE && !q.players().containsKey(player))) return Result.no("Esa misión ya no está disponible.");
        if (q.reservedFor() != null && !q.reservedFor().equals(player)) return Result.no("Esa misión es para otra persona.");
        if (q.state() == Quest.State.OFFERED && now() > q.offeredUntil()) { fail(q, Quest.State.EXPIRED, "nadie aceptó a tiempo"); return Result.no("Esa misión ya no está disponible."); }
        QuestTemplate t = templates.get(q.template()).orElse(null);
        double trust = social.trust(q.giver(), player);
        if (t != null && trust < t.minTrust()) { metrics.refusals.incrementAndGet(); return Result.no(QuestDialogue.refusal(q)); }
        boolean first = q.state() == Quest.State.OFFERED;
        q.players().put(player, name);
        if (first) {
            q.state(Quest.State.ACTIVE);
            q.stage(QuestTemplate.StoryStage.DEVELOPMENT);
            q.accepted(now(), clock.minutesPerDay());
            for (QuestObjective o : q.objectives()) if (o.branch() == null) o.state(QuestObjective.State.ACTIVE);
            List<BranchSpec> offered = paths(q, player);
            if (q.branches().size() == 1) choose(questId, player, q.branches().get(0).path());
            else if (!q.branches().isEmpty() && offered.size() == 1) choose(questId, player, offered.get(0).path());
            metrics.accepted.incrementAndGet();
        }
        history.accepted(q, player);
        dirty = true;
        publish(new QuestUpdatedEvent(now(), q.id(), "ACCEPTED", name));
        return Result.yes(QuestDialogue.offer(q, social.mood(q.giver()), trust), q);
    }

    /** The paths this player may take (trust of the giver, standing in the settlement). */
    public List<BranchSpec> paths(Quest q, UUID player) {
        List<BranchSpec> out = new ArrayList<>();
        double trust = social.trust(q.giver(), player);
        double standing = q.settlement() == null ? 0 : social.standing(player, q.settlement(), "village");
        for (BranchSpec b : q.branches()) if (trust >= b.minTrust() && standing >= b.minStanding()) out.add(b);
        return out;
    }

    public Result choose(UUID questId, UUID player, Path path) {
        Quest q = quests.get(questId);
        if (q == null || q.state() != Quest.State.ACTIVE) return Result.no("No tienes esa misión activa.");
        if (q.path() != null) return Result.no("Ya elegiste: " + q.path().label());
        BranchSpec branch = null;
        for (BranchSpec b : paths(q, player)) if (b.path() == path) branch = b;
        if (branch == null) return Result.no("Ese camino no está abierto para ti.");
        q.path(path);
        q.decisions().add(new Quest.Decision(now(), player, path.name() + ": " + branch.label()));
        for (QuestObjective o : q.objectives()) {
            if (o.branch() == path) o.state(QuestObjective.State.ACTIVE);
            else if (o.branch() != null) o.state(QuestObjective.State.SKIPPED);
        }
        metrics.path(path);
        dirty = true;
        publish(new QuestUpdatedEvent(now(), q.id(), "PATH", path.name()));
        checkDone(q);
        return Result.yes("Has elegido la vía " + path.label() + ": " + branch.label(), q);
    }

    public Result abandon(UUID questId, UUID player) {
        Quest q = quests.get(questId);
        if (q == null || !q.players().containsKey(player) || !q.open()) return Result.no("No tienes esa misión.");
        q.players().remove(player);
        if (q.players().isEmpty()) {
            if (q.settlement() != null) social.adjustStanding(player, "", q.settlement(), "village", -0.03D);
            q.players().put(player, "");
            fail(q, Quest.State.ABANDONED, "abandonada");
        }
        dirty = true;
        return Result.yes("Has abandonado «" + q.title() + "».", q);
    }

    // ------------------------------------------------------------------ signals from the world

    private List<Quest> activeFor(UUID player) {
        List<Quest> out = new ArrayList<>();
        for (Quest q : quests.values()) if (q.state() == Quest.State.ACTIVE && q.players().containsKey(player)) out.add(q);
        return out;
    }

    /** A player talked to an NPC (by chat or right-click). {@code role} is the NPC's profession. */
    public int talked(UUID player, UUID npc, String role) {
        int n = 0;
        for (Quest q : activeFor(player))
            for (QuestObjective o : q.relevant()) {
                if (!o.active() || o.type() != ObjectiveType.TALK) continue;
                boolean match = o.target().equals(npc.toString()) || o.target().equals(role) || (o.target().equals("priest") && "monk".equals(role)) || o.target().equals("citizen")
                        || (o.target().equals("giver") && npc.equals(q.giver()));
                if (!match) continue;
                Set<String> seen = talkedTo.computeIfAbsent(o.id(), k -> new HashSet<>());
                if (!seen.add(npc.toString())) continue;
                progress(q, o, 1);
                n++;
            }
        return n;
    }

    /** A player is at a place: investigation objectives there are done; a defender is noted. */
    public int arrived(UUID player, String dimension, double x, double y, double z) {
        int n = 0;
        for (Quest q : activeFor(player))
            for (QuestObjective o : q.relevant()) {
                if (!o.active() || !o.near(dimension, x, y, z)) continue;
                if (o.type() == ObjectiveType.INVESTIGATE || o.type() == ObjectiveType.FOLLOW) { progress(q, o, o.remaining()); n++; }
                else if (o.type() == ObjectiveType.DEFEND) defenders.computeIfAbsent(q.id(), k -> new LinkedHashSet<>()).add(player);
            }
        return n;
    }

    /** A player spent {@code minutes} in stillness at a place (meditating). */
    public int meditated(UUID player, String dimension, double x, double y, double z, double minutes) {
        int n = 0;
        for (Quest q : activeFor(player))
            for (QuestObjective o : q.relevant()) if (o.active() && o.type() == ObjectiveType.MEDITATE && o.near(dimension, x, y, z)) { progress(q, o, minutes); n++; }
        return n;
    }

    /** A player killed something hostile (or a creature of {@code kind}) at a place. */
    public int killed(UUID player, String kind, boolean hostile, String dimension, double x, double y, double z) {
        int n = 0;
        for (Quest q : activeFor(player))
            for (QuestObjective o : q.relevant()) {
                if (!o.active() || o.type() != ObjectiveType.COMBAT) continue;
                boolean match = (o.target().equals("hostile") && hostile) || o.target().equalsIgnoreCase(kind);
                if (match && (!o.placed() || o.near(dimension, x, y, z))) { progress(q, o, 1); n++; }
            }
        return n;
    }

    /** A player delivered goods in a settlement (the hub already stored them). Returns how much counted towards quests. */
    public double delivered(UUID player, UUID settlement, String resource, double quantity) {
        double counted = 0;
        for (Quest q : activeFor(player))
            for (QuestObjective o : q.relevant()) {
                if (!o.active() || (o.type() != ObjectiveType.DELIVER && o.type() != ObjectiveType.GATHER) || quantity - counted <= 1e-9) continue;
                if (settlement != null && q.settlement() != null && !settlement.equals(q.settlement()) && !"destination".equals(o.target())) continue;
                boolean match = o.target().equalsIgnoreCase(resource) || ("food".equals(o.target()) && economy.isFood(resource));
                if (!match) continue;
                double use = Math.min(o.remaining(), quantity - counted);
                counted += use;
                progress(q, o, use);
            }
        return counted;
    }

    /** A caravan from {@code origin} reached {@code destination}; escorts with a player near are done. */
    public int caravanArrived(UUID origin, UUID destination, Set<UUID> playersNear) {
        int n = 0;
        for (Quest q : List.copyOf(quests.values())) {
            if (q.state() != Quest.State.ACTIVE || !origin.equals(q.settlement())) continue;
            for (QuestObjective o : q.relevant())
                if (o.active() && o.type() == ObjectiveType.ESCORT && playersNear.stream().anyMatch(q.players()::containsKey)) { progress(q, o, o.remaining()); n++; }
        }
        return n;
    }

    /** A caravan from a settlement was ambushed: escort quests there take a twist. */
    public void caravanAmbushed(UUID origin) {
        for (Quest q : quests.values())
            if (q.state() == Quest.State.ACTIVE && origin.equals(q.settlement()) && q.relevant().stream().anyMatch(o -> o.type() == ObjectiveType.ESCORT && o.active())) {
                q.stage(QuestTemplate.StoryStage.TWIST);
                metrics.twists.incrementAndGet();
                publish(new QuestUpdatedEvent(now(), q.id(), "TWIST", q.text()));
            }
    }

    /** A world event ended: defend objectives are won by those who were there, or the quest fails. */
    public void worldEventEnded(UUID event, boolean success) {
        for (Quest q : List.copyOf(quests.values())) {
            if (!q.open()) continue;
            for (QuestObjective o : q.relevant()) {
                if (o.type() != ObjectiveType.DEFEND || !o.target().equals(event.toString())) continue;
                if (q.state() == Quest.State.OFFERED) { fail(q, success ? Quest.State.RESOLVED_BY_WORLD : Quest.State.FAILED, success ? "otros defendieron la aldea" : "la aldea cayó"); break; }
                if (!success) { fail(q, Quest.State.FAILED, "la aldea cayó"); break; }
                if (defenders.getOrDefault(q.id(), Set.of()).stream().anyMatch(q.players()::containsKey)) progress(q, o, o.remaining());
                else fail(q, Quest.State.RESOLVED_BY_WORLD, "la defensa se hizo sin ti");
                break;
            }
        }
    }

    private void progress(Quest q, QuestObjective o, double amount) {
        if (!o.advance(amount)) { dirty = true; publish(new QuestUpdatedEvent(now(), q.id(), "PROGRESS", o.description())); return; }
        dirty = true;
        publish(new QuestUpdatedEvent(now(), q.id(), "OBJECTIVE_DONE", o.description()));
        for (UUID p : q.players().keySet()) notifier.tell(p, "✔ " + o.description());
        checkDone(q);
    }

    private void checkDone(Quest q) { if (q.state() == Quest.State.ACTIVE && (q.path() != null || q.branches().isEmpty()) && q.requiredDone()) complete(q, 1.0D); }

    // ------------------------------------------------------------------ endings

    private String playerNames(Quest q) { return String.join(", ", q.players().values().stream().filter(n -> !n.isEmpty()).toList()); }

    private void complete(Quest q, double factor) {
        long now = now();
        q.state(Quest.State.COMPLETED);
        q.stage(QuestTemplate.StoryStage.ENDING);
        q.ended(now);
        metrics.completed.incrementAndGet();
        metrics.durationMinutes.addAndGet(Math.max(0, now - q.acceptedAt()));
        q.variables().put("player", playerNames(q).isEmpty() ? "un forastero" : playerNames(q));
        String mood = social.mood(q.giver());
        double pathFactor = 1.0D;
        for (BranchSpec b : q.branches()) if (b.path() == q.path()) pathFactor = b.rewardFactor();
        double moodFactor = mood == null ? 1.0D : switch (mood) { case "HAPPY", "HOPEFUL", "INSPIRED" -> 1.1D; case "ANGRY" -> 0.9D; default -> 1.0D; };
        for (UUID player : q.players().keySet()) giveRewards(q, player, factor * pathFactor * moodFactor * settings.rewardScale());
        applyConsequences(q, true);
        q.stage(QuestTemplate.StoryStage.CONSEQUENCES);
        history.record(q, now);
        cooldown.put(q.originKey(), now + settings.cooldownMinutes());
        byKey.remove(q.originKey(), q.id());
        dirty = true;
        publish(new QuestCompletedEvent(now, q.id(), q.title(), q.path() == null ? "" : q.path().name(), Set.copyOf(q.players().keySet())));
        for (UUID p : q.players().keySet()) notifier.tell(p, QuestDialogue.thanks(q, mood));
        advanceCampaign(q, true);
        followUp(q);
    }

    private void fail(Quest q, Quest.State state, String reason) {
        long now = now();
        q.state(state);
        q.stage(QuestTemplate.StoryStage.ENDING);
        q.ended(now);
        q.log(reason);
        switch (state) {
            case FAILED -> metrics.failed.incrementAndGet();
            case EXPIRED -> metrics.expired.incrementAndGet();
            case ABANDONED -> metrics.abandoned.incrementAndGet();
            case RESOLVED_BY_WORLD -> metrics.resolvedByWorld.incrementAndGet();
            default -> { }
        }
        q.variables().put("player", playerNames(q).isEmpty() ? "nadie" : playerNames(q));
        if (state == Quest.State.FAILED || state == Quest.State.EXPIRED && !q.players().isEmpty() || state == Quest.State.ABANDONED) applyConsequences(q, false);
        if (state == Quest.State.EXPIRED && q.players().isEmpty() && q.originKind() != ConditionKind.CUSTOM) applyConsequences(q, false);   // nobody helped: the problem runs its course
        history.record(q, now);
        byKey.remove(q.originKey(), q.id());
        dirty = true;
        publish(new QuestFailedEvent(now, q.id(), q.title(), state.name(), reason));
        for (UUID p : q.players().keySet()) if (state != Quest.State.RESOLVED_BY_WORLD) notifier.tell(p, QuestDialogue.failure(q));
        advanceCampaign(q, false);
    }

    private void giveRewards(Quest q, UUID player, double factor) {
        String name = q.players().getOrDefault(player, "");
        for (RewardSpec r : q.rewards()) {
            double amount = Expr.number(r.amount(), Map.of()) * factor;
            String detail = "";
            switch (r.kind()) {
                case COINS -> { double paid = q.settlement() == null ? 0 : economy.reward(q.settlement(), player, name, amount, "recompensa: " + q.title()); detail = String.format("%.0f monedas", paid); }
                case ITEMS -> { double got = q.settlement() == null ? 0 : economy.giveItems(q.settlement(), player, r.target(), Math.max(1, Math.round(amount)), "recompensa: " + q.title()); detail = String.format("%.0f %s", got, r.target()); }
                case REPUTATION -> { if (q.settlement() != null) social.adjustStanding(player, name, q.settlement(), r.target().toLowerCase(java.util.Locale.ROOT), amount); detail = r.target() + String.format(" %+.2f", amount); }
                case KNOWLEDGE -> { notifier.tell(player, "Aprendes: " + r.text()); detail = r.text(); }
                case RELATIONSHIP -> { social.experience(q.giver(), player, name, "HELPED_ME", q.title()); detail = "gratitud de " + q.giverName(); }
                case TITLE -> detail = "título (reservado para el futuro)";
            }
            q.given().add(detail);
            metrics.rewards.incrementAndGet();
            publish(new QuestRewardGivenEvent(now(), q.id(), player, r.kind().name(), detail));
        }
    }

    private void applyConsequences(Quest q, boolean success) {
        long now = now();
        String players = q.variables().getOrDefault("player", "");
        UUID anyPlayer = q.players().isEmpty() ? null : q.players().keySet().iterator().next();
        for (ConsequenceSpec c : q.consequences()) {
            if (success ? !c.onSuccess() : !c.onFailure()) continue;
            if (c.branch() != null && c.branch() != q.path()) continue;
            double amount = Expr.number(c.amount(), Map.of());
            String text = Expr.fill(c.text(), q.variables());
            String detail = text;
            try {
                switch (c.kind()) {
                    case RENOWN -> { if (q.settlement() != null) villages.renown(q.settlement(), amount, text); }
                    case UNREST -> { if (q.settlement() != null) villages.unrest(q.settlement(), amount); }
                    case REPUTATION -> { for (var p : q.players().entrySet()) social.adjustStanding(p.getKey(), p.getValue(), q.settlement(), c.target().toLowerCase(java.util.Locale.ROOT), amount); }
                    case RELATIONSHIP -> { for (var p : q.players().entrySet()) social.experience(q.giver(), p.getKey(), p.getValue(), amount >= 0 ? "HELPED_ME" : "BETRAYED", text); }
                    case MEMORY -> { if (anyPlayer != null && q.settlement() != null) social.witnesses(q.settlement(), anyPlayer, players, c.target(), text); }
                    case HISTORY -> {
                        Set<String> scopes = new LinkedHashSet<>();
                        if (q.settlement() != null) { scopes.add("settlement:" + q.settlement()); scopes.add("village:" + q.settlement()); }
                        if (q.region() != null) scopes.add("region:" + q.region());
                        scopes.add("quest:" + q.id());
                        for (UUID p : q.players().keySet()) scopes.add("player:" + p);
                        chronicle.record(now, c.target(), text, q.title(), scopes, amount, Provenance.of("quest", q.id().toString(), q.title(), now));
                    }
                    case RESOLVE_EVENT -> {
                        String id = q.variables().get(c.target());
                        if (id != null) detail = world.resolveEvent(UUID.fromString(id), true, players, text) ? "evento resuelto" : "el evento ya había terminado";
                    }
                    case SPAWN_EVENT -> world.spawnEvent(c.target(), q.region(), q.settlement(), amount, text).ifPresent(id -> q.log("provoca el evento " + id));
                    case LOOT -> { if (q.settlement() != null) economy.loot(q.settlement(), amount, text); }
                    case BUILD_HOUSE -> { if (q.settlement() != null) detail = villages.buildHouse(q.settlement()) ? "casa nueva" : "no se pudo construir"; }
                    case BUILD_ROAD -> {
                        String id = q.variables().getOrDefault(c.target(), q.variables().get("destination"));
                        if (id != null && q.settlement() != null) detail = world.buildRoad(q.settlement(), UUID.fromString(id)) ? "nueva senda" : "sin senda";
                    }
                    case FAMILY_HONOR -> { if (families != null) families.honor(subject(q, c.target()), amount, text); }
                    case FAMILY_MEMORY -> { if (families != null) families.memory(subject(q, c.target()), text); }
                    case MENTORSHIP -> {
                        if (families != null && q.variables().containsKey("masterId") && q.variables().containsKey("discipleId"))
                            families.mentorship(UUID.fromString(q.variables().get("masterId")), UUID.fromString(q.variables().get("discipleId")), amount);
                    }
                }
            } catch (RuntimeException error) { detail = "no aplicada: " + error.getMessage(); }
            q.applied().add(c.kind() + ": " + detail);
            metrics.consequences.incrementAndGet();
            publish(new QuestConsequenceAppliedEvent(now, q.id(), c.kind().name(), c.target(), detail));
        }
    }

    private static String subject(Quest q, String target) {
        return switch (target) {
            case "giver" -> "npc:" + q.giver();
            case "family" -> q.variables().containsKey("family") ? "family:" + q.variables().get("family") : "npc:" + q.giver();
            default -> target;
        };
    }

    // ------------------------------------------------------------------ campaigns and chains

    private void startCampaign(CampaignCatalog.Definition def, WorldCondition c, Quest first) {
        Campaign camp = new Campaign(UUID.randomUUID(), def.type(), Expr.fill(def.title(), first.variables()), def.stages(), c.key(), c.settlement(), now());
        camp.quests().add(first.id());
        campaigns.put(camp.id(), camp);
        campaignByKey.put(c.key(), camp.id());
        first.campaign(camp.id(), 0);
        metrics.campaigns.incrementAndGet();
        publish(new CampaignStartedEvent(now(), camp.id(), def.type().name(), camp.title()));
    }

    private void advanceCampaign(Quest q, boolean success) {
        if (q.campaign() == null) return;
        Campaign camp = campaigns.get(q.campaign());
        if (camp == null || camp.state() != Campaign.State.ACTIVE) return;
        if (!success) { camp.state(Campaign.State.FAILED, now()); publish(new CampaignEndedEvent(now(), camp.id(), "FAILED")); dirty = true; return; }
        if (camp.last()) {
            camp.state(Campaign.State.COMPLETED, now());
            chronicle.record(now(), "QUEST", camp.title() + ": concluida", "campaña de " + camp.stages().size() + " etapas",
                    q.settlement() == null ? Set.of("world") : Set.of("settlement:" + q.settlement()), 0.6D, Provenance.of("campaign", camp.id().toString(), camp.title(), now()));
            publish(new CampaignEndedEvent(now(), camp.id(), "COMPLETED"));
            dirty = true;
            return;
        }
        camp.advance();
        camp.player(q.players().keySet().stream().findFirst().orElse(null));
        String next = camp.currentTemplate();
        Map<String, String> vars = new LinkedHashMap<>(q.variables());
        vars.remove("giver"); vars.remove("giverName");
        WorldCondition c = new WorldCondition(q.originKind(), q.originKey() + "#" + camp.stage(), q.settlement(), q.region(), vars.getOrDefault("resource", ""), q.severity(), q.cause(), vars);
        templates.get(next).flatMap(t -> create(t, c)).ifPresent(n -> {
            n.campaign(camp.id(), camp.stage());
            n.reservedFor(camp.player());
            camp.quests().add(n.id());
            publish(new CampaignAdvancedEvent(now(), camp.id(), camp.stage(), next));
            if (camp.player() != null) notifier.tell(camp.player(), "La historia continúa: " + n.title());
        });
        dirty = true;
    }

    private void followUp(Quest q) {
        if (!settings.chains()) return;
        QuestTemplate t = templates.get(q.template()).orElse(null);
        if (t == null || t.followUp().isEmpty()) return;
        Map<String, String> vars = new LinkedHashMap<>(q.variables());
        vars.put("giver", q.giver().toString());
        vars.put("giverName", q.giverName());
        vars.put("giverProfession", q.giverProfession());
        WorldCondition c = new WorldCondition(ConditionKind.CUSTOM, "chain:" + q.id(), q.settlement(), q.region(), q.variables().getOrDefault("resource", ""), 1.0D,
                Provenance.of("quest", q.id().toString(), "continuación", now()), vars);
        templates.get(t.followUp()).flatMap(next -> create(next, c)).ifPresent(n -> {
            n.parent(q.id());
            n.reservedFor(q.players().keySet().stream().findFirst().orElse(null));
            chains.link(q.id(), n.id());
            metrics.chains.incrementAndGet();
        });
    }

    // ------------------------------------------------------------------ time

    /** Offers and deadlines expire; old finished quests are dropped (their history stays). */
    public void tick() {
        refresh();
        tickCount++;
        if (tickCount % 20 != 0) return;
        long now = now();
        for (Quest q : List.copyOf(quests.values())) {
            if (q.state() == Quest.State.OFFERED && now > q.offeredUntil()) fail(q, Quest.State.EXPIRED, "nadie aceptó a tiempo");
            else if (q.state() == Quest.State.ACTIVE && now > q.deadline()) fail(q, Quest.State.EXPIRED, "se acabó el tiempo");
        }
        quests.values().removeIf(q -> !q.open() && now - q.endedAt() > 30L * clock.minutesPerDay());
        cooldown.values().removeIf(t -> t < now);
    }

    // ------------------------------------------------------------------ queries

    public Optional<Quest> quest(UUID id) { return Optional.ofNullable(quests.get(id)); }
    public Optional<Quest> find(String prefix) { for (Quest q : quests.values()) if (q.id().toString().startsWith(prefix)) return Optional.of(q); return Optional.empty(); }
    public Collection<Quest> quests() { return List.copyOf(quests.values()); }
    public List<Quest> open() { return quests.values().stream().filter(Quest::open).toList(); }
    public List<Quest> offeredIn(UUID settlement) { return quests.values().stream().filter(q -> q.state() == Quest.State.OFFERED && settlement.equals(q.settlement())).toList(); }
    public List<Quest> active(UUID player) { return activeFor(player); }
    public List<Quest> givenBy(UUID npc) { return quests.values().stream().filter(q -> q.open() && npc.equals(q.giver())).toList(); }
    public Collection<Campaign> campaigns() { return List.copyOf(campaigns.values()); }
    public QuestTemplates templates() { return templates; }
    public QuestHistory history() { return history; }
    public QuestChain chains() { return chains; }
    public QuestMetrics metrics() { return metrics; }
    public QuestSettings settings() { return settings; }
    public WorldClock clock() { return clock; }
    public Map<String, Long> cooldowns() { return Map.copyOf(cooldown); }
    public Map<UUID, Set<UUID>> defenders() { return defenders; }
    public long seed() { return seed; }
    public boolean dirty() { return dirty || history.dirty(); }
    public void clean() { dirty = false; history.clean(); }

    public void restoreQuest(Quest q) { quests.put(q.id(), q); if (q.open()) byKey.put(q.originKey(), q.id()); }
    public void restoreCampaign(Campaign c) { campaigns.put(c.id(), c); if (c.state() == Campaign.State.ACTIVE) campaignByKey.put(c.originKey(), c.id()); }
    public void restoreCooldown(String key, long until) { cooldown.put(key, until); }

    public void reset() {
        quests.clear(); byKey.clear(); cooldown.clear(); campaigns.clear(); campaignByKey.clear(); defenders.clear(); talkedTo.clear(); history.clear(); chains.clear();
        metrics.reset(); tickCount = 0; dirty = false;
    }

    public static String keyOf(ConditionKind kind, UUID settlement, String subject) { return kind.name().toLowerCase(java.util.Locale.ROOT) + ":" + LivingIds.shortId(settlement) + ":" + subject; }
}
