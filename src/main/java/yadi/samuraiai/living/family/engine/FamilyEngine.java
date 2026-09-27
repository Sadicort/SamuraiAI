package yadi.samuraiai.living.family.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import yadi.samuraiai.living.family.aging.AgeEngine;
import yadi.samuraiai.living.family.aging.LifeStage;
import yadi.samuraiai.living.family.clan.ClanRecord;
import yadi.samuraiai.living.family.events.ArtifactNamedEvent;
import yadi.samuraiai.living.family.events.BirthRegisteredEvent;
import yadi.samuraiai.living.family.events.ClanCreatedEvent;
import yadi.samuraiai.living.family.events.ClanDisbandedEvent;
import yadi.samuraiai.living.family.events.ClanJoinedEvent;
import yadi.samuraiai.living.family.events.ClanLeaderChangedEvent;
import yadi.samuraiai.living.family.events.ClanLeftEvent;
import yadi.samuraiai.living.family.events.EpithetGrantedEvent;
import yadi.samuraiai.living.family.events.HouseTitleGrantedEvent;
import yadi.samuraiai.living.family.events.FamilyBranchCreatedEvent;
import yadi.samuraiai.living.family.events.FamilyCreatedEvent;
import yadi.samuraiai.living.family.events.FamilyExtinctEvent;
import yadi.samuraiai.living.family.events.FamilyHeadChangedEvent;
import yadi.samuraiai.living.family.events.FamilyMemberJoinedEvent;
import yadi.samuraiai.living.family.events.FamilyMigratedEvent;
import yadi.samuraiai.living.family.events.FamilyReputationChangedEvent;
import yadi.samuraiai.living.family.events.GenerationAdvancedEvent;
import yadi.samuraiai.living.family.events.HeirloomLostEvent;
import yadi.samuraiai.living.family.events.HeirloomTransferredEvent;
import yadi.samuraiai.living.family.events.InheritanceCompletedEvent;
import yadi.samuraiai.living.family.events.LegacyCreatedEvent;
import yadi.samuraiai.living.family.events.LifeStageChangedEvent;
import yadi.samuraiai.living.family.events.LifeStateChangedEvent;
import yadi.samuraiai.living.family.events.LineageLeaderChangedEvent;
import yadi.samuraiai.living.family.events.MentorshipEndedEvent;
import yadi.samuraiai.living.family.events.MentorshipStartedEvent;
import yadi.samuraiai.living.family.events.TechniqueLostEvent;
import yadi.samuraiai.living.family.events.TechniqueTaughtEvent;
import yadi.samuraiai.living.family.events.TraditionEmergedEvent;
import yadi.samuraiai.living.family.family_memory.FamilyMemoryEntry;
import yadi.samuraiai.living.family.genealogy.ConsistencyValidator;
import yadi.samuraiai.living.family.genealogy.GenealogyGraph;
import yadi.samuraiai.living.family.heritage.Heirloom;
import yadi.samuraiai.living.family.household.Household;
import yadi.samuraiai.living.family.inheritance.InheritanceRecord;
import yadi.samuraiai.living.family.integration.FamilyPorts;
import yadi.samuraiai.living.family.kinship.KinshipEngine;
import yadi.samuraiai.living.family.knowledge.Technique;
import yadi.samuraiai.living.family.legacy.LegacyRecord;
import yadi.samuraiai.living.family.lifecycle.LifeState;
import yadi.samuraiai.living.family.lineage.Lineage;
import yadi.samuraiai.living.family.mentorship.Mentorship;
import yadi.samuraiai.living.family.metrics.FamilyMetrics;
import yadi.samuraiai.living.family.model.KinGender;
import yadi.samuraiai.living.family.model.Person;
import yadi.samuraiai.living.family.naming.NameRecord;
import yadi.samuraiai.living.family.naming.NamingEngine;
import yadi.samuraiai.living.family.naming.cultures.NameCulture;
import yadi.samuraiai.living.family.naming.cultures.NameCultureCatalog;
import yadi.samuraiai.living.family.naming.cultures.NameCultureProfile;
import yadi.samuraiai.living.family.naming.epithets.EpithetCatalog;
import yadi.samuraiai.living.family.naming.epithets.EpithetCategory;
import yadi.samuraiai.living.family.parenthood.BirthRecord;
import yadi.samuraiai.living.family.profession.ProfessionHeritage;
import yadi.samuraiai.living.family.registry.FamilyRecord;
import yadi.samuraiai.living.family.reputation.CauseLedger;
import yadi.samuraiai.living.family.succession.SuccessionEngine;
import yadi.samuraiai.living.family.traditions.TraditionDetector;

/**
 * The Family, Lineage & Legacy Engine: "where does this NPC come from and what will it leave behind". It owns families,
 * kinship (a genealogy graph with two canonical edges), generations, households, family heads and succession, lineages
 * (families, samurai schools, crafts, religious and merchant lines), masters and disciples, the knowledge families and schools
 * keep (and lose), inheritances, heirlooms, family reputation and honour with their causes, family memory and history,
 * traditions, names, ages and life states, and legacy.
 *
 * <p>It is not the Relationship Engine (a parent is kinship; whether they love each other is the Relationship Engine's), not
 * the Memory Engine (a child never remembers what happened before it was born; stories are <i>taught</i> as knowledge), and
 * not the Economy (it asks for coins, it does not keep them). Pure Java.
 */
public final class FamilyEngine {
    private final Supplier<FamilySettings> settingsSupplier;
    private final WorldClock clock;
    private final FamilyMetrics metrics = new FamilyMetrics();
    private final GenealogyGraph graph = new GenealogyGraph();
    private final ProfessionHeritage heritage = new ProfessionHeritage();
    private final TraditionDetector traditions = new TraditionDetector();
    private final Map<UUID, Person> people = new LinkedHashMap<>();
    private final Map<UUID, FamilyRecord> families = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> members = new HashMap<>();
    private final Map<UUID, Household> households = new LinkedHashMap<>();
    private final Map<UUID, UUID> householdByHome = new HashMap<>();
    private final Map<UUID, Lineage> lineages = new LinkedHashMap<>();
    private final Map<UUID, Mentorship> mentorships = new LinkedHashMap<>();
    private final Map<String, Technique> techniques = new LinkedHashMap<>();
    private final Map<UUID, InheritanceRecord> inheritances = new LinkedHashMap<>();
    private final Map<UUID, Heirloom> heirlooms = new LinkedHashMap<>();
    private final Map<UUID, BirthRecord> births = new LinkedHashMap<>();
    private final Map<UUID, LegacyRecord> legacies = new LinkedHashMap<>();
    private final Map<UUID, ClanRecord> clans = new LinkedHashMap<>();
    private final Map<UUID, LifeStage> stages = new HashMap<>();
    private final Set<String> storiesTold = new java.util.HashSet<>();
    private EventSink bus;
    private FamilySettings settings;
    private AgeEngine ages;
    private ConsistencyValidator validator;
    private SuccessionEngine succession;
    private KinshipEngine kinship;
    private Dice dice;
    private long seed;
    private FamilyPorts.Calendar calendar = () -> 360L * 1440;
    private FamilyPorts.Villages villages;
    private FamilyPorts.Economy economy;
    private FamilyPorts.Social social = FamilyPorts.NEUTRAL_SOCIAL;
    private FamilyPorts.Chronicle chronicle;
    private boolean dirty, loading;
    private long revision;

    public FamilyEngine(Supplier<FamilySettings> settings, EventSink bus, WorldClock clock, long seed) {
        this.settingsSupplier = Objects.requireNonNull(settings);
        this.bus = Objects.requireNonNull(bus);
        this.clock = Objects.requireNonNull(clock);
        this.seed = seed;
        this.dice = new Dice(seed);
        rebuild(settings.get());
        kinship = new KinshipEngine(graph, people::get);
    }

    private void rebuild(FamilySettings s) {
        settings = s;
        long year = calendar.minutesPerYear();
        ages = new AgeEngine(year, s.childAge(), s.adolescentAge(), s.youngAdultAge(), s.adultAge(), s.matureAge(), s.elderAge());
        validator = new ConsistencyValidator(s.minParentAge(), year);
        succession = new SuccessionEngine(s.successionRules());
    }

    private void refresh() { FamilySettings s = settingsSupplier.get(); if (s != settings || ages.minutesPerYear() != calendar.minutesPerYear()) rebuild(s); }

    public void useEventSink(EventSink sink) { bus = Objects.requireNonNull(sink); }
    public void useCalendar(FamilyPorts.Calendar c) { calendar = Objects.requireNonNull(c); rebuild(settings); }
    public void useVillages(FamilyPorts.Villages v) { villages = v; }
    public void useEconomy(FamilyPorts.Economy e) { economy = e; }
    public void useSocial(FamilyPorts.Social s) { social = Objects.requireNonNull(s); }
    public void useChronicle(FamilyPorts.Chronicle c) { chronicle = c; }
    public void useSeed(long s) { seed = s; dice = new Dice(s); }
    public void loading(boolean v) { loading = v; }
    private void publish(NpcEvent e) { if (!loading) bus.publish(e); }
    private long now() { return clock.now(); }

    private void record(String category, String title, String detail, Set<String> scopes, double significance, String cause) {
        if (chronicle != null && !loading) chronicle.record(now(), category, title, detail, scopes, significance, Provenance.of("family", "", cause, now()));
    }

    // ------------------------------------------------------------------ people and families

