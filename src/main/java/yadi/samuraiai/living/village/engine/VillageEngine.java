package yadi.samuraiai.living.village.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.event.NpcEvent;
import yadi.samuraiai.living.core.CalendarDate;
import yadi.samuraiai.living.core.DayPhase;
import yadi.samuraiai.living.core.Dice;
import yadi.samuraiai.living.core.LivingIds;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.core.WeatherKind;
import yadi.samuraiai.living.core.WorldClock;
import yadi.samuraiai.living.village.buildings.Building;
import yadi.samuraiai.living.village.buildings.BuildingKind;
import yadi.samuraiai.living.village.buildings.OwnerRef;
import yadi.samuraiai.living.village.citizens.Citizen;
import yadi.samuraiai.living.village.districts.District;
import yadi.samuraiai.living.village.districts.DistrictKind;
import yadi.samuraiai.living.village.events.BuildingRegisteredEvent;
import yadi.samuraiai.living.village.events.BuildingStateChangedEvent;
import yadi.samuraiai.living.village.events.CitizenJoinedEvent;
import yadi.samuraiai.living.village.events.CitizenLeftEvent;
import yadi.samuraiai.living.village.events.GuardShiftChangedEvent;
import yadi.samuraiai.living.village.events.HomeAssignedEvent;
import yadi.samuraiai.living.village.events.HousingShortageEvent;
import yadi.samuraiai.living.village.events.MarketClosedEvent;
import yadi.samuraiai.living.village.events.MarketOpenedEvent;
import yadi.samuraiai.living.village.events.ProfessionAssignedEvent;
import yadi.samuraiai.living.village.events.TempleRitualEvent;
import yadi.samuraiai.living.village.events.VillageCreatedEvent;
import yadi.samuraiai.living.village.events.VillageEventEndedEvent;
import yadi.samuraiai.living.village.events.VillageEventStartedEvent;
import yadi.samuraiai.living.village.events.VillagePopulationChangedEvent;
import yadi.samuraiai.living.village.events.VillageSecurityChangedEvent;
import yadi.samuraiai.living.village.events.VisitorArrivedEvent;
import yadi.samuraiai.living.village.events.VisitorLeftEvent;
import yadi.samuraiai.living.village.guards.GuardEngine;
import yadi.samuraiai.living.village.homes.HomeEngine;
import yadi.samuraiai.living.village.homes.HomeRecord;
import yadi.samuraiai.living.village.life.VillageEventKind;
import yadi.samuraiai.living.village.life.VillageEventRecord;
import yadi.samuraiai.living.village.market.MarketLifeEngine;
import yadi.samuraiai.living.village.memory.CommunityMemory;
import yadi.samuraiai.living.village.metrics.VillageMetrics;
import yadi.samuraiai.living.village.population.VillageCensus;
import yadi.samuraiai.living.village.professions.ProfessionAssigner;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.village.runtime.VillageLayout;
import yadi.samuraiai.living.village.runtime.VillagePlanner;
import yadi.samuraiai.living.village.schedules.DailyScheduleEngine;
import yadi.samuraiai.living.village.schedules.DayPlan;
import yadi.samuraiai.living.village.security.SecurityRuntime;
import yadi.samuraiai.living.village.security.SecurityState;
import yadi.samuraiai.living.village.social.SocialLifeEngine;
import yadi.samuraiai.living.village.temple.TempleLifeEngine;
import yadi.samuraiai.living.village.visitors.VisitorEngine;
import yadi.samuraiai.living.village.visitors.VisitorRecord;

/**
 * The Living Villages Engine: "how does this community work". It owns villages, citizenship, buildings, districts, homes, the
 * local professions, daily schedules, market and temple life, guards, visitors, security, village events and community
 * memory; villages keep running when no player is there (they are simulated with the world's regions).
 *
 * <p>It never moves an NPC: its day plans, duties and events reach the Behavior Scheduler as routine <b>bias</b> through the
 * hub ({@link #routineBias}), and the scheduler, which still owns the decision, weighs them with everything else. Pure Java.
 */
public final class VillageEngine {
    /** Routine bias for one citizen now, with the reasons (debug). */
    public record Bias(Map<String, Double> bias, List<String> reasons, String planned) {
        public static final Bias NONE = new Bias(Map.of(), List.of(), "");
    }

    private record Planned(long minute, Map<UUID, String> routines, Map<UUID, String> professions) { }

    private static final Set<String> OUTDOOR = Set.of("farmer", "fisherman", "woodcutter", "hunter", "herbalist", "miner");