    public Optional<Person> person(UUID id) { return Optional.ofNullable(people.get(id)); }
    public Collection<Person> people() { return List.copyOf(people.values()); }
    public Optional<FamilyRecord> family(UUID id) { return Optional.ofNullable(families.get(id)); }
    public Collection<FamilyRecord> families() { return List.copyOf(families.values()); }
    public Optional<FamilyRecord> familyOf(UUID person) { Person p = people.get(person); return p == null || p.family() == null ? Optional.empty() : family(p.family()); }
    public Set<UUID> members(UUID family) { return Set.copyOf(members.getOrDefault(family, Set.of())); }
    public List<Person> living(UUID family) { return members(family).stream().map(people::get).filter(p -> p != null && p.state().living()).toList(); }
    public Optional<FamilyRecord> findFamily(String name) {
        for (FamilyRecord f : families.values()) if (f.name().equalsIgnoreCase(name) || f.id().toString().startsWith(name)) return Optional.of(f);
        return Optional.empty();
    }
    public Optional<Person> findPerson(String name) {
        for (Person p : people.values()) if (p.name().full().equalsIgnoreCase(name) || p.name().given().equalsIgnoreCase(name) || p.id().toString().startsWith(name)) return Optional.of(p);
        return Optional.empty();
    }

    private void index(Person p) { people.put(p.id(), p); if (p.family() != null) members.computeIfAbsent(p.family(), k -> new LinkedHashSet<>()).add(p.id()); }

    private FamilyRecord newFamily(String name, UUID village, UUID region) {
        FamilyRecord f = new FamilyRecord(UUID.randomUUID(), name, now(), village, region);
        families.put(f.id(), f);
        metrics.familiesCreated.incrementAndGet();
        dirty = true; revision++;
        return f;
    }

    public double age(UUID person) { Person p = people.get(person); return p == null ? 0 : ages.years(p.birth(), p.death() == Long.MIN_VALUE ? now() : p.death()); }
    public LifeStage stage(UUID person) { return ages.stage(age(person)); }
    public AgeEngine.AgeProfile ageProfile(UUID person) { Person p = people.get(person); return p == null ? null : ages.profile(p.birth(), now(), p.agingEnabled()); }

    /**
     * An NPC arrives without a family record: it gets one. If someone of a family already lives in the same house, it joins that
     * family as a partner, a sibling or a child (by their ages); otherwise a new family is founded, with {@code ancestorDepth}
     * generations of ancestors as historical records (its founders), so every person has a past. Its age is estimated
     * deterministically and marked as such.
     */
    public Person adopt(UUID npc, String name, UUID village, UUID region, UUID home, String profession, boolean embodied) {
        return adopt(npc, name, village, region, home, profession, embodied, "");
    }

    /**
     * As {@link #adopt(UUID, String, UUID, UUID, UUID, String, boolean)}, but also told the NPC's type, so a name a system
     * handed the NPC rather than one anyone chose for it ("Merchant_3") is replaced by a real given name drawn from the
     * family's naming culture, instead of becoming the person's name forever (see {@code NamingEngine.isGeneric}).
     */
    public Person adopt(UUID npc, String name, UUID village, UUID region, UUID home, String profession, boolean embodied, String npcType) {
        refresh();
        Person existing = people.get(npc);
        if (existing != null) { existing.embodied(embodied || existing.embodied()); if (profession != null && !profession.isEmpty()) noteProfession(existing, profession); return existing; }
        long now = now();
        double years = dice.between("age:" + npc, 0, settings.minAdultAge(), settings.maxAdultAge());
        long birth = ages.birthFor(years, now);
        boolean generic = NamingEngine.isGeneric(name, npcType);
        NameRecord nr = generic ? new NameRecord("", "", "", "", "") : NamingEngine.parse(name);
        Household hh = home == null ? null : household(home, village);
        Person anchor = null;
        if (hh != null && settings.groupHouseholds())
            for (UUID r : hh.residents()) { Person x = people.get(r); if (x != null && x.state().living() && x.family() != null) { anchor = x; break; } }
        Person p;
        String relation;
        if (anchor != null) {
            FamilyRecord f = families.get(anchor.family());
            double gap = age(anchor.id()) - years;
            NameCulture cultureHere = NameCulture.parse(f.cultureId()).orElse(NameCulture.YAMATO);
            String givenHere = nr.given().isEmpty() ? NamingEngine.givenName(dice, npc.toString(), KinGender.UNSPECIFIED, cultureHere) : nr.given();
            String familyHere = nr.family().isEmpty() ? f.name() : nr.family();
            NameRecord named = new NameRecord(givenHere, familyHere, "", "", "").withCulture(f.cultureId(), NamingEngine.orderOf(cultureHere));
            p = new Person(npc, named, KinGender.UNSPECIFIED, f.id(), anchor.generation(), birth, true, LifeState.ALIVE, embodied);
            if (graph.currentPartners(anchor.id()).isEmpty() && Math.abs(gap) <= settings.partnerAgeGap() && years >= settings.minAdultAge()) {
                p.birthFamily(null);
                index(p);
                graph.linkPartners(anchor.id(), npc, now, Long.MAX_VALUE);
                relation = "pareja de " + anchor.name().full();
                remember(f, FamilyMemoryEntry.Kind.UNION, p.name().full() + " se une a " + anchor.name().full(), List.of(anchor.id(), npc), 0.4);
            } else if (Math.abs(gap) <= settings.siblingAgeGap()) {
                index(p);
                ensureParents(anchor);
                for (UUID parent : graph.parentsOf(anchor.id())) link(parent, npc);
                relation = "hermano/a de " + anchor.name().full();
            } else if (gap >= settings.parentAgeGap()) {
                p.generation(anchor.generation() + 1);
                index(p);
                link(anchor.id(), npc);
                for (UUID partner : graph.currentPartners(anchor.id())) link(partner, npc);
                relation = "descendiente de " + anchor.name().full();
            } else { p = null; relation = ""; }
            if (p != null) {
                p.household(hh.id());
                hh.residents().add(npc);
                hh.families().add(f.id());
                f.generationCount(p.generation() + 1);
                p.birthPlace(village);
                if (profession != null && !profession.isEmpty()) noteProfession(p, profession);
                metrics.adopted.incrementAndGet();
                dirty = true; revision++;
                publish(new FamilyMemberJoinedEvent(now, f.id(), npc, p.name().full(), relation));
                return p;
            }
        }
        NameCulture culture = NamingEngine.cultureFor(dice, Dice.key(region != null ? region : village), settings.nameCultureWeights());
        NameCultureProfile profile = NameCultureCatalog.of(culture);
        String familyName = nr.family().isEmpty() ? NamingEngine.familyName(dice, npc.toString(), culture) : nr.family();
        FamilyRecord f = newFamily(familyName, village, region);
        f.cultureId(culture.name().toLowerCase(java.util.Locale.ROOT));
        String founderGiven = nr.given().isEmpty() ? NamingEngine.givenName(dice, npc.toString(), KinGender.UNSPECIFIED, culture) : nr.given();
        int depth = settings.ancestorDepth();
        p = new Person(npc, new NameRecord(founderGiven, familyName, "", "", "").withCulture(f.cultureId(), profile.order()), KinGender.UNSPECIFIED, f.id(), depth, birth, true, LifeState.ALIVE, embodied);
        p.birthPlace(village);
        index(p);
        List<UUID> frontier = List.of(npc);
        for (int g = depth - 1; g >= 0; g--) {
            List<UUID> next = new ArrayList<>();
            for (UUID child : frontier) {
                long childBirth = people.get(child).birth();
                List<UUID> parents = new ArrayList<>();
                for (KinGender kg : List.of(KinGender.MASCULINE, KinGender.FEMININE)) {
                    UUID id = LivingIds.named("ancestor", child + ":" + kg);
                    long ab = childBirth - Math.round(dice.between("ancestor-age:" + id, 0, settings.parentAgeGap() + 4, settings.parentAgeGap() + 16) * ages.minutesPerYear());
                    Person a = new Person(id, new NameRecord(NamingEngine.givenName(dice, id.toString(), kg, culture), familyName, "", "", "").withCulture(f.cultureId(), profile.order()), kg, f.id(), g, ab, true, LifeState.HISTORICAL, false);
                    a.remember("antepasado de " + p.name().full());
                    index(a);
                    graph.linkParent(id, child);
                    parents.add(id);
                }
                graph.linkPartners(parents.get(0), parents.get(1), childBirth - ages.minutesPerYear(), Long.MAX_VALUE);
                if (g == 0) f.founders().addAll(parents);
                next.addAll(parents);
            }
            frontier = next;
        }
        if (depth == 0) f.founders().add(npc);
        f.generationCount(depth + 1);
        f.head(npc);
        p.role("HEAD");
        if (hh != null) { p.household(hh.id()); hh.residents().add(npc); hh.families().add(f.id()); if (hh.head() == null) hh.head(npc); f.households().add(hh.id()); }
        if (profession != null && !profession.isEmpty()) noteProfession(p, profession);
        remember(f, FamilyMemoryEntry.Kind.FOUNDING, "La familia " + familyName + " llega a la historia con " + p.name().full(), List.of(npc), 0.4);
        metrics.adopted.incrementAndGet();
        dirty = true; revision++;
        publish(new FamilyCreatedEvent(now, f.id(), familyName, village, f.generationCount()));
        publish(new FamilyMemberJoinedEvent(now, f.id(), npc, p.name().full(), "fundador vivo"));
        return p;
    }

    /** A person without recorded parents gets a pair of historical parents (so siblings can share them). */
    private void ensureParents(Person p) {
        if (!graph.parentsOf(p.id()).isEmpty()) return;
        NameCulture culture = NameCulture.parse(p.name().cultureId()).orElse(NameCulture.YAMATO);
        List<UUID> parents = new ArrayList<>();
        for (KinGender kg : List.of(KinGender.MASCULINE, KinGender.FEMININE)) {
            UUID id = LivingIds.named("ancestor", p.id() + ":" + kg);
            long ab = p.birth() - Math.round((settings.parentAgeGap() + 8) * ages.minutesPerYear());
            Person a = new Person(id, new NameRecord(NamingEngine.givenName(dice, id.toString(), kg, culture), p.name().family(), "", "", "").withCulture(p.name().cultureId(), p.name().order()),
                    kg, p.family(), Math.max(0, p.generation() - 1), ab, true, LifeState.HISTORICAL, false);
            index(a);
            graph.linkParent(id, p.id());
            parents.add(id);
        }
        graph.linkPartners(parents.get(0), parents.get(1), p.birth() - ages.minutesPerYear(), Long.MAX_VALUE);
    }

    private void link(UUID parent, UUID child) { graph.linkParent(parent, child); }

    /** Adds a parent link after validation. Returns the refusal reason, or empty when linked. */
    public Optional<String> addParent(UUID parent, UUID child) {
        ConsistencyValidator.Verdict v = validator.canLinkParent(graph, people.get(parent), people.get(child));
        if (!v.ok()) { metrics.rejectedLinks.incrementAndGet(); return Optional.of(v.reason()); }
        graph.linkParent(parent, child);
        Person c = people.get(child), p = people.get(parent);
        if (c.family() != null && c.family().equals(p.family()) && c.generation() <= p.generation()) c.generation(p.generation() + 1);
        dirty = true; revision++;
        return Optional.empty();
    }

    public Optional<String> addPartners(UUID a, UUID b) {
        ConsistencyValidator.Verdict v = validator.canPartner(graph, people.get(a), people.get(b));
        if (!v.ok()) { metrics.rejectedLinks.incrementAndGet(); return Optional.of(v.reason()); }
        graph.linkPartners(a, b, now(), Long.MAX_VALUE);
        Person pa = people.get(a), pb = people.get(b);
        familyOf(a).ifPresent(f -> remember(f, FamilyMemoryEntry.Kind.UNION, pa.name().full() + " y " + pb.name().full() + " se unen", List.of(a, b), 0.45));
        dirty = true; revision++;
        return Optional.empty();
    }

    /**
     * Registers a birth: a new person of the parents' family and household, one generation after the eldest parent's, with a
     * birth record, a family memory, a timeline entry and a yearly birthday. The child has no entity until the adapter gives it
     * one ({@code embodied} false).
     */
    public Person birth(UUID parentA, UUID parentB, String given, UUID village) {
        refresh();
        Person a = people.get(parentA);
        if (a == null) throw new IllegalArgumentException("unknown parent " + parentA);
        Person b = parentB == null ? null : people.get(parentB);
        FamilyRecord f = families.get(a.family());
        UUID id = UUID.randomUUID();
        KinGender kg = KinGender.values()[dice.below("birth-gender:" + id, 0, 2)];
        NameCulture culture = NameCulture.parse(f == null ? a.name().cultureId() : f.cultureId()).orElse(NameCulture.YAMATO);
        String first = given == null || given.isBlank() ? NamingEngine.givenName(dice, id.toString(), kg, culture) : given;
        int generation = Math.max(a.generation(), b == null ? a.generation() : b.generation()) + 1;
        Person child = new Person(id, new NameRecord(first, f == null ? a.name().family() : f.name(), "", "", "").withCulture(f == null ? a.name().cultureId() : f.cultureId(), NamingEngine.orderOf(culture)),
                kg, a.family(), generation, now(), false, LifeState.ALIVE, false);
        child.birthPlace(village);
        child.household(a.household());
        index(child);
        graph.linkParent(parentA, id);
        if (b != null && validator.canLinkParent(graph, b, child).ok()) graph.linkParent(parentB, id);
        Household hh = a.household() == null ? null : households.get(a.household());
        if (hh != null) hh.residents().add(id);
        births.put(id, new BirthRecord(id, now(), clock.today().year(), village, parentA, parentB, a.family(), a.household(), generation, List.of()));
        metrics.births.incrementAndGet();
        if (f != null) {
            boolean newGeneration = generation + 1 > f.generationCount();
            f.generationCount(generation + 1);
            remember(f, FamilyMemoryEntry.Kind.BIRTH, "Nace " + child.name().full(), List.of(id, parentA), 0.35);
            if (newGeneration) { metrics.generations.incrementAndGet(); publish(new GenerationAdvancedEvent(now(), f.id(), generation)); }
            record("BIRTH", "Nace " + child.name().full(), "hijo/a de " + a.name().full() + (b == null ? "" : " y " + b.name().full()), Set.of(f.scope(), "npc:" + id, "npc:" + parentA), 0.25, "nacimiento");
            if (chronicle != null) chronicle.anniversary("BIRTH", "npc:" + id, "Nacimiento de " + child.name().full(), now());
        }
        stages.put(id, LifeStage.INFANT_FUTURE);
        dirty = true; revision++;
        publish(new BirthRegisteredEvent(now(), id, child.name().full(), a.family(), parentA, parentB, generation));
        return child;
    }

    private void noteProfession(Person p, String profession) {
        if (profession.equals(p.profession())) return;
        p.profession(profession);
        FamilyRecord f = p.family() == null ? null : families.get(p.family());
        if (f != null) { f.professionsByGeneration().merge(p.generation() + ":" + profession, 1, Integer::sum); f.markDirty(); }
        dirty = true; revision++;
    }

    /** The village reports someone's profession changed. */
    public void professionChanged(UUID person, String profession) { Person p = people.get(person); if (p != null && profession != null) noteProfession(p, profession); }

    // ------------------------------------------------------------------ kinship queries (the Kinship Query API)

    private <T> T measure(Supplier<T> q) {
        long t = System.nanoTime();
        T r = q.get();
        metrics.queries.incrementAndGet();
        metrics.queryNanos.addAndGet(System.nanoTime() - t);
        return r;
    }

    public Set<UUID> parents(UUID p) { return measure(() -> graph.parentsOf(p)); }
    public Set<UUID> children(UUID p) { return measure(() -> graph.childrenOf(p)); }
    public Set<UUID> siblings(UUID p) { return measure(() -> graph.siblingsOf(p)); }
    public Set<UUID> grandparents(UUID p) { return measure(() -> graph.grandparentsOf(p)); }
    public Map<UUID, Integer> ancestors(UUID p, int depth) { return measure(() -> graph.ancestors(p, depth)); }
    public Map<UUID, Integer> descendants(UUID p, int depth) { return measure(() -> graph.descendants(p, depth)); }
    public GenealogyGraph.Common commonAncestor(UUID a, UUID b) { return measure(() -> graph.commonAncestor(a, b)); }
    public int kinshipDegree(UUID a, UUID b) { return measure(() -> graph.degree(a, b)); }
    public boolean isRelated(UUID a, UUID b) { return measure(() -> graph.related(a, b)); }
    public String kinship(UUID a, UUID b) { return measure(() -> kinship.term(a, b)); }
    public Set<UUID> partners(UUID p) { return graph.currentPartners(p); }

    // ------------------------------------------------------------------ households

    /** The household of a home (created on first use), in a village. */
    public Household household(UUID home, UUID village) {
        UUID id = householdByHome.get(home);
        if (id != null) return households.get(id);
        Household h = new Household(LivingIds.named("household", home.toString()), home, village, now());
        if (villages != null) h.beds(villages.beds(home));
        households.put(h.id(), h);
        householdByHome.put(home, h.id());
        dirty = true; revision++;
        return h;
    }

    public Optional<Household> household(UUID id) { return Optional.ofNullable(households.get(id)); }
    public Collection<Household> households() { return List.copyOf(households.values()); }

    // ------------------------------------------------------------------ life states, succession, knowledge loss, inheritance, legacy

    /** Someone went missing, moved away, died, or is now only a historical record. Everything that hangs on it follows. */
    public void lifeState(UUID personId, LifeState state, String cause) {
        Person p = people.get(personId);
        if (p == null || p.state() == state) return;
        LifeState before = p.state();
        p.state(state);
        long now = now();
        if (state == LifeState.DECEASED_FUTURE) p.death(now);
        FamilyRecord f = p.family() == null ? null : families.get(p.family());
        if (state.gone()) {
            Household hh = p.household() == null ? null : households.get(p.household());
            if (hh != null) hh.residents().remove(personId);
            for (Mentorship m : mentorships.values()) {
                if (!m.open()) continue;
                if (m.master().equals(personId)) endMentorship(m, state == LifeState.DECEASED_FUTURE ? Mentorship.State.MASTER_DECEASED : Mentorship.State.BROKEN);
                else if (m.disciple().equals(personId)) endMentorship(m, Mentorship.State.DISCIPLE_DEPARTED);
            }
            for (Technique t : techniques.values()) if (t.holderGone(personId, now)) {
                metrics.techniquesLost.incrementAndGet();
                t.evidence(t.evidence().isEmpty() ? "solo quedan historias sobre " + t.name() : t.evidence());
                publish(new TechniqueLostEvent(now, t.key(), t.name(), personId));
                if (f != null) remember(f, FamilyMemoryEntry.Kind.OTHER, "Se pierde " + t.name() + " con " + p.name().full(), List.of(personId), 0.6);
                record("OTHER", "Se pierde " + t.name(), "nadie aprendió de " + p.name().full(), Set.of("npc:" + personId), 0.5, "conocimiento perdido");
            }
            for (Lineage l : lineages.values()) if (personId.equals(l.leader()) && l.status() == Lineage.Status.ACTIVE) lineageSuccession(l, "el maestro ya no está");
            if (f != null) {
                remember(f, state == LifeState.DECEASED_FUTURE ? FamilyMemoryEntry.Kind.DEATH : state == LifeState.MISSING ? FamilyMemoryEntry.Kind.MISSING : FamilyMemoryEntry.Kind.OTHER,
                        p.name().full() + (state == LifeState.DECEASED_FUTURE ? " muere" : state == LifeState.MISSING ? " desaparece" : " pasa a la historia") + (cause == null || cause.isEmpty() ? "" : " (" + cause + ")"),
                        List.of(personId), 0.5);
                if (personId.equals(f.head())) succeed(f.id(), "el jefe ya no está");
                if (state == LifeState.DECEASED_FUTURE) { inheritFrom(personId, cause); record("DEATH", "Muere " + p.name().full(), cause, Set.of(f.scope(), "npc:" + personId), 0.4, "muerte"); }
                checkExtinct(f);
            }
            legacy(personId);
        } else if (state == LifeState.MIGRATED && f != null) remember(f, FamilyMemoryEntry.Kind.MIGRATION, p.name().full() + " se marcha", List.of(personId), 0.35);
        dirty = true; revision++;
        publish(new LifeStateChangedEvent(now, personId, before.name(), state.name(), cause == null ? "" : cause));
    }