    private final Supplier<VillageSettings> settingsSupplier;
    private final WorldClock clock;
    private final VillageMetrics metrics = new VillageMetrics();
    private final Map<UUID, Village> villages = new LinkedHashMap<>();
    private final Map<UUID, Citizen> citizens = new LinkedHashMap<>();
    private final Map<UUID, Building> buildings = new HashMap<>();
    private final HomeEngine homes = new HomeEngine();
    private final ProfessionAssigner assigner = new ProfessionAssigner();
    private final MarketLifeEngine market = new MarketLifeEngine();
    private final TempleLifeEngine temple = new TempleLifeEngine();
    private final GuardEngine guards = new GuardEngine();
    private final SocialLifeEngine social = new SocialLifeEngine();
    private final CommunityMemory memory = new CommunityMemory();
    private final VisitorEngine visitorEngine = new VisitorEngine();
    private final Map<UUID, Planned> plannedCache = new HashMap<>();
    private final Map<UUID, Integer> reportedPopulation = new HashMap<>();
    private final Map<UUID, Boolean> dayShift = new HashMap<>();
    private final Map<UUID, Long> visitorsDrawn = new HashMap<>();
    private final Map<UUID, Long> festivalsChecked = new HashMap<>();
    private final Map<UUID, Integer> reportedHomeless = new HashMap<>();
    private final Set<UUID> endAnnounced = new java.util.HashSet<>();
    private EventSink bus;
    private VillageSettings settings;
    private DailyScheduleEngine schedules;
    private Dice dice;
    private long seed;
    private VillagePorts.Professions professions = VillagePorts.NO_PROFESSIONS;
    private VillagePorts.Calendar calendar;
    private VillagePorts.Community community = VillagePorts.NO_COMMUNITY;
    private VillagePorts.Chronicle chronicle = VillagePorts.SILENT;
    private VillagePorts.Economy economy = VillagePorts.NO_ECONOMY;
    private VillagePorts.Family family = VillagePorts.NO_FAMILY;
    private long tickCount;
    private int cursor;
    private boolean citizensDirty, loading;

    public VillageEngine(Supplier<VillageSettings> settings, EventSink bus, WorldClock clock, long seed) {
        this.settingsSupplier = Objects.requireNonNull(settings);
        this.bus = Objects.requireNonNull(bus);
        this.clock = Objects.requireNonNull(clock);
        this.seed = seed;
        this.dice = new Dice(seed);
        this.calendar = VillagePorts.fixedCalendar(clock.today());
        rebuild(settings.get());
    }

    private void rebuild(VillageSettings s) {
        settings = s;
        schedules = new DailyScheduleEngine(s.templates(), s.professionSchedules(), s.nightShiftMinutes());
    }

    private void refresh() { VillageSettings s = settingsSupplier.get(); if (s != settings) rebuild(s); }

    public void useEventSink(EventSink sink) { bus = Objects.requireNonNull(sink); }
    public void useProfessions(VillagePorts.Professions p) { professions = Objects.requireNonNull(p); }
    public void useCalendar(VillagePorts.Calendar c) { calendar = Objects.requireNonNull(c); }
    public void useCommunity(VillagePorts.Community c) { community = Objects.requireNonNull(c); }
    public void useChronicle(VillagePorts.Chronicle c) { chronicle = Objects.requireNonNull(c); }
    public void useEconomy(VillagePorts.Economy e) { economy = Objects.requireNonNull(e); }
    public void useFamily(VillagePorts.Family f) { family = Objects.requireNonNull(f); }
    public void useSeed(long s) { seed = s; dice = new Dice(s); }
    /** While true (restoring from disk) nothing is announced or chronicled. */
    public void loading(boolean v) { loading = v; }

    private void publish(NpcEvent e) { if (!loading) bus.publish(e); }
    private long now() { return clock.now(); }

    // ------------------------------------------------------------------ villages

    /**
     * Creates the village of a world settlement (same id). {@code plan}: lay out a planned village; {@code built}: planned
     * buildings count as built (a village that exists only in the simulation).
     */
    public Village createVillage(UUID settlement, UUID region, String name, String culture, String dimension, double x, double y, double z, double radius, boolean plan, boolean built) {
        refresh();
        Village existing = villages.get(settlement);
        if (existing != null) return existing;
        String key = community.ensure("village-" + LivingIds.shortId(settlement), name, culture, dimension, x, y, z, radius);
        Village v = new Village(settlement, region, name, culture, key, dimension, x, y, z, radius, now());
        villages.put(settlement, v);
        v.layout().add(new VillageLayout.Node(LivingIds.named("layout-plaza", settlement.toString()), VillageLayout.NodeKind.PLAZA, x, z, null));
        if (plan) {
            boolean templeWanted = !"market".equals(culture), marketWanted = !"temple".equals(culture);
            for (VillagePlanner.Planned p : VillagePlanner.plan(settlement.toString(), x, z, radius, settings.plannedHouses(), templeWanted, marketWanted, dice)) {
                if (p.kind() == BuildingKind.GATE) v.layout().add(new VillageLayout.Node(LivingIds.named("layout-gate", settlement + p.name()), VillageLayout.NodeKind.GATE, p.x(), p.z(), null));
                registerBuilding(settlement, p.kind(), p.name(), dimension, p.x(), y, p.z(), p.radius(), built ? Building.State.BUILT : Building.State.PLANNED, "");
            }
        }
        metrics.villagesCreated.incrementAndGet();
        publish(new VillageCreatedEvent(now(), settlement, name, region, culture, plan));
        return v;
    }

    public Optional<Village> village(UUID id) { return Optional.ofNullable(villages.get(id)); }
    public Collection<Village> villages() { return List.copyOf(villages.values()); }
    public Optional<Village> villageOf(UUID npc) { Citizen c = citizens.get(npc); return c == null || c.village() == null ? Optional.empty() : village(c.village()); }
    public Optional<Village> villageAt(String dimension, double x, double z) {
        Village best = null;
        for (Village v : villages.values()) if (v.contains(dimension, x, z) && (best == null || v.distance(x, z) < best.distance(x, z))) best = v;
        return Optional.ofNullable(best);
    }
    public Optional<Village> nearest(String dimension, double x, double z, double max) {
        Village best = null;
        for (Village v : villages.values()) if (v.dimension().equals(dimension) && v.distance(x, z) <= Math.max(max, v.radius()) && (best == null || v.distance(x, z) < best.distance(x, z))) best = v;
        return Optional.ofNullable(best);
    }
    public Optional<Village> find(String nameOrId) {
        for (Village v : villages.values()) if (v.name().equalsIgnoreCase(nameOrId) || v.id().toString().startsWith(nameOrId) || LivingIds.key(v.name()).equals(LivingIds.key(nameOrId))) return Optional.of(v);
        return Optional.empty();
    }
    public void restoreVillage(Village v) { villages.put(v.id(), v); for (Building b : v.buildings().values()) buildings.put(b.id(), b); }