    private void checkExtinct(FamilyRecord f) {
        long alive = members.getOrDefault(f.id(), Set.of()).stream().map(people::get).filter(x -> x != null && x.state().living()).count();
        if (alive == 0 && f.status() != FamilyRecord.Status.EXTINCT) {
            f.status(FamilyRecord.Status.EXTINCT);
            metrics.extinct.incrementAndGet();
            remember(f, FamilyMemoryEntry.Kind.OTHER, "La familia " + f.name() + " se extingue; su historia permanece", List.of(), 0.7);
            record("FAMILY", "Se extingue la familia " + f.name(), "", Set.of(f.scope()), 0.6, "extinción");
            publish(new FamilyExtinctEvent(now(), f.id(), f.name()));
            if (f.clan() != null) leaveClan(f.clan(), f.id(), "la familia se extingue");
        } else if (alive == 1 && f.status() == FamilyRecord.Status.ACTIVE) f.status(FamilyRecord.Status.DECLINING);
        else if (alive > 1 && f.status() == FamilyRecord.Status.DECLINING) f.status(FamilyRecord.Status.ACTIVE);
    }

    /** Culture of the family's village, used by succession rules (default "village"). */
    private String cultureOf(FamilyRecord f) { return f.tags().stream().filter(t -> t.startsWith("culture:")).map(t -> t.substring(8)).findFirst().orElse("village"); }

    /** The succession pipeline. Returns the new head, if any adult member remains. */
    public Optional<UUID> succeed(UUID familyId, String reason) {
        FamilyRecord f = families.get(familyId);
        if (f == null) return Optional.empty();
        UUID previous = f.head();
        List<Person> candidates = living(familyId).stream().filter(x -> !x.id().equals(previous) && ages.stage(age(x.id())).adult()).toList();
        if (candidates.isEmpty()) { f.head(null); checkExtinct(f); return Optional.empty(); }
        double maxAge = candidates.stream().mapToDouble(x -> age(x.id())).max().orElse(1), minGen = candidates.stream().mapToInt(Person::generation).min().orElse(0);
        String trade = f.traditions().stream().filter(t -> t.startsWith("oficio:")).map(t -> t.substring(7)).findFirst().orElse("");
        List<SuccessionEngine.Candidate> list = new ArrayList<>();
        for (Person c : candidates) {
            double honor = 0;
            for (var cause : f.honor().causes()) if (c.id().equals(cause.person())) honor += cause.delta();
            long known = techniques.values().stream().filter(t -> t.holders().contains(c.id())).count();
            boolean close = previous != null && (graph.parentsOf(c.id()).contains(previous) || graph.currentPartners(previous).contains(c.id()));
            list.add(new SuccessionEngine.Candidate(c.id(), c.name().full(), age(c.id()) / Math.max(1, maxAge), c.generation() == minGen ? 1 : 0,
                    Math.max(0, social.standing(c.id(), f.village()) + 1) / 2, Math.max(0, Math.min(1, honor / 10)), !trade.isEmpty() && trade.equals(c.profession()) ? 1 : 0,
                    Math.min(1, known / 3.0), c.id().equals(f.designatedHeir()), close));
        }
        List<SuccessionEngine.Ranked> ranked = succession.rank(list, cultureOf(f));
        UUID next = ranked.get(0).person();
        if (previous != null && people.containsKey(previous)) people.get(previous).role("MEMBER");
        f.head(next);
        f.successor(ranked.size() > 1 ? ranked.get(1).person() : null);
        people.get(next).role("HEAD");
        for (int i = 0; i < ranked.size(); i++) people.get(ranked.get(i).person()).successionPosition(i + 1);
        metrics.successions.incrementAndGet();
        remember(f, FamilyMemoryEntry.Kind.SUCCESSION, people.get(next).name().full() + " pasa a ser cabeza de la familia (" + String.join(", ", ranked.get(0).reasons()) + ")", List.of(next), 0.5);
        for (Household h : households.values()) if (h.families().contains(f.id()) && h.residents().contains(next)) h.head(next);
        dirty = true; revision++;
        publish(new FamilyHeadChangedEvent(now(), f.id(), previous, next, reason));
        social.experience(next, previous, previous == null ? "" : nameOf(previous), "HONOR_OBSERVED", "toma la jefatura de la familia " + f.name());
        return Optional.of(next);
    }

    public List<SuccessionEngine.Ranked> successionRanking(UUID familyId) {
        FamilyRecord f = families.get(familyId);
        if (f == null) return List.of();
        List<SuccessionEngine.Candidate> list = new ArrayList<>();
        List<Person> alive = living(familyId).stream().filter(x -> ages.stage(age(x.id())).adult()).toList();
        double maxAge = alive.stream().mapToDouble(x -> age(x.id())).max().orElse(1);
        int minGen = alive.stream().mapToInt(Person::generation).min().orElse(0);
        for (Person c : alive) list.add(new SuccessionEngine.Candidate(c.id(), c.name().full(), age(c.id()) / Math.max(1, maxAge), c.generation() == minGen ? 1 : 0, 0.5, 0, 0, 0,
                c.id().equals(f.designatedHeir()), false));
        return succession.rank(list, cultureOf(f));
    }

    public boolean designateHeir(UUID familyId, UUID person) {
        FamilyRecord f = families.get(familyId);
        if (f == null || !members.getOrDefault(familyId, Set.of()).contains(person)) return false;
        f.designatedHeir(person);
        dirty = true; revision++;
        return true;
    }

    /** Divides an estate: current partner first, then children (eldest first), then siblings, then the family head. */
    public InheritanceRecord inheritFrom(UUID owner, String reason) {
        Person p = people.get(owner);
        List<UUID> heirs = new ArrayList<>();
        graph.currentPartners(owner).stream().filter(x -> people.get(x) != null && people.get(x).state().living()).forEach(heirs::add);
        graph.childrenOf(owner).stream().filter(x -> people.get(x) != null && people.get(x).state().living()).sorted((a, b) -> Long.compare(people.get(a).birth(), people.get(b).birth())).forEach(heirs::add);
        if (heirs.isEmpty()) graph.siblingsOf(owner).stream().filter(x -> people.get(x) != null && people.get(x).state().living()).forEach(heirs::add);
        if (heirs.isEmpty() && p != null && p.family() != null) { UUID head = families.get(p.family()).head(); if (head != null && !head.equals(owner)) heirs.add(head); }
        List<InheritanceRecord.Asset> assets = new ArrayList<>();
        if (economy != null) { double coins = economy.coins(owner); if (coins > 0) assets.add(new InheritanceRecord.Asset(InheritanceRecord.Asset.Kind.COINS, owner, coins, "monedas")); }
        if (villages != null) for (UUID b : villages.buildingsOwnedBy(owner)) assets.add(new InheritanceRecord.Asset(InheritanceRecord.Asset.Kind.HOME, b, 1, "edificio"));
        for (Heirloom h : heirlooms.values()) if (owner.equals(h.currentOwner()) && !h.lost()) assets.add(new InheritanceRecord.Asset(InheritanceRecord.Asset.Kind.HEIRLOOM, h.itemId(), 1, h.name()));
        InheritanceRecord r = new InheritanceRecord(UUID.randomUUID(), owner, heirs, assets, "", reason, now());
        inheritances.put(r.id(), r);
        execute(r);
        return r;
    }

    private void execute(InheritanceRecord r) {
        long now = now();
        if (r.heirs().isEmpty()) { r.executed(InheritanceRecord.Status.VOID, now); publish(new InheritanceCompletedEvent(now, r.id(), r.owner(), r.status().name(), 0)); return; }
        int done = 0;
        for (InheritanceRecord.Asset a : r.assets()) {
            switch (a.kind()) {
                case COINS -> {
                    double share = a.amount() / r.heirs().size(), moved = 0;
                    for (UUID heir : r.heirs()) moved += economy == null ? 0 : economy.transfer(r.owner(), heir, people.get(heir).name().full(), share, "herencia");
                    r.transfers().add(new InheritanceRecord.Transfer(a, r.heirs().get(0), now, moved > 0, String.format("%.0f monedas repartidas", moved)));
                    if (moved > 0) done++;
                }
                case HOME, WORKSHOP, BUSINESS, LAND -> {
                    UUID heir = r.heirs().get(0);
                    if (villages != null) villages.transferBuilding(a.id(), heir, people.get(heir).name().full());
                    r.transfers().add(new InheritanceRecord.Transfer(a, heir, now, villages != null, "pasa a " + people.get(heir).name().full()));
                    done++;
                }
                case HEIRLOOM -> { UUID heir = r.heirs().get(0); transferHeirloom(a.id(), heir, "herencia"); r.transfers().add(new InheritanceRecord.Transfer(a, heir, now, true, "reliquia")); done++; }
                default -> r.transfers().add(new InheritanceRecord.Transfer(a, r.heirs().get(0), now, false, "sin mecanismo aún"));
            }
        }
        r.executed(done == r.assets().size() ? InheritanceRecord.Status.EXECUTED : done > 0 ? InheritanceRecord.Status.PARTIAL : InheritanceRecord.Status.VOID, now);
        metrics.inheritances.incrementAndGet();
        Person owner = people.get(r.owner());
        if (owner != null && owner.family() != null) remember(families.get(owner.family()), FamilyMemoryEntry.Kind.PROPERTY_LOST, "La herencia de " + owner.name().full() + " pasa a " + people.get(r.heirs().get(0)).name().full(), r.heirs(), 0.45);
        dirty = true; revision++;
        publish(new InheritanceCompletedEvent(now, r.id(), r.owner(), r.status().name(), done));
    }

    // ------------------------------------------------------------------ lineages, masters and disciples, techniques

    public Lineage createLineage(String name, Lineage.Type type, UUID founder, String profession, UUID school) {
        Lineage l = new Lineage(UUID.randomUUID(), name, type, founder, now(), profession);
        l.school(school);
        l.note(clock.today().shortDate() + ": fundada por " + nameOf(founder));
        lineages.put(l.id(), l);
        Person p = people.get(founder);
        if (p != null) p.name(p.name().withLineage(name));
        dirty = true; revision++;
        return l;
    }

    public Optional<Lineage> lineage(UUID id) { return Optional.ofNullable(lineages.get(id)); }
    public Collection<Lineage> lineages() { return List.copyOf(lineages.values()); }
    public Optional<Lineage> findLineage(String name) { for (Lineage l : lineages.values()) if (l.name().equalsIgnoreCase(name) || l.id().toString().startsWith(name)) return Optional.of(l); return Optional.empty(); }

    /** The school chooses its next leader: the disciple with the most progress (completed first); dormant when none remains. */
    public Optional<UUID> lineageSuccession(Lineage l, String how) {
        UUID best = null;
        double bestScore = -1;
        for (Mentorship m : mentorships.values()) {
            if (!l.id().equals(m.lineage()) || !people.containsKey(m.disciple()) || !people.get(m.disciple()).state().living()) continue;
            double score = m.progress() + (m.state() == Mentorship.State.COMPLETED ? 1 : 0);
            if (score > bestScore) { bestScore = score; best = m.disciple(); }
        }
        if (best == null) {
            for (UUID member : l.members()) if (!member.equals(l.leader()) && people.containsKey(member) && people.get(member).state().living()) { best = member; break; }
        }
        UUID previous = l.leader();
        if (best == null) { l.status(Lineage.Status.DORMANT); l.note(clock.today().shortDate() + ": sin sucesor, la escuela queda dormida"); dirty = true; revision++; return Optional.empty(); }
        l.succeed(best, now(), how);
        l.note(clock.today().shortDate() + ": " + nameOf(best) + " sucede a " + nameOf(previous));
        dirty = true; revision++;
        publish(new LineageLeaderChangedEvent(now(), l.id(), previous, best, how));
        social.experience(best, previous, nameOf(previous), "HONOR_OBSERVED", "sucede al frente de " + l.name());
        return Optional.of(best);
    }

    /** A master proposes to teach a disciple. It becomes active at once unless {@code proposeOnly}. */
    public Mentorship mentor(UUID master, UUID disciple, Mentorship.Type type, UUID lineage, boolean proposeOnly) {
        for (Mentorship m : mentorships.values()) if (m.open() && m.master().equals(master) && m.disciple().equals(disciple)) return m;
        Mentorship m = new Mentorship(UUID.randomUUID(), master, disciple, type, now());
        m.lineage(lineage);
        m.bond(social.trust(disciple, master), social.respect(disciple, master));
        if (!proposeOnly) m.activate();
        mentorships.put(m.id(), m);
        metrics.mentorships.incrementAndGet();
        if (lineage != null && lineages.containsKey(lineage)) lineages.get(lineage).members().add(disciple);
        dirty = true; revision++;
        publish(new MentorshipStartedEvent(now(), m.id(), master, disciple, type.name()));
        return m;
    }

    public Collection<Mentorship> mentorships() { return List.copyOf(mentorships.values()); }
    public List<Mentorship> disciplesOf(UUID master) { return mentorships.values().stream().filter(m -> m.master().equals(master)).toList(); }
    public Optional<Mentorship> masterOf(UUID disciple) { return mentorships.values().stream().filter(m -> m.disciple().equals(disciple) && m.state() != Mentorship.State.BROKEN).findFirst(); }

    /** A technique (knowledge that must be taught to survive) created by someone, kept by a lineage or a family. */
    public Technique technique(String key, String name, UUID creator, UUID lineage, double rarity) {
        Technique t = techniques.computeIfAbsent(key, k -> new Technique(k, name, creator, lineage, people.containsKey(creator) ? people.get(creator).family() : null, now(), rarity));
        if (lineage != null && lineages.containsKey(lineage)) lineages.get(lineage).knowledge().add(key);
        familyOf(creator).ifPresent(f -> f.knowledge().add(key));
        dirty = true; revision++;
        return t;
    }

    public Collection<Technique> techniques() { return List.copyOf(techniques.values()); }
    public Optional<Technique> techniqueOf(String key) { return Optional.ofNullable(techniques.get(key)); }

    /** Someone teaches a technique they hold (a parent, a master, the community). The Knowledge Engine learns it for the pupil. */
    public boolean teach(String key, UUID from, UUID to, Technique.Via via) {
        Technique t = techniques.get(key);
        if (t == null || !t.teach(from, to, now(), via)) return false;
        social.learn(to, "technique:" + key, t.name(), from);
        familyOf(to).ifPresent(f -> f.knowledge().add(key));
        metrics.techniquesTaught.incrementAndGet();
        dirty = true; revision++;
        publish(new TechniqueTaughtEvent(now(), key, from, to, via.name()));
        return true;
    }

    /** Advances a mentorship by some days of training; teaches techniques at progress milestones; completes it at the end. */
    public void train(Mentorship m, double days) {
        if (m.state() != Mentorship.State.ACTIVE || days <= 0) return;
        Person master = people.get(m.master()), disciple = people.get(m.disciple());
        if (master == null || disciple == null || !master.state().living() || !disciple.state().living()) return;
        if (villages != null) {
            Optional<UUID> a = villages.villageOf(m.master()), b = villages.villageOf(m.disciple());
            if (a.isEmpty() || b.isEmpty() || !a.get().equals(b.get())) return;   // training needs them together
        }
        double quality = social.teachingQuality(m.master(), m.disciple());
        m.bond(social.trust(m.disciple(), m.master()), social.respect(m.disciple(), m.master()));
        m.advance(settings.mentorDailyRate() * (0.5D + quality) * days, now());
        List<Technique> known = techniques.values().stream().filter(t -> t.holders().contains(m.master()) && !t.holders().contains(m.disciple())).toList();
        int total = m.techniquesTaught().size() + known.size();
        for (Technique t : known) {
            double threshold = (m.techniquesTaught().size() + 1.0D) / (total + 1.0D);
            if (m.progress() < threshold) break;
            if (teach(t.key(), m.master(), m.disciple(), Technique.Via.MASTER)) { m.techniquesTaught().add(t.key()); m.knowledgeTaught().add(t.name()); }
        }
        if (m.progress() >= 1.0D) endMentorship(m, Mentorship.State.COMPLETED);
        dirty = true; revision++;
    }

    public void endMentorship(Mentorship m, Mentorship.State state) {
        if (!m.open()) return;
        m.end(state, now());
        if (state == Mentorship.State.COMPLETED) {
            metrics.mentorshipsCompleted.incrementAndGet();
            Person d = people.get(m.disciple());
            if (d != null) { d.legacyTags().add("discípulo de " + nameOf(m.master())); d.name(d.name().withLineage(m.lineage() != null && lineages.containsKey(m.lineage()) ? lineages.get(m.lineage()).name() : d.name().lineageName())); }
            familyOf(m.disciple()).ifPresent(f -> remember(f, FamilyMemoryEntry.Kind.SUCCESSION, nameOf(m.disciple()) + " termina su aprendizaje con " + nameOf(m.master()), List.of(m.disciple(), m.master()), 0.45));
            if (m.type() == Mentorship.Type.CRAFT && villages != null && d != null && villages.profession(m.disciple()).map(String::isEmpty).orElse(true))
                villages.profession(m.master()).ifPresent(p -> villages.assignProfession(m.disciple(), p, "aprendió el oficio de su maestro"));
            social.experience(m.disciple(), m.master(), nameOf(m.master()), "CELEBRATED", "ceremonia de graduación con su maestro");
            social.experience(m.master(), m.disciple(), nameOf(m.disciple()), "CELEBRATED", "ve a su discípulo terminar el camino");
        }
        dirty = true; revision++;
        publish(new MentorshipEndedEvent(now(), m.id(), state.name(), m.progress()));
    }

    // ------------------------------------------------------------------ heirlooms

    public Heirloom registerHeirloom(UUID itemId, String name, String kind, UUID owner) {
        Heirloom h = heirlooms.get(itemId);
        if (h != null) return h;
        h = new Heirloom(itemId, name, kind, owner, people.containsKey(owner) ? people.get(owner).family() : null, now());
        h.event(now(), "pasa a ser reliquia de " + nameOf(owner));
        heirlooms.put(itemId, h);
        Heirloom heirloom = h;
        familyOf(owner).ifPresent(f -> { f.heirlooms().add(itemId); remember(f, FamilyMemoryEntry.Kind.HEIRLOOM, name + " se convierte en reliquia familiar", List.of(owner), 0.4); });
        dirty = true; revision++;
        return heirloom;
    }

    public boolean transferHeirloom(UUID itemId, UUID to, String reason) {
        Heirloom h = heirlooms.get(itemId);
        if (h == null || to == null) return false;
        UUID from = h.currentOwner();
        h.transfer(to, now(), reason);
        h.event(now(), "pasa de " + nameOf(from) + " a " + nameOf(to) + " (" + reason + ")");
        dirty = true; revision++;
        publish(new HeirloomTransferredEvent(now(), itemId, h.name(), from, to, reason));
        social.experience(to, from, nameOf(from), "GIFT_RECEIVED", h.displayName() + ": " + reason);
        return true;
    }