    // ------------------------------------------------------------------ buildings

    public Building registerBuilding(UUID villageId, BuildingKind kind, String name, String dimension, double x, double y, double z, double radius, Building.State state, String zoneId) {
        Village v = villages.get(villageId);
        if (v == null) throw new IllegalArgumentException("unknown village " + villageId);
        for (Building b : v.buildings().values())
            if (b.kind() == kind && b.distance(x, z) <= 2.0D) { if (zoneId != null && !zoneId.isEmpty()) b.zoneId(zoneId); if (state == Building.State.BUILT && b.state() == Building.State.PLANNED) setBuildingState(b.id(), Building.State.BUILT, "construido"); return b; }
        UUID id = LivingIds.named("building", villageId + ":" + kind + ":" + (int) Math.floor(x) + ":" + (int) Math.floor(z));
        Building b = new Building(id, villageId, kind, name, dimension, x, y, z, radius, state, now());
        b.zoneId(zoneId);
        if (kind != BuildingKind.HOUSE) b.owner(kind == BuildingKind.TEMPLE ? new OwnerRef(OwnerRef.Kind.TEMPLE, villageId, "templo") : OwnerRef.village(villageId));
        District d = v.districts().computeIfAbsent(kind.district(), k -> new District(LivingIds.named("district", villageId + ":" + k), k, k.name().toLowerCase(java.util.Locale.ROOT), x, z, 12));
        d.buildings().add(id);
        b.district(d.id());
        v.buildings().put(id, b);
        buildings.put(id, b);
        v.layout().addBuilding(id, x, z);
        if (kind == BuildingKind.TEMPLE && v.mainTemple() == null) v.mainTemple(id);
        if (kind == BuildingKind.MARKET && v.market() == null) v.market(id);
        v.markDirty();
        metrics.buildingsRegistered.incrementAndGet();
        publish(new BuildingRegisteredEvent(now(), villageId, id, kind.name(), b.name(), state.name()));
        return b;
    }

    public boolean setBuildingState(UUID buildingId, Building.State state, String reason) {
        Building b = buildings.get(buildingId);
        if (b == null || b.state() == state) return false;
        Building.State before = b.state();
        b.state(state);
        if (state == Building.State.BUILT) { b.builtAt(now()); b.condition(1.0D); }
        if (state == Building.State.DAMAGED) b.condition(Math.min(b.condition(), 0.5D));
        if (state == Building.State.DESTROYED) b.condition(0);
        b.note(state + ": " + reason);
        Village v = villages.get(b.village());
        if (v != null) {
            v.markDirty();
            if (state == Building.State.BUILT && !loading)
                memory.remember(v, Village.Counter.CONSTRUCTIONS, "CONSTRUCTION", "CONSTRUCTION", "Se construye " + b.name() + " en " + v.name(), reason,
                        b.kind() == BuildingKind.TEMPLE || b.kind() == BuildingKind.MARKET ? 0.55D : 0.25D, settings.memorySignificance(), null, now(), chronicle, community, null);
            if (state == Building.State.DESTROYED && b.kind() == BuildingKind.HOUSE) for (HomeRecord h : List.copyOf(v.homes().values())) if (h.house().equals(buildingId)) homes.release(v, h.citizen());
        }
        publish(new BuildingStateChangedEvent(now(), b.village(), buildingId, before.name(), state.name(), reason));
        return true;
    }

    public Optional<Building> building(UUID id) { return Optional.ofNullable(buildings.get(id)); }

    // ------------------------------------------------------------------ citizens

    /**
     * Makes a person a citizen of a village: profession (by NPC type, by the village's needs, by quota), home, membership of
     * the linked knowledge community. Moving from another village counts as leaving it. {@code personalityShift} makes early
     * risers earlier and late sleepers later (minutes).
     */
    public Citizen admit(UUID villageId, UUID npc, String name, String npcType, boolean embodied, int personalityShift) {
        refresh();
        Village v = villages.get(villageId);
        if (v == null) throw new IllegalArgumentException("unknown village " + villageId);
        Citizen c = citizens.get(npc);
        if (c != null && villageId.equals(c.village()) && c.present()) { c.embodied(embodied || c.embodied()); if (name != null) c.name(name); return c; }
        if (c != null && c.present() && c.village() != null && !villageId.equals(c.village())) depart(npc, Citizen.Status.MIGRATED, "se muda a " + v.name());
        boolean fresh = c == null;
        if (fresh) {
            c = new Citizen(npc, name, npcType, villageId, now(), embodied);
            c.scheduleOffset(Math.max(-90, Math.min(90, personalityShift + dice.below("offset:" + npc, 0, 41) - 20)));
            citizens.put(npc, c);
        } else { c.village(villageId); c.status(Citizen.Status.RESIDENT); c.embodied(embodied); }
        boolean first = v.citizens().isEmpty();
        v.citizens().add(npc);
        if (c.profession().isEmpty() && worksAge(npc)) {
            ProfessionAssigner.Choice choice = assigner.choose(v, professionCounts(v), professions.forNpcType(c.npcType()), economy.mostNeededProfession(villageId));
            assignProfession(npc, choice.profession(), choice.reason());
        }
        Optional<HomeRecord> home = homes.assign(v, npc, c.profession(), now(), dice);
        home.ifPresent(h -> { citizenHome(npc, h); });
        community.join(npc, v.communityKey(), first);
        v.count(Village.Counter.ARRIVALS);
        v.markDirty();
        citizensDirty = true;
        metrics.citizensJoined.incrementAndGet();
        publish(new CitizenJoinedEvent(now(), villageId, npc, c.name(), c.profession(), embodied));
        reportPopulation(v);
        return c;
    }