    public void heirloomEvent(UUID itemId, String text) { Heirloom h = heirlooms.get(itemId); if (h != null) { h.event(now(), text); dirty = true; revision++; } }
    public void heirloomLost(UUID itemId, boolean lost) {
        Heirloom h = heirlooms.get(itemId);
        if (h == null) return;
        boolean wasLost = h.lost();
        h.lost(lost); h.event(now(), lost ? "se pierde" : "es recuperada");
        dirty = true; revision++;
        if (lost && !wasLost) publish(new HeirloomLostEvent(now(), itemId, h.name(), h.family()));
    }
    public Optional<Heirloom> heirloom(UUID id) { return Optional.ofNullable(heirlooms.get(id)); }
    public Collection<Heirloom> heirlooms() { return List.copyOf(heirlooms.values()); }
    public Optional<Heirloom> findHeirloom(String name) { for (Heirloom h : heirlooms.values()) if (h.name().equalsIgnoreCase(name) || h.itemId().toString().startsWith(name)) return Optional.of(h); return Optional.empty(); }

    // ------------------------------------------------------------------ identity: houses and epithets

    /**
     * A family historically important enough, and old enough, is recognised as a house ("Casa Ashborne"): granted once, in
     * the culture's own word for it, never taken away. Called once a day for every active family (see {@link #simulate}).
     */
    private void considerHouseTitle(FamilyRecord f) {
        if (f.isHouse() || !f.active()) return;
        if (f.historicalImportance() < settings.houseImportanceThreshold() || f.generationCount() < settings.houseMinGenerations()) return;
        NameCultureProfile profile = NameCultureCatalog.of(NameCulture.parse(f.cultureId()).orElse(NameCulture.YAMATO));
        String title = profile.houseWord() + " " + f.name();
        f.grantHouse(title, now());
        dirty = true; revision++;
        remember(f, FamilyMemoryEntry.Kind.OTHER, "La familia " + f.name() + " es reconocida como " + title, List.of(), 0.6);
        publish(new HouseTitleGrantedEvent(now(), f.id(), title, f.historicalImportance()));
    }

    /**
     * Whether a living adult earns an epithet from a real cause: heading their family, leading a school of more than one
     * generation, completing an apprenticeship, or enough honour (positive or negative) recorded under their own name. At most
     * one epithet per person, granted once (see {@link #simulate}); {@link EpithetCatalog} keeps it in Spanish regardless of
     * the person's given-name culture, in the Castilian chronicle style ("el Sabio", "la Cruel").
     */
    private void considerEpithet(Person p) {
        if (!p.name().epithet().isEmpty() || !p.state().living()) return;
        FamilyRecord f = p.family() == null ? null : families.get(p.family());
        if (f == null) return;
        EpithetCategory category = null;
        String cause = "";
        if (p.id().equals(f.head()) && f.historicalImportance() >= settings.houseImportanceThreshold() * 0.6) { category = EpithetCategory.LEADERSHIP; cause = "cabeza de " + f.name(); }
        if (category == null) for (Lineage l : lineages.values()) if (p.id().equals(l.leader()) && l.generations() > 1) { category = EpithetCategory.MASTERY; cause = "al frente de " + l.name(); break; }
        if (category == null) {
            double honorSum = 0;
            String lastCause = "";
            for (CauseLedger.Cause c : f.honor().causes()) if (p.id().equals(c.person())) { honorSum += c.delta(); lastCause = c.cause(); }
            if (Math.abs(honorSum) >= settings.epithetHonorThreshold()) { category = honorSum >= 0 ? EpithetCategory.HONOR : EpithetCategory.DISHONOR; cause = lastCause; }
        }
        if (category == null) {
            Optional<Mentorship> mastered = masterOf(p.id());
            if (mastered.isPresent() && mastered.get().state() == Mentorship.State.COMPLETED) { category = EpithetCategory.MENTORSHIP; cause = "terminó su aprendizaje"; }
        }
        if (category == null) return;
        String epithet = EpithetCatalog.pick(category, p.gender(), dice, p.id().toString());
        p.name(p.name().withEpithet(epithet));
        dirty = true; revision++;
        publish(new EpithetGrantedEvent(now(), p.id(), epithet, category.name(), cause));
        social.experience(p.id(), null, "", category == EpithetCategory.DISHONOR ? "DISHONOR_OBSERVED" : "HONOR_OBSERVED", "gana el nombre de " + epithet);
    }

    /** An heirloom that has passed through enough hands and events earns a name of its own, in its owning family's culture. */
    private void considerArtifactEpithet(Heirloom h) {
        if (h.lost() || !h.epithet().isEmpty() || h.symbolicValue() < settings.artifactEpithetThreshold()) return;
        FamilyRecord f = h.family() == null ? null : families.get(h.family());
        NameCultureProfile profile = NameCultureCatalog.of(NameCulture.parse(f == null ? "" : f.cultureId()).orElse(NameCulture.YAMATO));
        List<String> words = profile.surnamePrefixes();
        String word = words.isEmpty() ? "Vieja" : words.get(dice.below("artifact-word:" + h.itemId(), 0, words.size()));
        String kind = h.kind().isEmpty() ? "reliquia" : h.kind();
        String epithet = word + " " + Character.toUpperCase(kind.charAt(0)) + kind.substring(1);
        h.epithet(epithet);
        h.event(now(), "es conocida ahora como " + epithet);
        dirty = true; revision++;
        publish(new ArtifactNamedEvent(now(), h.itemId(), epithet, h.symbolicValue()));
    }

    // ------------------------------------------------------------------ clans

    /** Founds a clan over a family (a deliberate choice: nothing forms one on its own). The founder is its first leader. */
    public ClanRecord createClan(String name, UUID founderFamily) {
        FamilyRecord f = families.get(founderFamily);
        if (f == null) throw new IllegalArgumentException("unknown founder family");
        ClanRecord c = new ClanRecord(UUID.randomUUID(), name, f.head(), now(), f.originRegion());
        c.memberFamilies().add(founderFamily);
        c.leaderFamily(founderFamily);
        clans.put(c.id(), c);
        f.clan(c.id());
        refreshClanStatus(c);
        dirty = true; revision++;
        publish(new ClanCreatedEvent(now(), c.id(), name, founderFamily));
        if (f.head() != null) {
            social.experience(f.head(), null, "", "HONOR_OBSERVED", "funda el clan " + name);
            Person founder = people.get(f.head());
            if (founder != null && founder.name().epithet().isEmpty()) {
                String epithet = EpithetCatalog.pick(EpithetCategory.FOUNDING, founder.gender(), dice, founder.id().toString());
                founder.name(founder.name().withEpithet(epithet));
                publish(new EpithetGrantedEvent(now(), founder.id(), epithet, EpithetCategory.FOUNDING.name(), "funda el clan " + name));
            }
        }
        return c;
    }

    /** A family joins a clan, leaving whichever one it was in before. */
    public boolean joinClan(UUID clanId, UUID familyId) {
        ClanRecord c = clans.get(clanId);
        FamilyRecord f = families.get(familyId);
        if (c == null || f == null || c.memberFamilies().contains(familyId)) return false;
        if (f.clan() != null) leaveClan(f.clan(), familyId, "se une a otro clan");
        c.memberFamilies().add(familyId);
        f.clan(clanId);
        refreshClanStatus(c);
        dirty = true; revision++;
        publish(new ClanJoinedEvent(now(), clanId, familyId));
        return true;
    }

    /** A family leaves a clan; the clan disbands (remembered, not deleted) once no family is left to carry it. */
    public boolean leaveClan(UUID clanId, UUID familyId, String reason) {
        ClanRecord c = clans.get(clanId);
        FamilyRecord f = families.get(familyId);
        if (c == null || !c.memberFamilies().remove(familyId)) return false;
        if (f != null && clanId.equals(f.clan())) f.clan(null);
        dirty = true; revision++;
        publish(new ClanLeftEvent(now(), clanId, familyId, reason == null ? "" : reason));
        if (c.memberFamilies().isEmpty()) {
            c.status(ClanRecord.Status.DISPERSED);
            publish(new ClanDisbandedEvent(now(), clanId, c.name()));
        } else {
            if (familyId.equals(c.leaderFamily())) electClanLeader(c);
            refreshClanStatus(c);
        }
        return true;
    }

    private void electClanLeader(ClanRecord c) {
        UUID previous = c.leaderFamily();
        UUID best = null;
        double bestScore = -Double.MAX_VALUE;
        for (UUID fid : c.memberFamilies()) {
            FamilyRecord f = families.get(fid);
            if (f == null || !f.active()) continue;
            double score = f.reputation().value() + f.honor().value() / 100.0 + f.historicalImportance();
            if (score > bestScore) { bestScore = score; best = fid; }
        }
        c.leaderFamily(best);
        if (!java.util.Objects.equals(previous, best)) publish(new ClanLeaderChangedEvent(now(), c.id(), previous, best));
    }

    private void refreshClanStatus(ClanRecord c) {
        if (c.status() == ClanRecord.Status.DISPERSED || c.status() == ClanRecord.Status.EXTINCT || c.status() == ClanRecord.Status.HISTORICAL) return;
        long activeCount = c.memberFamilies().stream().map(families::get).filter(x -> x != null && x.active()).count();
        if (activeCount == 0) { c.status(ClanRecord.Status.EXTINCT); return; }
        c.status(activeCount >= settings.clanMinFamilies() ? ClanRecord.Status.ACTIVE : ClanRecord.Status.FORMING);
        if (c.leaderFamily() == null || families.get(c.leaderFamily()) == null || !families.get(c.leaderFamily()).active()) electClanLeader(c);
    }

    public Optional<ClanRecord> clan(UUID id) { return Optional.ofNullable(clans.get(id)); }
    public Collection<ClanRecord> clans() { return List.copyOf(clans.values()); }
    public Optional<ClanRecord> findClan(String name) {
        for (ClanRecord c : clans.values()) if (c.name().equalsIgnoreCase(name) || c.id().toString().startsWith(name)) return Optional.of(c);
        return Optional.empty();
    }