    private void citizenHome(UUID npc, HomeRecord h) {
        Citizen c = citizens.get(npc);
        if (c != null) c.home(h.house());
        metrics.homesAssigned.incrementAndGet();
        publish(new HomeAssignedEvent(now(), c == null ? null : c.village(), npc, h.house(), h.room()));
    }

    private boolean worksAge(UUID npc) {
        String stage = family.lifeStage(npc);
        return stage == null || !(stage.startsWith("INFANT") || stage.startsWith("CHILD"));
    }

    /** A citizen leaves: moved away, went missing or died. The record stays (history); the bed and the community seat are freed. */
    public boolean depart(UUID npc, Citizen.Status status, String reason) {
        Citizen c = citizens.get(npc);
        if (c == null || !c.present()) return false;
        Village v = c.village() == null ? null : villages.get(c.village());
        c.status(status);
        if (v != null) {
            v.citizens().remove(npc);
            homes.release(v, npc);
            v.nightWatch().remove(npc);
            v.stalls().remove(npc);
            community.leave(npc, v.communityKey());
            v.count(Village.Counter.DEPARTURES);
            v.markDirty();
            reportPopulation(v);
        }
        c.home(null);
        schedules.forget(npc);
        citizensDirty = true;
        metrics.citizensLeft.incrementAndGet();
        publish(new CitizenLeftEvent(now(), c.village(), npc, c.name(), status.name(), reason));
        return true;
    }

    public boolean assignProfession(UUID npc, String profession, String reason) {
        Citizen c = citizens.get(npc);
        if (c == null || profession == null) return false;
        if (!professions.ids().isEmpty() && !professions.ids().contains(profession)) return false;
        if (profession.equals(c.profession())) return true;
        c.profession(profession);
        schedules.forget(npc);
        citizensDirty = true;
        if (c.village() != null) village(c.village()).ifPresent(Village::markDirty);
        metrics.professionsAssigned.incrementAndGet();
        publish(new ProfessionAssignedEvent(now(), c.village(), npc, profession, reason));
        return true;
    }

    public Optional<Citizen> citizen(UUID npc) { return Optional.ofNullable(citizens.get(npc)); }
    public Collection<Citizen> citizens() { return List.copyOf(citizens.values()); }
    public List<Citizen> citizensOf(UUID village) {
        Village v = villages.get(village);
        if (v == null) return List.of();
        List<Citizen> out = new ArrayList<>();
        for (UUID id : v.citizens()) { Citizen c = citizens.get(id); if (c != null) out.add(c); }
        return out;
    }
    public void restoreCitizen(Citizen c) { citizens.put(c.id(), c); }
    public Optional<HomeRecord> home(UUID npc) { return villageOf(npc).map(v -> v.homes().get(npc)); }
    public List<UUID> neighbours(UUID npc) { return villageOf(npc).map(v -> homes.neighbours(v, npc, 24)).orElse(List.of()); }

    /** Hours of work a citizen really did (the scheduler reported a finished work routine). */
    public void recordWork(UUID npc, double hours) {
        Citizen c = citizens.get(npc);
        if (c == null || hours <= 0) return;
        c.worked(hours, clock.currentDay());
        citizensDirty = true;
        if (c.village() != null) village(c.village()).ifPresent(Village::markDirty);
    }