    // ------------------------------------------------------------------ elders: what a village's oldest can be asked about

    /** Whether this person is the oldest living resident of their village (village unknown or nobody older there → false). */
    public boolean isVillageElder(UUID person) {
        Person p = people.get(person);
        if (p == null || !p.state().living() || villages == null) return false;
        Optional<UUID> village = villages.villageOf(person);
        if (village.isEmpty() || ages.stage(age(person)) != LifeStage.ELDER) return false;
        for (Person other : people.values())
            if (!other.id().equals(person) && other.state().living() && villages.villageOf(other.id()).equals(village) && age(other.id()) > age(person)) return false;
        return true;
    }

    /**
     * What an elder could speak to from real family memory: wars and heroism their family lived through, a trade practised
     * across generations, techniques they or their line hold, a lineage's long history. Never a new knowledge system — every
     * line comes from {@link FamilyRecord#memory()}, {@link Technique} or {@link Lineage#history()} that already exist.
     */
    public List<String> elderTopics(UUID person) {
        List<String> out = new ArrayList<>();
        Person p = people.get(person);
        FamilyRecord f = p == null ? null : familyOf(person).orElse(null);
        if (f == null) return out;
        for (FamilyMemoryEntry e : f.memory())
            if ((e.kind() == FamilyMemoryEntry.Kind.WAR || e.kind() == FamilyMemoryEntry.Kind.BETRAYAL || e.kind() == FamilyMemoryEntry.Kind.HEROISM) && e.significance() >= settings.storySignificance())
                out.add(e.text() + " (año " + clock.date(e.minute()).year() + ")");
        f.professionsByGeneration().keySet().stream().map(k -> k.contains(":") ? k.substring(k.indexOf(':') + 1) : k).distinct()
                .forEach(trade -> out.add("el oficio de " + trade + " en la familia " + f.name()));
        for (Technique t : techniques.values()) if (t.holders().contains(person) || person.equals(t.creator())) out.add(t.lostAt() > Long.MIN_VALUE ? t.name() + " (perdida)" : t.name());
        for (Lineage l : lineages.values()) if (l.members().contains(person) && l.generations() > 1) out.add(l.name() + ", " + l.generations() + " generaciones de maestros");
        return out;
    }

    // ------------------------------------------------------------------ reputation, honour, memory, traditions, branches, migration

    public void reputation(UUID familyId, double delta, String cause, UUID person) {
        FamilyRecord f = families.get(familyId);
        if (f == null) return;
        double before = f.reputation().value();
        f.reputation().add(delta, cause, person, now());
        f.markDirty();
        publish(new FamilyReputationChangedEvent(now(), familyId, before, f.reputation().value(), cause));
    }

    public void honor(UUID familyId, double delta, String cause, UUID person) {
        FamilyRecord f = families.get(familyId);
        if (f == null) return;
        double before = f.honor().value();
        f.honor().add(delta, cause, person, now());
        f.historicalImportance(f.historicalImportance() + Math.abs(delta) * 0.01);
        remember(f, delta >= 0 ? FamilyMemoryEntry.Kind.HEROISM : FamilyMemoryEntry.Kind.BETRAYAL, cause, person == null ? List.of() : List.of(person), Math.min(1, 0.4 + Math.abs(delta) * 0.05));
        publish(new FamilyReputationChangedEvent(now(), familyId, before, f.honor().value(), "honor: " + cause));
    }

    /** What the community expects of a person because of their family: the family's reputation, which the person can change. */
    public double expectation(UUID person) { return familyOf(person).map(f -> f.reputation().value()).orElse(0.0D); }

    public void remember(FamilyRecord f, FamilyMemoryEntry.Kind kind, String text, List<UUID> persons, double significance) {
        if (f == null) return;
        f.remember(new FamilyMemoryEntry(now(), kind, text, persons, significance));
        dirty = true; revision++;
    }

    /** Family history by year ("Año 102: ..."), from the family memory and the official calendar. */
    public List<String> history(UUID familyId) {
        FamilyRecord f = families.get(familyId);
        List<String> out = new ArrayList<>();
        if (f == null) return out;
        int year = Integer.MIN_VALUE;
        for (FamilyMemoryEntry e : f.memory()) {
            int y = clock.date(e.minute()).year();
            if (y != year) { out.add("Año " + y + ":"); year = y; }
            out.add("  " + e.text());
        }
        return out;
    }

    public FamilyRecord branch(UUID familyId, UUID founder, String reason, UUID village) {
        FamilyRecord parent = families.get(familyId);
        Person p = people.get(founder);
        if (parent == null || p == null) throw new IllegalArgumentException("unknown family or founder");
        FamilyRecord b = newFamily(parent.name(), village == null ? parent.village() : village, parent.originRegion());
        b.branchOf(parent.id(), reason);
        b.founders().add(founder);
        b.tags().add("rama");
        b.cultureId(parent.cultureId());
        if (parent.clan() != null) { b.clan(parent.clan()); ClanRecord pc = clans.get(parent.clan()); if (pc != null) pc.memberFamilies().add(b.id()); }
        parent.branches().add(b.id());
        members.getOrDefault(parent.id(), new LinkedHashSet<>()).remove(founder);
        p.family(b.id());
        p.role("HEAD");
        b.head(founder);
        b.generationCount(p.generation() + 1);
        members.computeIfAbsent(b.id(), k -> new LinkedHashSet<>()).add(founder);
        for (UUID d : graph.descendants(founder, 0).keySet()) { Person x = people.get(d); if (x != null && parent.id().equals(x.family())) { members.get(parent.id()).remove(d); x.family(b.id()); members.get(b.id()).add(d); } }
        remember(parent, FamilyMemoryEntry.Kind.BRANCH, p.name().full() + " funda una rama propia (" + reason + ")", List.of(founder), 0.5);
        remember(b, FamilyMemoryEntry.Kind.FOUNDING, "Nace la rama de " + p.name().full() + ": " + reason, List.of(founder), 0.5);
        if (founder.equals(parent.head())) succeed(parent.id(), "el jefe fundó una rama");
        metrics.branches.incrementAndGet();
        publish(new FamilyBranchCreatedEvent(now(), parent.id(), b.id(), founder, reason));
        return b;
    }

    /** The family moves to another village (the hub moves the people through the villages; this records it). */
    public void migrated(UUID familyId, UUID toVillage) {
        FamilyRecord f = families.get(familyId);
        if (f == null || toVillage.equals(f.village())) return;
        UUID from = f.village();
        f.village(toVillage);
        f.status(FamilyRecord.Status.MIGRATED);
        remember(f, FamilyMemoryEntry.Kind.MIGRATION, "La familia se traslada", List.of(), 0.5);
        publish(new FamilyMigratedEvent(now(), familyId, from, toVillage));
    }

    public void relation(UUID a, UUID b, FamilyRecord.Relation relation) {
        FamilyRecord fa = families.get(a), fb = families.get(b);
        if (fa == null || fb == null || a.equals(b)) return;
        fa.relations().put(b, relation);
        fb.relations().put(a, relation);
        dirty = true; revision++;
    }

    public List<ProfessionHeritage.Suggestion> professionSuggestions(UUID person) {
        Person p = people.get(person);
        if (p == null) return List.of();
        Map<String, Double> exposure = new LinkedHashMap<>(), affinity = new LinkedHashMap<>();
        for (UUID parent : graph.parentsOf(person)) { Person x = people.get(parent); if (x != null && !x.profession().isEmpty()) exposure.merge(x.profession(), 0.5, Double::sum); }
        Household hh = p.household() == null ? null : households.get(p.household());
        if (hh != null) for (UUID r : hh.residents()) { Person x = people.get(r); if (x != null && !r.equals(person) && !x.profession().isEmpty()) exposure.merge(x.profession(), 0.15, Double::sum); }
        for (String prof : exposure.keySet()) affinity.put(prof, social.affinity(person, prof));
        List<String> needs = villages == null ? List.of() : villages.villageOf(person).map(villages::neededProfessions).orElse(List.of());
        for (String n : needs) affinity.putIfAbsent(n, social.affinity(person, n));
        String mentor = masterOf(person).flatMap(m -> villages == null ? Optional.empty() : villages.profession(m.master())).orElse("");
        List<String> trads = familyOf(person).map(f -> List.copyOf(f.traditions())).orElse(List.of());
        return heritage.suggest(new ProfessionHeritage.Inputs(exposure, affinity, needs, trads, mentor));
    }

    // ------------------------------------------------------------------ legacy