    public Map<String, Integer> professionCounts(Village v) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (UUID id : v.citizens()) { Citizen c = citizens.get(id); if (c != null && !c.profession().isEmpty()) out.merge(c.profession(), 1, Integer::sum); }
        return out;
    }

    // ------------------------------------------------------------------ schedules

    private DailyScheduleEngine.Context context(Village v, Citizen c, CalendarDate date) {
        boolean festive = !v.activeEvents(now()).stream().filter(e -> e.kind() == VillageEventKind.FESTIVAL || e.kind() == VillageEventKind.CELEBRATION).toList().isEmpty();
        boolean night = GuardEngine.GUARD_PROFESSIONS.contains(c.profession()) && guards.nightWatch(v, c.id());
        return new DailyScheduleEngine.Context(v.culture(), c.profession(), night, calendar.sunriseShift(), festive, c.scheduleOffset());
    }

    /** Today's plan for a citizen (empty when not a present citizen). */
    public Optional<DayPlan> plan(UUID npc) {
        Citizen c = citizens.get(npc);
        if (c == null || !c.present() || c.village() == null) return Optional.empty();
        Village v = villages.get(c.village());
        if (v == null) return Optional.empty();
        CalendarDate date = clock.today();
        return Optional.of(schedules.plan(npc, date.dayIndex(), context(v, c, date)));
    }

    public String plannedRoutine(UUID npc) { return plan(npc).map(p -> p.routineAt(clock.today().minuteOfDay())).orElse(""); }

    /** Hours of work a citizen's plan puts between two instants (used by the economy when the region is simulated abstractly). */
    public double plannedWorkHours(UUID npc, long from, long to) {
        Optional<DayPlan> plan = plan(npc);
        if (plan.isEmpty() || to <= from || !worksAge(npc)) return 0;
        int perDay = clock.minutesPerDay();
        if (to - from >= perDay) return plan.get().workHours() * (to - from) / (double) perDay;
        int a = (int) Math.floorMod(from, (long) perDay), b = (int) Math.floorMod(to, (long) perDay);
        double minutes = a <= b ? plan.get().minutesOf(DayPlan.WORKING, a, b) : plan.get().minutesOf(DayPlan.WORKING, a, perDay) + plan.get().minutesOf(DayPlan.WORKING, 0, b);
        return minutes / 60.0D;
    }

    private Planned planned(Village v) {
        Planned p = plannedCache.get(v.id());
        long now = now();
        if (p != null && now - p.minute() < 10) return p;
        Map<UUID, String> routines = new HashMap<>(), trades = new HashMap<>();
        int minute = clock.today().minuteOfDay();
        for (UUID id : v.citizens()) {
            Citizen c = citizens.get(id);
            if (c == null) continue;
            trades.put(id, c.profession());
            plan(id).ifPresent(plan -> routines.put(id, plan.routineAt(minute)));
        }
        p = new Planned(now, routines, trades);
        plannedCache.put(v.id(), p);
        return p;
    }

    /**
     * The village's pull on one citizen's routines right now: the day plan, the profession, guard duty, the temple, village
     * events (festivals, markets, attacks...), security, weather and cold, the social pull of neighbours and colleagues, and
     * age. Returned to the Behavior Scheduler as extra points per routine; the scheduler still decides.
     */
    public Bias routineBias(UUID npc) {
        long started = System.nanoTime();
        metrics.biasQueries.incrementAndGet();
        Citizen c = citizens.get(npc);
        if (c == null || !c.present() || c.village() == null) return Bias.NONE;
        Village v = villages.get(c.village());
        if (v == null) return Bias.NONE;
        VillageSettings s = settings;
        CalendarDate date = clock.today();
        int minute = date.minuteOfDay();
        Map<String, Double> bias = new HashMap<>();
        List<String> why = new ArrayList<>();
        DayPlan plan = schedules.plan(npc, date.dayIndex(), context(v, c, date));
        String planned = plan.routineAt(minute);
        add(bias, planned, s.scheduleBias());
        why.add("horario: " + planned + " (" + plan.label() + ")");
        boolean guard = GuardEngine.GUARD_PROFESSIONS.contains(c.profession());
        professions.get(c.profession()).ifPresent(p -> {
            if (DayPlan.WORKING.contains(planned)) { p.bias().forEach((k, val) -> add(bias, k, val * s.professionBiasScale())); why.add("oficio " + p.id()); }
        });
        if (guard) { guards.bias(v, npc, minute).forEach((k, val) -> add(bias, k, val)); why.add(guards.onDuty(v, npc, minute) ? "de guardia" : "fuera de turno"); }
        temple.bias(v, date.phase(), "monk".equals(c.profession())).forEach((k, val) -> add(bias, k, val));
        if (v.ritualActive()) why.add("ritual en el templo");
        for (VillageEventRecord e : v.activeEvents(now())) {
            e.bias().forEach((k, val) -> add(bias, k, val * s.eventBiasScale()));
            if (guard) e.guardBias().forEach((k, val) -> add(bias, k, val * s.eventBiasScale()));
            why.add("evento " + e.title());
        }
        SecurityState sec = v.security().state();
        if (!guard && sec == SecurityState.DANGER) { add(bias, "REST", 30); add(bias, "SOCIAL", -20); add(bias, "WORK", -15); why.add("peligro: quedarse en casa"); }
        WeatherKind weather = calendar.weather("region:" + v.region());
        if (weather.outdoorFactor() < 1.0D && (OUTDOOR.contains(c.profession()) || guard)) {
            double penalty = s.weatherPenalty() * (1.0D - weather.outdoorFactor());
            add(bias, guard ? "PATROL" : "WORK", -penalty);
            add(bias, "REST", penalty * 0.4D);
            why.add("clima " + weather);
        }
        double temperature = calendar.temperature("region:" + v.region());
        if (temperature < s.coldThreshold() && !planned.equals("SLEEP")) { add(bias, "REST", 10); add(bias, "SOCIAL", -8); why.add(String.format("frío %.0f°C", temperature)); }
        if (SocialLifeEngine.safeToGather(v) && !planned.equals("SLEEP")) {
            Planned p = planned(v);
            double pull = social.pull(npc, homes.neighbours(v, npc, 24), p.professions(), p.routines(), s.neighbourSocialBias(), s.neighbourSocialBias() * 3);
            if (pull > 0) { add(bias, "SOCIAL", pull); why.add("sus vecinos se reúnen"); }
        }
        String stage = family.lifeStage(npc);
        if (stage != null && (stage.startsWith("CHILD") || stage.startsWith("INFANT"))) { add(bias, "WORK", -100); add(bias, "SOCIAL", 20); why.add("es un niño"); }
        else if ("ELDER".equals(stage)) { add(bias, "WORK", -20); add(bias, "REST", 10); why.add("anciano"); }
        metrics.biasNanos.addAndGet(System.nanoTime() - started);
        return new Bias(bias, why, planned);
    }

    private static void add(Map<String, Double> bias, String routine, double value) { if (value != 0) bias.merge(routine, value, Double::sum); }

    // ------------------------------------------------------------------ events, security, visitors

    public VillageEventRecord startEvent(UUID villageId, VillageEventKind kind, String title, long start, long end, String source, Map<String, Double> bias, Map<String, Double> guardBias) {
        Village v = villages.get(villageId);
        if (v == null) throw new IllegalArgumentException("unknown village " + villageId);
        for (int i = 0; i < v.events().size(); i++) {
            VillageEventRecord e = v.events().get(i);
            if (e.kind() != kind || !e.source().equals(source) || e.end() <= now()) continue;
            if (end <= e.end()) return e;
            VillageEventRecord extended = new VillageEventRecord(e.id(), e.kind(), e.title(), e.start(), end, e.source(), e.bias(), e.guardBias());   // still going: extend
            v.events().set(i, extended);
            return extended;
        }
        VillageEventRecord e = new VillageEventRecord(UUID.randomUUID(), kind, title, start, Math.max(start + 1, end), source, bias, guardBias);
        v.events().add(e);
        v.markDirty();
        metrics.eventsStarted.incrementAndGet();
        publish(new VillageEventStartedEvent(start, villageId, e.id(), kind.name(), title, source));
        plannedCache.remove(villageId);
        return e;
    }

    /** Ends every open village event that came from {@code source} (the world event finished, the festival ended). */
    public int endEvents(UUID villageId, String source) {
        Village v = villages.get(villageId);
        if (v == null) return 0;
        int n = 0;
        long now = now();
        for (int i = 0; i < v.events().size(); i++) {
            VillageEventRecord e = v.events().get(i);
            if (!e.source().equals(source) || e.end() <= now) continue;
            v.events().set(i, new VillageEventRecord(e.id(), e.kind(), e.title(), e.start(), now, e.source(), e.bias(), e.guardBias()));
            if (endAnnounced.add(e.id())) publish(new VillageEventEndedEvent(now, villageId, e.id(), e.kind().name(), e.title()));
            n++;
        }
        if (n > 0) v.markDirty();
        return n;
    }

    public void reportThreat(UUID villageId, double amount, String reason) {
        Village v = villages.get(villageId);
        if (v == null || amount <= 0) return;
        v.security().report(amount, reason, now(), settings.threatDecayPerHour());
        updateSecurity(v);
    }

    public void attackStarted(UUID villageId, UUID worldEvent, String title) {
        Village v = villages.get(villageId);
        if (v == null) return;
        v.security().attacks().add(worldEvent);
        startEvent(villageId, VillageEventKind.ATTACK, title, now(), now() + 7L * 1440, "world-event:" + worldEvent, null, null);
        updateSecurity(v);
    }

    /** An attack ended; {@code repelled} records it in the community's memory and renown. */
    public void attackEnded(UUID villageId, UUID worldEvent, boolean repelled, String by) {
        Village v = villages.get(villageId);
        if (v == null) return;
        v.security().attacks().remove(worldEvent);
        endEvents(villageId, "world-event:" + worldEvent);
        updateSecurity(v);
        if (repelled) {
            v.renown(3, "ataque rechazado");
            memory.remember(v, Village.Counter.ATTACKS_REPELLED, "BATTLE", "BATTLE", "Ataque rechazado en " + v.name(), by, 0.65D, settings.memorySignificance(), null, now(), chronicle, community,
                    Provenance.of("world-event", worldEvent.toString(), "defensa", now()));
        }
    }

    private void updateSecurity(Village v) {
        SecurityRuntime.Change change = v.security().update(now(), settings.threatDecayPerHour(), settings.alertThreshold(), settings.dangerThreshold(), settings.recoveryMinutes());
        if (change == null) return;
        v.markDirty();
        metrics.securityChanges.incrementAndGet();
        plannedCache.remove(v.id());
        publish(new VillageSecurityChangedEvent(now(), v.id(), change.from().name(), change.to().name(), change.reason(), v.security().threat()));
    }

    public VisitorRecord addVisitor(UUID villageId, VisitorRecord.Kind kind, String name, UUID npc, UUID origin, double hours, String purpose) {
        Village v = villages.get(villageId);
        if (v == null) throw new IllegalArgumentException("unknown village " + villageId);
        VisitorRecord r = new VisitorRecord(UUID.randomUUID(), kind, name, npc, origin, now(), now() + (long) (hours * 60), purpose);
        v.visitors().add(r);
        v.count(Village.Counter.VISITORS);
        metrics.visitorsArrived.incrementAndGet();
        publish(new VisitorArrivedEvent(now(), villageId, r.id(), kind.name(), name, purpose));
        return r;
    }

    /** Visitors present now (arrived and not gone). */
    public int visitorsPresent(UUID villageId) {
        Village v = villages.get(villageId);
        if (v == null) return 0;
        long now = now();
        int n = 0;
        for (VisitorRecord r : v.visitors()) if (r.arrived() <= now && r.phase() != VisitorRecord.Phase.GONE) n++;
        return n;
    }

    public void remember(UUID villageId, Village.Counter counter, String category, String historyType, String title, String detail, double significance, UUID actor, Provenance source) {
        Village v = villages.get(villageId);
        if (v != null) memory.remember(v, counter, category, historyType, title, detail, significance, settings.memorySignificance(), actor, now(), chronicle, community, source);
    }

    // ------------------------------------------------------------------ living: the tick and abstract simulation

    /** Every {@code tickIntervalTicks}: a few villages (round-robin) update their life at full detail. */
    public void tick() {
        refresh();
        tickCount++;
        if (tickCount % settings.tickIntervalTicks() != 0 || villages.isEmpty()) return;
        List<Village> all = new ArrayList<>(villages.values());
        int n = Math.min(settings.villagesPerTick(), all.size());
        for (int i = 0; i < n; i++) update(all.get((cursor + i) % all.size()));
        cursor = (cursor + n) % Math.max(1, all.size());
    }

    /** Brings one village's life up to now. */
    public void update(Village v) {
        long started = System.nanoTime();
        long now = now();
        CalendarDate date = clock.today();
        metrics.villageUpdates.incrementAndGet();
        updateSecurity(v);
        // village events: expire, festivals of the day, rain
        for (VillageEventRecord e : List.copyOf(v.events())) if (e.end() <= now && endAnnounced.add(e.id())) publish(new VillageEventEndedEvent(now, v.id(), e.id(), e.kind().name(), e.title()));
        v.events().removeIf(e -> { boolean old = e.end() <= now - 1440; if (old) endAnnounced.remove(e.id()); return old; });
        if (!Long.valueOf(date.dayIndex()).equals(festivalsChecked.get(v.id()))) {
            festivalsChecked.put(v.id(), date.dayIndex());
            List<VillagePorts.FestivalInfo> today = calendar.festivals(v.culture());
            for (VillagePorts.FestivalInfo f : today) {
                Map<String, Double> scaled = new HashMap<>();
                f.bias().forEach((k, val) -> scaled.put(k, val * settings.festivalBiasScale()));
                startEvent(v.id(), VillageEventKind.FESTIVAL, f.name(), now, (date.dayIndex() + 1) * clock.minutesPerDay(), "festival:" + f.id(), scaled, null);
                v.renown(0.5, "celebra " + f.name());
            }
            for (VillageEventRecord e : List.copyOf(v.events()))
                if (e.kind() == VillageEventKind.FESTIVAL && today.stream().noneMatch(f -> ("festival:" + f.id()).equals(e.source()))) endEvents(v.id(), e.source());
            if (!today.isEmpty()) v.count(Village.Counter.FESTIVALS);
        }
        WeatherKind weather = calendar.weather("region:" + v.region());
        if (weather.wet()) startEvent(v.id(), VillageEventKind.RAIN, "Lluvia", now, now + 120, "weather", null, null);
        else endEvents(v.id(), "weather");
        // who does what now
        Planned p = planned(v);
        List<UUID> merchants = new ArrayList<>(), guardList = new ArrayList<>();
        int customers = 0, socialisers = 0;
        for (UUID id : v.citizens()) {
            String prof = p.professions().getOrDefault(id, "");
            String routine = p.routines().getOrDefault(id, "");
            if ("merchant".equals(prof) && "MERCHANT".equals(routine)) merchants.add(id);
            else if ("MERCHANT".equals(routine)) customers++;
            if ("SOCIAL".equals(routine)) socialisers++;
            if (GuardEngine.GUARD_PROFESSIONS.contains(prof)) guardList.add(id);
        }
        for (VisitorRecord r : v.visitors()) if (r.phase() == VisitorRecord.Phase.INTERACT) customers++;
        List<VillageEventRecord> active = v.activeEvents(now);
        switch (market.update(v, date.phase(), active, merchants, customers)) {
            case OPENED -> { metrics.marketOpenings.incrementAndGet(); publish(new MarketOpenedEvent(now, v.id(), v.stalls().size())); }
            case CLOSED -> publish(new MarketClosedEvent(now, v.id(), v.security().state() == SecurityState.ATTACK ? "ataque" : "fin de la jornada"));
            default -> { }
        }
        switch (temple.update(v, date.phase(), active)) {
            case RITUAL_STARTED -> { metrics.rituals.incrementAndGet(); publish(new TempleRitualEvent(now, v.id(), true, date.phase() == DayPhase.DAWN ? "oración del alba" : date.phase() == DayPhase.SUNSET ? "ritual del ocaso" : "ceremonia")); }
            case RITUAL_ENDED -> publish(new TempleRitualEvent(now, v.id(), false, ""));
            default -> { }
        }
        guards.roster(v, guardList, settings.nightWatchShare());
        boolean day = date.minuteOfDay() >= 6 * 60 && date.minuteOfDay() < 18 * 60;
        Boolean last = dayShift.put(v.id(), day);
        if (last != null && last != day) { metrics.shiftChanges.incrementAndGet(); publish(new GuardShiftChangedEvent(now, v.id(), !day, guards.onDutyCount(v, guardList, date.minuteOfDay()))); }
        VisitorEngine.Changes visits = visitorEngine.advance(v.visitors(), now, date.phase(), v.marketOpen() || v.ritualActive());
        for (VisitorRecord r : visits.left()) publish(new VisitorLeftEvent(now, v.id(), r.id(), r.kind().name(), r.name()));
        double festivalSocial = 1.0D;
        for (VillagePorts.FestivalInfo f : calendar.festivals(v.culture())) festivalSocial = Math.max(festivalSocial, f.social());
        social.activity(v, socialisers, calendar.seasonSocial(), festivalSocial);
        districtActivity(v, p);
        v.prosperity(economy.prosperity(v.id()));
        double cover = economy.foodCoverDays(v.id());
        v.unrest(cover < settings.unrestFoodDays() ? v.unrest() + 0.02D : v.unrest() - 0.01D);
        int homeless = homes.homeless(v);
        Integer lastHomeless = reportedHomeless.put(v.id(), homeless);
        if (homeless > 0 && (lastHomeless == null || lastHomeless != homeless)) publish(new HousingShortageEvent(now, v.id(), homeless));
        reportPopulation(v);
        v.lastSimulated(Math.max(v.lastSimulated(), now));
        metrics.updateNanos.addAndGet(System.nanoTime() - started);
    }

    private void districtActivity(Village v, Planned p) {
        Map<DistrictKind, Integer> present = new HashMap<>();
        for (var e : p.routines().entrySet()) {
            DistrictKind d = switch (e.getValue()) {
                case "SLEEP", "REST", "EAT", "WAKE" -> DistrictKind.RESIDENTIAL;
                case "MERCHANT", "SOCIAL" -> DistrictKind.MARKET;
                case "PRAYER", "MEDITATE" -> DistrictKind.SPIRITUAL;
                case "GUARD", "PATROL", "TRAINING" -> DistrictKind.MILITARY;
                default -> switch (p.professions().getOrDefault(e.getKey(), "")) {
                    case "farmer", "fisherman" -> DistrictKind.AGRICULTURAL; case "woodcutter", "miner", "hunter", "herbalist" -> DistrictKind.OUTER_FOREST; default -> DistrictKind.CRAFTS;
                };
            };
            present.merge(d, 1, Integer::sum);
        }
        for (District d : v.districts().values()) {
            int capacity = 0;
            for (UUID b : d.buildings()) { Building x = v.buildings().get(b); if (x != null && x.usable()) capacity += x.capacity(); }
            int people = present.getOrDefault(d.kind(), 0);
            d.activity(capacity == 0 ? 0 : people / (double) capacity, people);
        }
    }

    private void reportPopulation(Village v) {
        int n = v.citizens().size();
        Integer last = reportedPopulation.put(v.id(), n);
        if (last == null || last != n) publish(new VillagePopulationChangedEvent(now(), v.id(), n, professionCounts(v)));
    }

    /**
     * Abstract simulation of a village over [from, to) (the world simulates its region without a player near): visitors come and
     * go, security calms down, and citizens who are not being simulated in full accrue the work hours their plans give them.
     */
    public void simulate(UUID villageId, long from, long to, boolean fullDetail) {
        Village v = villages.get(villageId);
        if (v == null || to <= from) return;
        metrics.simulations.incrementAndGet();
        int perDay = clock.minutesPerDay();
        long lastDay = visitorsDrawn.getOrDefault(villageId, Math.floorDiv(from, (long) perDay) - 1);
        long toDay = Math.floorDiv(to, (long) perDay);
        int roads = 1;
        for (long d = Math.max(lastDay + 1, toDay - 30); d <= toDay; d++) {
            int room = settings.maxVisitors() - v.visitors().size();
            for (VisitorRecord r : visitorEngine.draw(villageId, d, Math.max(from, d * perDay), dice, settings.visitorsPerDay(), roads, 1.0D - Math.min(1.0D, v.security().threat() / 100.0D),
                    calendar.holyDay(), v.mainTemple() != null, settings.visitorMinStayHours(), settings.visitorMaxStayHours(), room)) {
                v.visitors().add(r);
                v.count(Village.Counter.VISITORS);
                metrics.visitorsArrived.incrementAndGet();
                publish(new VisitorArrivedEvent(r.arrived(), villageId, r.id(), r.kind().name(), r.name(), r.purpose()));
            }
        }
        visitorsDrawn.put(villageId, toDay);
        if (!fullDetail) {
            for (UUID id : v.citizens()) {
                Citizen c = citizens.get(id);
                if (c == null || c.profession().isEmpty()) continue;
                double hours = plannedWorkHours(id, from, to);
                if (hours > 0) c.worked(hours, Math.floorDiv(to - 1, (long) perDay));
            }
            citizensDirty = true;
            v.markDirty();
            update(v);
        }
        v.lastSimulated(to);
    }

    public VillageCensus census(UUID villageId) {
        Village v = villages.get(villageId);
        if (v == null) return new VillageCensus(0, 0, Map.of(), 0, 0, 0, 0, 0, 0);
        int abstractCitizens = 0, guardCount = 0, monks = 0, children = 0, elders = 0;
        for (UUID id : v.citizens()) {
            Citizen c = citizens.get(id);
            if (c == null) continue;
            if (!c.embodied()) abstractCitizens++;
            if (GuardEngine.GUARD_PROFESSIONS.contains(c.profession())) guardCount++;
            if ("monk".equals(c.profession())) monks++;
            String stage = family.lifeStage(id);
            if (stage != null && (stage.startsWith("CHILD") || stage.startsWith("INFANT"))) children++;
            if ("ELDER".equals(stage)) elders++;
        }
        return new VillageCensus(v.citizens().size(), abstractCitizens, professionCounts(v), guardCount, monks, children, elders, visitorsPresent(villageId), homes.homeless(v));
    }

    // ------------------------------------------------------------------ accessors

    public VillageSettings settings() { return settings; }
    public VillageMetrics metrics() { return metrics; }
    public DailyScheduleEngine schedules() { return schedules; }
    public HomeEngine homes() { return homes; }
    public GuardEngine guards() { return guards; }
    public WorldClock clock() { return clock; }
    public long seed() { return seed; }
    public List<String> problems() { return schedules.problems(); }
    public boolean citizensDirty() { return citizensDirty; }
    public void cleanCitizens() { citizensDirty = false; }

    public void reset() {
        villages.clear(); citizens.clear(); buildings.clear(); plannedCache.clear(); reportedPopulation.clear(); dayShift.clear(); visitorsDrawn.clear(); festivalsChecked.clear();
        reportedHomeless.clear(); endAnnounced.clear(); metrics.reset(); schedules.invalidateAll(); tickCount = 0; cursor = 0; citizensDirty = false;
    }
}