    /** A person's legacy, from its causes. */
    public LegacyRecord legacy(UUID personId) {
        Person p = people.get(personId);
        List<LegacyRecord.Cause> causes = new ArrayList<>();
        if (p == null) return new LegacyRecord(personId, now(), causes);
        int descendants = graph.descendants(personId, 0).size();
        if (descendants > 0) causes.add(new LegacyRecord.Cause(LegacyRecord.Kind.FAMILY_IMPACT, descendants * 0.5, descendants + " descendientes"));
        FamilyRecord f = p.family() == null ? null : families.get(p.family());
        if (f != null && !p.profession().isEmpty() && f.traditions().contains("oficio:" + p.profession()))
            causes.add(new LegacyRecord.Cause(LegacyRecord.Kind.PROFESSION_IMPACT, 2, "su oficio, " + p.profession() + ", es tradición de la familia"));
        long disciples = mentorships.values().stream().filter(m -> m.master().equals(personId) && m.state() == Mentorship.State.COMPLETED).count();
        if (disciples > 0) causes.add(new LegacyRecord.Cause(LegacyRecord.Kind.DISCIPLES, disciples * 2.0, disciples + " discípulos formados"));
        long taught = techniques.values().stream().mapToLong(t -> t.transmissions().stream().filter(x -> x.from().equals(personId)).count()).sum();
        if (taught > 0) causes.add(new LegacyRecord.Cause(LegacyRecord.Kind.TAUGHT_KNOWLEDGE, taught * 1.5, taught + " enseñanzas transmitidas"));
        if (f != null) {
            double honor = 0, rep = 0;
            for (var c : f.honor().causes()) if (personId.equals(c.person())) honor += c.delta();
            for (var c : f.reputation().causes()) if (personId.equals(c.person())) rep += c.delta();
            if (Math.abs(honor) > 0) causes.add(new LegacyRecord.Cause(LegacyRecord.Kind.HONOR, honor, String.format("honor familiar %+.1f", honor)));
            if (Math.abs(rep) > 0) causes.add(new LegacyRecord.Cause(LegacyRecord.Kind.REPUTATION, rep * 5, String.format("reputación familiar %+.2f", rep)));
            long heroic = f.memory().stream().filter(e -> e.kind() == FamilyMemoryEntry.Kind.HEROISM && e.persons().contains(personId)).count();
            if (heroic > 0) causes.add(new LegacyRecord.Cause(LegacyRecord.Kind.COMMUNITY_IMPACT, heroic * 2.0, heroic + " actos por la comunidad"));
        }
        for (Heirloom h : heirlooms.values()) if (personId.equals(h.originalOwner())) causes.add(new LegacyRecord.Cause(LegacyRecord.Kind.HEIRLOOM, h.symbolicValue() * 0.5, "dejó " + h.name()));
        if (villages != null) { int owned = villages.buildingsOwnedBy(personId).size(); if (owned > 0) causes.add(new LegacyRecord.Cause(LegacyRecord.Kind.PROPERTY, owned, owned + " propiedades")); }
        if (chronicle != null) { int n = chronicle.mentions("npc:" + personId); if (n > 0) causes.add(new LegacyRecord.Cause(LegacyRecord.Kind.HISTORIC_EVENT, n, n + " hechos en la historia del mundo")); }
        LegacyRecord r = new LegacyRecord(personId, now(), causes);
        legacies.put(personId, r);
        dirty = true; revision++;
        if (p.state().gone()) publish(new LegacyCreatedEvent(now(), personId, r.total(), causes.size()));
        return r;
    }

    public Optional<LegacyRecord> storedLegacy(UUID person) { return Optional.ofNullable(legacies.get(person)); }

    // ------------------------------------------------------------------ simulation

    /**
     * One day (or {@code days} days, catching up) for the families of some villages: life stages from the calendar, training,
     * household states, heads that are missing, traditions that emerged, and family stories told to young adults by a living
     * relative (knowledge, not memory).
     */
    public void simulate(Collection<UUID> villageIds, double days) {
        long started = System.nanoTime();
        refresh();
        Set<UUID> inVillages = new LinkedHashSet<>(villageIds);
        for (Person p : List.copyOf(people.values())) {
            if (!p.state().living() || !p.agingEnabled()) continue;
            Optional<UUID> v = villages == null ? Optional.empty() : villages.villageOf(p.id());
            if (v.isPresent() && !inVillages.contains(v.get())) continue;
            LifeStage now = ages.stage(age(p.id()));
            LifeStage before = stages.put(p.id(), now);
            if (before != null && before != now) {
                publish(new LifeStageChangedEvent(now(), p.id(), before.name(), now.name()));
                if (now == LifeStage.YOUNG_ADULT) tellStories(p);
            } else if (before == null && now.adult()) tellStories(p);
            if (now.adult()) considerEpithet(p);
        }
        for (Mentorship m : List.copyOf(mentorships.values())) if (m.state() == Mentorship.State.ACTIVE) train(m, days);
        for (Household h : households.values()) {
            if (h.village() != null && !inVillages.contains(h.village())) continue;
            if (villages != null) { h.beds(villages.beds(h.home())); h.refresh(villages.damaged(h.home()), villages.destroyed(h.home())); }
        }
        for (FamilyRecord f : families.values()) {
            if (!f.active() || f.village() != null && !inVillages.contains(f.village())) continue;
            if (f.head() == null || !people.containsKey(f.head()) || !people.get(f.head()).state().living()) succeed(f.id(), "sin cabeza de familia");
            int martial = 0;
            Set<Integer> gens = new LinkedHashSet<>();
            for (Lineage l : lineages.values()) if (l.type() == Lineage.Type.SAMURAI) for (UUID m : l.members()) { Person x = people.get(m); if (x != null && f.id().equals(x.family())) gens.add(x.generation()); }
            martial = gens.size();
            for (String t : traditions.detect(f, f.heirlooms().stream().map(heirlooms::get).filter(Objects::nonNull).toList(), martial, (int) Math.min(Integer.MAX_VALUE, ages.minutesPerYear()))) {
                f.traditions().add(t);
                metrics.traditions.incrementAndGet();
                remember(f, FamilyMemoryEntry.Kind.TRADITION, "Nace una tradición: " + t, List.of(), 0.5);
                publish(new TraditionEmergedEvent(now(), f.id(), t));
            }
            considerHouseTitle(f);
            for (UUID item : f.heirlooms()) { Heirloom h = heirlooms.get(item); if (h != null) considerArtifactEpithet(h); }
            if (f.clan() != null) { ClanRecord c = clans.get(f.clan()); if (c != null) refreshClanStatus(c); }
            checkExtinct(f);
        }
        metrics.simulations.incrementAndGet();
        metrics.simulationNanos.addAndGet(System.nanoTime() - started);
    }

    /** Obon (the days of the ancestors): families with ancestors honour them, which in time becomes a tradition. */
    public void ancestorsHonoured() {
        for (FamilyRecord f : families.values()) {
            if (!f.active()) continue;
            boolean hasAncestors = members.getOrDefault(f.id(), Set.of()).stream().map(people::get).anyMatch(p -> p != null && p.state().gone());
            if (hasAncestors) remember(f, FamilyMemoryEntry.Kind.TRADITION, "La familia honra a sus antepasados en Obon", List.of(), 0.2);
        }
    }

    /** Family stories older than a person reach them as knowledge, told by the eldest living relative (never as their own memory). */
    public int tellStories(Person p) {
        FamilyRecord f = p.family() == null ? null : families.get(p.family());
        if (f == null) return 0;
        UUID teller = null;
        double oldest = -1;
        for (UUID r : graph.ancestors(p.id(), 3).keySet()) { Person x = people.get(r); if (x != null && x.state().living() && age(r) > oldest) { oldest = age(r); teller = r; } }
        if (teller == null) for (UUID r : members.getOrDefault(f.id(), Set.of())) { Person x = people.get(r); if (x != null && x.state().living() && !r.equals(p.id()) && age(r) > oldest) { oldest = age(r); teller = r; } }
        if (teller == null) return 0;
        int n = 0;
        for (FamilyMemoryEntry e : f.memory()) {
            if (e.significance() < settings.storySignificance() || e.minute() > p.birth() && !e.persons().contains(p.id()) && e.minute() > now() - ages.minutesPerYear()) continue;
            String key = p.id() + "|" + e.minute() + "|" + e.text().hashCode();
            if (!storiesTold.add(key)) continue;
            social.learn(p.id(), "family-story:" + Integer.toHexString(key.hashCode()), e.text(), teller);
            n++;
        }
        return n;
    }

    public List<String> audit() { return validator.audit(graph, people); }

    // ------------------------------------------------------------------ accessors

    private String nameOf(UUID id) { Person p = id == null ? null : people.get(id); return p == null ? "?" : p.name().full(); }
    public GenealogyGraph graph() { return graph; }
    public FamilySettings settings() { return settings; }
    public FamilyMetrics metrics() { return metrics; }
    public AgeEngine ages() { return ages; }
    public WorldClock clock() { return clock; }
    public Collection<InheritanceRecord> inheritances() { return List.copyOf(inheritances.values()); }
    public Collection<BirthRecord> births() { return List.copyOf(births.values()); }
    public Collection<LegacyRecord> legacies() { return List.copyOf(legacies.values()); }
    public Set<String> storiesTold() { return storiesTold; }
    public long seed() { return seed; }
    public boolean dirty() { if (dirty) return true; for (FamilyRecord f : families.values()) if (f.dirty()) return true; return false; }
    public void clean() { dirty = false; families.values().forEach(FamilyRecord::clean); }
    /** Grows with every change; each persistent section saves when it moved since its own last save. */
    public long revision() { return revision + graph.version(); }

    // restore hooks
    public void restorePerson(Person p) { index(p); }
    public void restoreFamily(FamilyRecord f) { families.put(f.id(), f); }
    public void restoreHousehold(Household h) { households.put(h.id(), h); if (h.home() != null) householdByHome.put(h.home(), h.id()); }
    public void restoreLineage(Lineage l) { lineages.put(l.id(), l); }
    public void restoreMentorship(Mentorship m) { mentorships.put(m.id(), m); }
    public void restoreTechnique(Technique t) { techniques.put(t.key(), t); }
    public void restoreInheritance(InheritanceRecord r) { inheritances.put(r.id(), r); }
    public void restoreHeirloom(Heirloom h) { heirlooms.put(h.itemId(), h); }
    public void restoreBirth(BirthRecord b) { births.put(b.person(), b); }
    public void restoreLegacy(LegacyRecord r) { legacies.put(r.person(), r); }
    public void restoreClan(ClanRecord c) { clans.put(c.id(), c); }

    public void reset() {
        graph.clear(); people.clear(); families.clear(); members.clear(); households.clear(); householdByHome.clear(); lineages.clear(); mentorships.clear(); techniques.clear();
        inheritances.clear(); heirlooms.clear(); births.clear(); legacies.clear(); clans.clear(); stages.clear(); storiesTold.clear(); metrics.reset(); dirty = false;
    }
}
