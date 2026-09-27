package yadi.samuraiai.living.world.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
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
import yadi.samuraiai.living.core.Season;
import yadi.samuraiai.living.core.TickBudget;
import yadi.samuraiai.living.core.WeatherKind;
import yadi.samuraiai.living.core.WorldClock;
import yadi.samuraiai.living.world.environment.EnvironmentInteractionEngine;
import yadi.samuraiai.living.world.events.WorldEventEngine;
import yadi.samuraiai.living.world.events.WorldEventPhase;
import yadi.samuraiai.living.world.events.WorldEventRecord;
import yadi.samuraiai.living.world.events.WorldEventType;
import yadi.samuraiai.living.world.events_api.CatchUpCompletedEvent;
import yadi.samuraiai.living.world.events_api.RegionActivatedEvent;
import yadi.samuraiai.living.world.events_api.RegionCreatedEvent;
import yadi.samuraiai.living.world.events_api.RegionDangerChangedEvent;
import yadi.samuraiai.living.world.events_api.RegionSleepingEvent;
import yadi.samuraiai.living.world.events_api.RoadBlockedEvent;
import yadi.samuraiai.living.world.events_api.RoadCreatedEvent;
import yadi.samuraiai.living.world.events_api.RoadReopenedEvent;
import yadi.samuraiai.living.world.events_api.SettlementFoundedEvent;
import yadi.samuraiai.living.world.events_api.SettlementStatusChangedEvent;
import yadi.samuraiai.living.world.events_api.WorldEventPhaseEvent;
import yadi.samuraiai.living.world.events_api.WorldPopulationChangedEvent;
import yadi.samuraiai.living.world.metrics.WorldMetrics;
import yadi.samuraiai.living.world.population.PopulationLedger;
import yadi.samuraiai.living.world.professions.ProfessionCatalog;
import yadi.samuraiai.living.world.regions.Region;
import yadi.samuraiai.living.world.regions.RegionCatalog;
import yadi.samuraiai.living.world.regions.RegionType;
import yadi.samuraiai.living.world.regions.ResourceDeposit;
import yadi.samuraiai.living.world.roads.RoadEdge;
import yadi.samuraiai.living.world.roads.RoadNetwork;
import yadi.samuraiai.living.world.roads.RoadNode;
import yadi.samuraiai.living.world.roads.RoutePlan;
import yadi.samuraiai.living.world.settlements.Settlement;
import yadi.samuraiai.living.world.settlements.SettlementType;
import yadi.samuraiai.living.world.simulation.RegionSimulator;
import yadi.samuraiai.living.world.simulation.SimulationEngine;
import yadi.samuraiai.living.world.streaming.SimulationLevel;
import yadi.samuraiai.living.world.streaming.StreamingEngine;
import yadi.samuraiai.living.world.wildlife.SpeciesDef;
import yadi.samuraiai.living.world.wildlife.WildlifeEngine;

/**
 * The Living World Engine: "what exists and what is happening in the world". It owns the regions (identity, danger, natural
 * resources, wildlife), the settlements (that they exist and where), the road network, the world events, streaming (level of
 * detail per region), the simulation schedule, the population aggregate and the profession catalogue, and offers them to the
 * other engines through its API. It does not manage citizens, inventories, quests or families; those engines reach it through
 * the hub.
 *
 * <p>Pure Java. It knows the world through {@link WorldPorts}: a classifier for land, the environment (season, weather) and
 * the chronicle where history is written.
 */
public final class WorldEngine {
    private final Supplier<WorldSettings> settingsSupplier;
    private final WorldClock clock;
    private final WorldMetrics metrics = new WorldMetrics();
    private final Map<UUID, Region> regions = new LinkedHashMap<>();
    private final Map<String, UUID> regionByKey = new HashMap<>();
    private final Map<UUID, Settlement> settlements = new LinkedHashMap<>();
    private final RoadNetwork roads = new RoadNetwork();
    private final WorldEventEngine worldEvents = new WorldEventEngine();
    private final StreamingEngine streaming = new StreamingEngine();
    private final SimulationEngine simulation = new SimulationEngine();
    private final PopulationLedger population = new PopulationLedger();
    private final EnvironmentInteractionEngine interactions = new EnvironmentInteractionEngine();
    private final Set<UUID> eventBlocked = new HashSet<>();
    private EventSink bus;
    private WorldSettings settings;
    private RegionCatalog catalog;
    private WildlifeEngine wildlife;
    private ProfessionCatalog professions;
    private Dice dice;
    private long seed;
    private WorldPorts.RegionClassifier classifier = WorldPorts.FIELDS_EVERYWHERE;
    private WorldPorts.Environment environment = WorldPorts.NEUTRAL;
    private WorldPorts.Chronicle chronicle = WorldPorts.SILENT;
    private List<StreamingEngine.Viewer> viewers = List.of();
    private long tickCount;
    private int reportedPopulation = -1;
    private boolean settlementsDirty, regionsDirty;

    public WorldEngine(Supplier<WorldSettings> settings, EventSink bus, WorldClock clock, long seed) {
        this.settingsSupplier = Objects.requireNonNull(settings);
        this.bus = Objects.requireNonNull(bus);
        this.clock = Objects.requireNonNull(clock);
        this.seed = seed;
        this.dice = new Dice(seed);
        rebuild(settings.get());
        simulation.register(new RegionSimulator() {
            @Override public String name() { return "wildlife"; }
            @Override public void simulate(Region region, long from, long to, SimulationLevel level, boolean catchUp) { simulateWildlife(region, to); }
        });
        worldEvents.listen(this::onWorldEventPhase);
    }

    private void rebuild(WorldSettings s) {
        this.settings = s;
        this.catalog = new RegionCatalog(s.regions());
        this.wildlife = new WildlifeEngine(s.wildlife());
        this.professions = new ProfessionCatalog(s.professions());
        streaming.configure(s.fullRadius(), s.nearRadius(), s.settlementRadius(), (long) s.historicalAfterDays() * clock.minutesPerDay());
        simulation.configure(s.stepMinutesActive(), s.stepMinutesSettlement(), s.stepMinutesAbstract(), s.stepMinutesHistorical(), s.maxRegionsPerTick(), s.maxCatchUpSteps());
        worldEvents.configure(s.maxOpenEvents(), s.eventArchive(), s.eventConsequenceMinutes());
        population.configure(s.populationLog());
        interactions.configure(s.maxInteractionOrders());
    }

    private void refreshSettings() { WorldSettings s = settingsSupplier.get(); if (s != settings) rebuild(s); }

    public void useEventSink(EventSink sink) { this.bus = Objects.requireNonNull(sink); }
    public void useClassifier(WorldPorts.RegionClassifier c) { this.classifier = Objects.requireNonNull(c); }
    public void useEnvironment(WorldPorts.Environment e) { this.environment = Objects.requireNonNull(e); }
    public void useChronicle(WorldPorts.Chronicle c) { this.chronicle = Objects.requireNonNull(c); }
    public void useSeed(long s) { this.seed = s; this.dice = new Dice(s); }
    private void publish(NpcEvent e) { bus.publish(e); }

    // ------------------------------------------------------------------ regions

    public String regionKey(String dimension, double x, double z) {
        int size = settings.regionCellSize();
        return dimension + ":" + Math.floorDiv((int) Math.floor(x), size) + ":" + Math.floorDiv((int) Math.floor(z), size);
    }

    public Optional<Region> regionAt(String dimension, double x, double z) {
        UUID id = regionByKey.get(regionKey(dimension, x, z));
        return id == null ? Optional.empty() : Optional.ofNullable(regions.get(id));
    }

    /** The region at a position, created (classified, endowed with resources and wildlife) the first time it is needed. */
    public Region ensureRegion(String dimension, double x, double z) {
        String key = regionKey(dimension, x, z);
        UUID existing = regionByKey.get(key);
        if (existing != null) return regions.get(existing);
        if (regions.size() >= settings.maxRegions()) throw new IllegalStateException("Too many regions (" + settings.maxRegions() + ")");
        int size = settings.regionCellSize();
        int cx = Math.floorDiv((int) Math.floor(x), size), cz = Math.floorDiv((int) Math.floor(z), size);
        WorldPorts.RegionClassifier.Classification c = classifier.classify(dimension, (cx + 0.5D) * size, (cz + 0.5D) * size);
        UUID id = LivingIds.named("region", key);
        long now = clock.now();
        Region region = new Region(id, key, dimension, cx, cz, size, RegionCatalog.name(key, c.type(), dice), c.type(), c.biome(), cultureFor(c.type()), now);
        region.altitude(c.altitude());
        catalog.endow(region, now);
        wildlife.populate(region, now);
        region.lastPlayerSeen(now);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dz == 0) continue;
            UUID n = regionByKey.get(dimension + ":" + (cx + dx) + ":" + (cz + dz));
            if (n == null) continue;
            region.relations().put(n, Region.Relation.ADJACENT);
            regions.get(n).relations().putIfAbsent(id, Region.Relation.ADJACENT);
        }
        regions.put(id, region);
        regionByKey.put(key, id);
        regionsDirty = true;
        metrics.regionsCreated.incrementAndGet();
        publish(new RegionCreatedEvent(now, id, key, region.name(), c.type().name()));
        return region;
    }

    private static String cultureFor(RegionType type) { return type == RegionType.TEMPLE ? "temple" : "village"; }

    public Optional<Region> region(UUID id) { return Optional.ofNullable(regions.get(id)); }
    public Collection<Region> regions() { return List.copyOf(regions.values()); }
    public int regionCount() { return regions.size(); }

    /** Restores a region from storage (no events, no endowment). */
    public void restoreRegion(Region region) { regions.put(region.id(), region); regionByKey.put(region.key(), region.id()); }

    // ------------------------------------------------------------------ settlements

    /**
     * Founds a settlement (or returns the existing one of the same type whose area already covers the point). It gets a node in
     * the road graph connected to its nearest neighbours, a founding entry in the world timeline and a yearly anniversary.
     */
    public Settlement foundSettlement(String name, SettlementType type, String dimension, double x, double y, double z, double radius, Provenance origin) {
        for (Settlement s : settlements.values()) if (s.active() && s.type() == type && s.contains(dimension, x, z)) return s;
        Region region = ensureRegion(dimension, x, z);
        long now = clock.now();
        String key = LivingIds.key(name.isBlank() ? type.name() + "-" + region.key() : name);
        UUID id = LivingIds.named("settlement", key + "@" + dimension + ":" + (int) x + ":" + (int) z);
        Settlement s = new Settlement(id, key, name.isBlank() ? region.name() : name, type, region.id(), dimension, x, y, z, radius, now,
                origin == null ? Provenance.of("world", "", "founded", now) : origin);
        settlements.put(id, s);
        region.settlements().add(id);
        region.markDirty();
        if (region.type() == RegionType.FIELDS || region.type() == RegionType.FOREST) { /* a settlement makes the land inhabited */ }
        RoadNode node = roads.addNode(new RoadNode(LivingIds.named("road-node", id.toString()), RoadNode.Kind.SETTLEMENT, s.name(), dimension, x, z, id));
        s.roadNode(node.id());
        connect(node);
        settlementsDirty = true;
        metrics.settlementsFounded.incrementAndGet();
        publish(new SettlementFoundedEvent(now, id, region.id(), s.name(), type.name()));
        chronicle.record(now, "FOUNDING", "Fundación de " + s.name(), type.name().toLowerCase(java.util.Locale.ROOT) + " en " + region.name(),
                Set.of(s.scope(), region.scope(), "world"), 0.8D, s.origin());
        chronicle.anniversary("SETTLEMENT_FOUNDING", s.scope(), "Fundación de " + s.name(), now);
        return s;
    }

    private void connect(RoadNode node) {
        for (RoadNode other : roads.nearestSettlements(node, settings.roadsPerSettlement(), settings.maxRoadLength())) {
            List<UUID> crossed = regionsAlong(node.dimension(), node.x(), node.z(), other.x(), other.z());
            int bridges = 0;
            for (UUID r : crossed) if (regions.get(r) != null && regions.get(r).type() == RegionType.RIVER) bridges++;
            roads.connect(node.id(), other.id(), RoadEdge.Kind.ROAD, settings.roadTortuosity(), bridges, crossed).ifPresent(edge -> {
                rateEdge(edge);
                metrics.roadsBuilt.incrementAndGet();
                publish(new RoadCreatedEvent(clock.now(), edge.id(), edge.a(), edge.b(), edge.length()));
            });
        }
    }

    /** The regions a straight segment crosses (sampled every half cell), created when missing so their danger is known. */
    private List<UUID> regionsAlong(String dimension, double x1, double z1, double x2, double z2) {
        Set<UUID> out = new LinkedHashSet<>();
        double length = Math.hypot(x2 - x1, z2 - z1);
        int samples = Math.max(1, (int) Math.ceil(length / (settings.regionCellSize() / 2.0D)));
        for (int i = 0; i <= samples; i++) {
            double t = i / (double) samples;
            out.add(ensureRegion(dimension, x1 + (x2 - x1) * t, z1 + (z2 - z1) * t).id());
        }
        return new ArrayList<>(out);
    }

    private void rateEdge(RoadEdge edge) {
        double sum = 0, event = 0;
        int n = 0;
        for (UUID r : edge.regions()) { Region region = regions.get(r); if (region == null) continue; sum += region.baseDanger(); event = Math.max(event, region.eventDanger()); n++; }
        edge.landDanger(n == 0 ? 0 : sum / n);
        edge.eventDanger(event);
    }

    /** Adds a manual road (a player or an admin built one) between two settlements. */
    public Optional<RoadEdge> buildRoad(UUID settlementA, UUID settlementB, RoadEdge.Kind kind) {
        Settlement a = settlements.get(settlementA), b = settlements.get(settlementB);
        if (a == null || b == null || a.roadNode() == null || b.roadNode() == null) return Optional.empty();
        List<UUID> crossed = regionsAlong(a.dimension(), a.x(), a.z(), b.x(), b.z());
        int bridges = 0;
        for (UUID r : crossed) if (regions.get(r) != null && regions.get(r).type() == RegionType.RIVER) bridges++;
        Optional<RoadEdge> edge = roads.connect(a.roadNode(), b.roadNode(), kind, kind == RoadEdge.Kind.SHORTCUT ? 1.05D : settings.roadTortuosity(), bridges, crossed);
        edge.ifPresent(e -> { rateEdge(e); metrics.roadsBuilt.incrementAndGet(); publish(new RoadCreatedEvent(clock.now(), e.id(), e.a(), e.b(), e.length())); });
        return edge;
    }

    public boolean blockRoad(UUID edge, String reason) {
        if (!roads.block(edge, reason)) return false;
        metrics.roadsBlocked.incrementAndGet();
        publish(new RoadBlockedEvent(clock.now(), edge, reason));
        return true;
    }

    public boolean reopenRoad(UUID edge) {
        if (!roads.reopen(edge)) return false;
        eventBlocked.remove(edge);
        publish(new RoadReopenedEvent(clock.now(), edge));
        return true;
    }

    /** World-scale route between two settlements for a kind of traveller. */
    public Optional<RoutePlan> route(UUID fromSettlement, UUID toSettlement, RoadNetwork.Preferences prefs) {
        Settlement a = settlements.get(fromSettlement), b = settlements.get(toSettlement);
        if (a == null || b == null || a.roadNode() == null || b.roadNode() == null) return Optional.empty();
        metrics.routesPlanned.incrementAndGet();
        return roads.route(a.roadNode(), b.roadNode(), prefs);
    }

    public Optional<Settlement> settlement(UUID id) { return Optional.ofNullable(settlements.get(id)); }
    public Collection<Settlement> settlements() { return List.copyOf(settlements.values()); }
    public List<Settlement> settlementsIn(UUID region) { return settlements.values().stream().filter(s -> s.regionId().equals(region)).toList(); }
    public Optional<Settlement> settlementAt(String dimension, double x, double z) {
        Settlement best = null;
        for (Settlement s : settlements.values()) if (s.active() && s.contains(dimension, x, z) && (best == null || s.distance(x, z) < best.distance(x, z))) best = s;
        return Optional.ofNullable(best);
    }
    public Optional<Settlement> nearestSettlement(String dimension, double x, double z, double maxDistance) {
        Settlement best = null;
        for (Settlement s : settlements.values()) if (s.active() && s.dimension().equals(dimension) && s.distance(x, z) <= maxDistance && (best == null || s.distance(x, z) < best.distance(x, z))) best = s;
        return Optional.ofNullable(best);
    }
    public Optional<Settlement> findSettlement(String nameOrKey) {
        String k = LivingIds.key(nameOrKey);
        for (Settlement s : settlements.values()) if (s.key().equals(k) || s.name().equalsIgnoreCase(nameOrKey)) return Optional.of(s);
        return Optional.empty();
    }

    public void setSettlementStatus(UUID id, Settlement.Status status, String why) {
        Settlement s = settlements.get(id);
        if (s == null || s.status() == status) return;
        Settlement.Status before = s.status();
        s.status(status);
        settlementsDirty = true;
        publish(new SettlementStatusChangedEvent(clock.now(), id, before.name(), status.name()));
        chronicle.record(clock.now(), status == Settlement.Status.ACTIVE ? "CONSTRUCTION" : "DISASTER", s.name() + ": " + status.name().toLowerCase(java.util.Locale.ROOT), why,
                Set.of(s.scope(), "region:" + s.regionId()), 0.7D, Provenance.of("world", s.id().toString(), why, clock.now()));
    }

    public void restoreSettlement(Settlement s) { settlements.put(s.id(), s); Region r = regions.get(s.regionId()); if (r != null) r.settlements().add(s.id()); }

    // ------------------------------------------------------------------ resources and wildlife (the origin of the economy)

    /** Takes a natural resource from a region's deposit. Returns what was really taken (never more than there is). */
    public double extract(UUID regionId, String resource, double amount) {
        Region r = regions.get(regionId);
        if (r == null) return 0;
        ResourceDeposit d = r.deposits().get(resource);
        if (d == null) return 0;
        double taken = d.extract(amount, clock.now(), clock.minutesPerDay());
        if (taken > 0) { metrics.extractions.incrementAndGet(); r.markDirty(); }
        return taken;
    }

    public double available(UUID regionId, String resource) {
        Region r = regions.get(regionId);
        if (r == null) return 0;
        ResourceDeposit d = r.deposits().get(resource);
        return d == null ? 0 : d.stock(clock.now(), clock.minutesPerDay());
    }

    /**
     * Hunting, fishing, husbandry: obtains up to {@code amount} of a resource from the region's animals (the species that
     * yield it, most numerous first), taking at most {@code maxShare} of each population. Returns the resource obtained.
     */
    public double harvestAnimals(UUID regionId, String resource, double amount, double maxShare) {
        Region r = regions.get(regionId);
        if (r == null || amount <= 0) return 0;
        double got = 0;
        for (String species : wildlife.sourcesOf(r, resource)) {
            SpeciesDef def = wildlife.get(species).orElse(null);
            if (def == null) continue;
            double per = def.yields().getOrDefault(resource, 0.0D);
            if (per <= 0) continue;
            double animals = wildlife.take(r, species, (amount - got) / per, maxShare);
            got += animals * per;
            if (got >= amount - 1e-9) break;
        }
        if (got > 0) metrics.hunts.incrementAndGet();
        return got;
    }

    private void simulateWildlife(Region region, long to) {
        double before = region.danger();
        double animalDanger = wildlife.simulate(region, to, clock.minutesPerDay(), environment.seasonAnimals(), environment.foodSurplus(region.id()));
        region.baseDanger(Math.min(1.0D, catalog.dangerOf(region.type()) + animalDanger));
        if (Math.abs(region.danger() - before) >= 0.05D) {
            publish(new RegionDangerChangedEvent(to, region.id(), before, region.danger()));
            for (RoadEdge e : roads.crossing(region.id())) rateEdge(e);
            roads.touch();
        }
    }

    // ------------------------------------------------------------------ world events

    public Optional<WorldEventRecord> scheduleEvent(WorldEventType type, String title, UUID region, UUID settlement, double severity, long startIn, long duration, Provenance cause, Set<String> tags) {
        long now = clock.now();
        Optional<WorldEventRecord> e = worldEvents.schedule(type, title, region, settlement, severity, now, now + Math.max(0, startIn),
                duration > 0 ? duration : type.durationMinutes(), cause == null ? Provenance.of("world", "", "scheduled", now) : cause, tags);
        e.ifPresent(x -> metrics.eventsScheduled.incrementAndGet());
        return e;
    }

    public Optional<WorldEventRecord> scheduleEvent(WorldEventType type, String title, UUID region, UUID settlement, double severity, Provenance cause) {
        return scheduleEvent(type, title, region, settlement, severity, type.prepareMinutes(), type.durationMinutes(), cause, Set.of());
    }

    public boolean resolveEvent(UUID id, boolean success, String by, String outcome) { return worldEvents.resolve(id, success, by, outcome, clock.now()); }

    /** The world's own consequences of an event phase: danger on the land and roads, blocked roads, region memory, history. */
    private void onWorldEventPhase(WorldEventRecord e, WorldEventPhase phase, long now) {
        metrics.eventTransitions.incrementAndGet();
        publish(new WorldEventPhaseEvent(now, e.id(), e.type().name(), phase.name(), e.region(), e.settlement(), e.severity(), e.title()));
        Region region = e.region() == null ? null : regions.get(e.region());
        switch (phase) {
            case START -> {
                if (region != null) {
                    switch (e.type()) {
                        case FIRE -> region.count(Region.Counter.FIRES);
                        case WAR -> region.count(Region.Counter.WARS);
                        case ATTACK, BANDITS -> region.count(Region.Counter.BATTLES);
                        case FESTIVAL, CELEBRATION, MARKET -> region.count(Region.Counter.FESTIVALS);
                        case STORM, FLOOD, EMERGENCY -> region.count(Region.Counter.DISASTERS);
                        default -> { }
                    }
                }
                refreshEventEffects();
                double significance = switch (e.type()) { case WAR -> 0.95D; case FIRE, ATTACK, FLOOD -> 0.7D; case BANDITS, EMERGENCY -> 0.55D; case DUEL, STORM -> 0.35D; default -> 0.3D; };
                chronicle.record(now, categoryOf(e.type()), e.title(), "comienza", e.scopes(), significance, e.cause());
            }
            case END -> {
                refreshEventEffects();
            }
            case CONSEQUENCES -> {
                if (region != null && e.type() == WorldEventType.FIRE && e.settlement() == null) {
                    ResourceDeposit wood = region.deposits().get("wood");
                    if (wood != null) { wood.damage(0.3D * e.severity(), now, clock.minutesPerDay()); e.consequences().add("bosque quemado: madera -" + Math.round(30 * e.severity()) + "%"); region.markDirty(); }
                }
                if (e.resolution() == WorldEventRecord.Resolution.RESOLVED || e.resolution() == WorldEventRecord.Resolution.FAILED)
                    chronicle.record(now, categoryOf(e.type()), e.title() + (e.resolution() == WorldEventRecord.Resolution.RESOLVED ? " — resuelto" : " — fracaso"),
                            e.outcome() + (e.resolvedBy().isEmpty() ? "" : " (" + e.resolvedBy() + ")"), e.scopes(), e.type() == WorldEventType.WAR ? 0.9D : 0.5D, e.cause());
            }
            default -> { }
        }
    }

    private static String categoryOf(WorldEventType type) {
        return switch (type) {
            case FESTIVAL, CELEBRATION -> "FESTIVAL"; case RAIN, STORM -> "WEATHER"; case FLOOD, EMERGENCY -> "DISASTER"; case FIRE -> "FIRE";
            case ATTACK, DUEL -> "BATTLE"; case BANDITS -> "BATTLE"; case WAR -> "WAR"; case MARKET -> "ECONOMY";
        };
    }

    /** Recomputes event danger on regions and roads, and event blockages, from the events running now. */
    private void refreshEventEffects() {
        Map<UUID, Double> danger = new HashMap<>();
        Set<UUID> blockingRegions = new HashSet<>();
        for (WorldEventRecord e : worldEvents.running()) {
            if (e.region() == null) continue;
            danger.merge(e.region(), e.type().danger() * Math.max(0.25D, e.severity()), Double::sum);
            if (e.type().blocksRoads()) blockingRegions.add(e.region());
        }
        for (Region r : regions.values()) {
            double before = r.danger();
            double next = Math.min(1.0D, danger.getOrDefault(r.id(), 0.0D));
            if (Math.abs(next - r.eventDanger()) > 1e-9) {
                r.eventDanger(next);
                if (Math.abs(r.danger() - before) >= 0.05D) publish(new RegionDangerChangedEvent(clock.now(), r.id(), before, r.danger()));
            }
        }
        for (RoadEdge edge : roads.edges()) {
            rateEdge(edge);
            boolean shouldBlock = false;
            for (UUID r : edge.regions()) if (blockingRegions.contains(r)) shouldBlock = true;
            if (shouldBlock && !edge.blocked()) { if (blockRoad(edge.id(), "evento de mundo")) eventBlocked.add(edge.id()); }
            else if (!shouldBlock && edge.blocked() && eventBlocked.contains(edge.id())) reopenRoad(edge.id());
        }
        roads.touch();
    }

    /** Once a day: spontaneous events that come from the state of the world (fires in dry heat, raids where it is dangerous...). */
    public void daily(long dayIndex) {
        long started = System.nanoTime();
        Season season = environment.season();
        for (Settlement s : settlements.values()) {
            if (!s.active()) continue;
            Region r = regions.get(s.regionId());
            if (r == null) continue;
            WeatherKind weather = environment.weather(r.scope());
            double fire = settings.fireChancePerDay() * (season == Season.SUMMER && weather == WeatherKind.SUNNY ? 2.5D : weather.wet() ? 0.3D : 1.0D) * (s.type() == SettlementType.CAMP ? 1.5D : 1.0D);
            if (dice.chance("fire:" + s.id(), dayIndex, fire))
                scheduleEvent(WorldEventType.FIRE, "Incendio en " + s.name(), r.id(), s.id(), dice.between("fire-sev:" + s.id(), dayIndex, 0.2D, 0.9D), 0, 0,
                        Provenance.of("world", s.id().toString(), weather == WeatherKind.SUNNY ? "calor y sequedad" : "descuido", clock.now()), Set.of("spontaneous"));
            if (s.type().hasResidents() && dice.chance("attack:" + s.id(), dayIndex, settings.attackChancePerDay() * (0.2D + 3.0D * r.danger())))
                scheduleEvent(WorldEventType.ATTACK, "Ataque a " + s.name(), r.id(), s.id(), dice.between("attack-sev:" + s.id(), dayIndex, 0.3D, 1.0D), 12 * 60 + dice.below("attack-at:" + s.id(), dayIndex, 10 * 60), 0,
                        Provenance.of("world", r.id().toString(), String.format("peligro de la región %.2f", r.danger()), clock.now()), Set.of("spontaneous"));
            if (s.type() == SettlementType.VILLAGE && dice.chance("duel:" + s.id(), dayIndex, settings.duelChancePerDay()))
                scheduleEvent(WorldEventType.DUEL, "Duelo en " + s.name(), r.id(), s.id(), 0.5D, WorldEventType.DUEL.prepareMinutes(), 0, Provenance.of("world", s.id().toString(), "honor", clock.now()), Set.of("spontaneous"));
            if (s.type().hasMarket() && settings.specialMarketEveryDays() > 0 && Math.floorMod(dayIndex + (s.id().hashCode() & 0xffff), (long) settings.specialMarketEveryDays()) == 0)
                scheduleEvent(WorldEventType.MARKET, "Gran mercado de " + s.name(), r.id(), s.id(), 0.6D, 18 * 60, 0, Provenance.of("world", s.id().toString(), "mercado periódico", clock.now()), Set.of("periodic"));
        }
        for (Region r : regions.values()) {
            if (r.settlements().isEmpty() && roads.crossing(r.id()).isEmpty()) continue;
            if (!roads.crossing(r.id()).isEmpty() && dice.chance("bandits:" + r.id(), dayIndex, settings.banditChancePerDay() * (0.3D + 2.0D * r.baseDanger())))
                scheduleEvent(WorldEventType.BANDITS, "Bandidos en " + r.name(), r.id(), null, dice.between("bandit-sev:" + r.id(), dayIndex, 0.3D, 0.9D), 0, 0,
                        Provenance.of("world", r.id().toString(), "caminos poco vigilados", clock.now()), Set.of("spontaneous"));
            WeatherKind w = environment.weather(r.scope());
            if ((r.type() == RegionType.RIVER || r.type() == RegionType.COAST || r.type() == RegionType.SWAMP) && w == WeatherKind.STORM && dice.chance("flood:" + r.id(), dayIndex, settings.floodChanceInStorm()))
                scheduleEvent(WorldEventType.FLOOD, "Crecida en " + r.name(), r.id(), null, 0.6D, 0, 0, Provenance.of("world", r.id().toString(), "tormenta", clock.now()), Set.of("spontaneous"));
        }
        metrics.dailyNanos.addAndGet(System.nanoTime() - started);
    }

    // ------------------------------------------------------------------ the tick

    /** One server tick: streaming (on its interval), world events, and the regions whose simulation step is due. */
    public void tick(List<StreamingEngine.Viewer> currentViewers) {
        long started = System.nanoTime();
        refreshSettings();
        tickCount++;
        long now = clock.now();
        if (currentViewers != null) viewers = List.copyOf(currentViewers);
        if (tickCount % settings.streamingIntervalTicks() == 0) stream(now);
        worldEvents.tick(now);
        if (tickCount % settings.simulationIntervalTicks() == 0) simulation.tick(regions.values(), now, TickBudget.micros(settings.budgetMicros()));
        metrics.tick(System.nanoTime() - started);
    }

    /** Re-classifies regions by where the viewers are; a region that wakes is caught up before it is shown. */
    public void stream(long now) {
        metrics.streamingUpdates.incrementAndGet();
        for (StreamingEngine.Transition t : streaming.update(regions.values(), viewers, now)) {
            Region r = t.region();
            if (t.woke()) {
                metrics.activations.incrementAndGet();
                SimulationEngine.CatchUp c = simulation.catchUp(r, now);
                if (c.steps() > 0) { metrics.catchUps.incrementAndGet(); publish(new CatchUpCompletedEvent(now, r.id(), c.elapsed(), c.steps())); }
                publish(new RegionActivatedEvent(now, r.id(), r.name(), t.from().name(), t.to().name()));
            } else if (t.slept()) {
                metrics.sleeps.incrementAndGet();
                publish(new RegionSleepingEvent(now, r.id(), r.name(), t.from().name(), t.to().name()));
            }
        }
    }

    public void registerSimulator(RegionSimulator simulator) { simulation.register(simulator); }

    /** The villages report their population; region totals follow, and a change of the world total is announced. */
    public void reportPopulation(UUID settlement, int count, Map<String, Integer> professionCounts) {
        population.report(settlement, count, professionCounts);
        Settlement s = settlements.get(settlement);
        if (s != null) {
            Region r = regions.get(s.regionId());
            if (r != null) { int sum = 0; for (UUID id : r.settlements()) sum += population.of(id); r.population(sum); }
        }
        int total = population.total();
        if (total != reportedPopulation) { reportedPopulation = total; publish(new WorldPopulationChangedEvent(clock.now(), total, regions.size(), settlements.size())); }
    }

    // ------------------------------------------------------------------ views

    public WorldRuntime runtime(int activeNpcs, String calendarReference, String weatherState, String economyReference, int worldHistory) {
        Map<SimulationLevel, Integer> levels = new EnumMap<>(SimulationLevel.class);
        for (SimulationLevel l : SimulationLevel.values()) levels.put(l, 0);
        int abstractRegions = 0;
        for (Region r : regions.values()) { levels.merge(r.level(), 1, Integer::sum); if (r.level().ordinal() >= SimulationLevel.SETTLEMENT.ordinal()) abstractRegions++; }
        return new WorldRuntime(levels, worldEvents.open().size(), activeNpcs, abstractRegions, calendarReference, weatherState, economyReference, worldHistory,
                simulation.steps(), simulation.catchUps(), settlements.size(), roads.edgeCount(), population.total());
    }

    public WorldSettings settings() { return settings; }
    public WorldClock clock() { return clock; }
    public WorldMetrics metrics() { return metrics; }
    public RoadNetwork roads() { return roads; }
    public WorldEventEngine events() { return worldEvents; }
    public StreamingEngine streaming() { return streaming; }
    public SimulationEngine simulation() { return simulation; }
    public PopulationLedger population() { return population; }
    public EnvironmentInteractionEngine interactions() { return interactions; }
    public RegionCatalog catalog() { return catalog; }
    public WildlifeEngine wildlife() { return wildlife; }
    public ProfessionCatalog professions() { return professions; }
    public List<StreamingEngine.Viewer> viewers() { return viewers; }
    public long seed() { return seed; }
    public Set<UUID> eventBlockedRoads() { return Set.copyOf(eventBlocked); }
    public void restoreEventBlocked(Collection<UUID> ids) { eventBlocked.clear(); eventBlocked.addAll(ids); }

    public List<String> problems() {
        List<String> all = new ArrayList<>();
        all.addAll(catalog.problems()); all.addAll(wildlife.problems()); all.addAll(professions.problems());
        return all;
    }

    public boolean regionsDirty() { if (regionsDirty) return true; for (Region r : regions.values()) if (r.dirty()) return true; return false; }
    public void cleanRegions() { regionsDirty = false; regions.values().forEach(Region::clean); }
    public boolean settlementsDirty() { if (settlementsDirty) return true; for (Settlement s : settlements.values()) if (s.dirty()) return true; return false; }
    public void cleanSettlements() { settlementsDirty = false; settlements.values().forEach(Settlement::clean); }

    public void reset() {
        regions.clear(); regionByKey.clear(); settlements.clear(); roads.clear(); worldEvents.clear(); population.clear(); interactions.clear(); eventBlocked.clear();
        metrics.reset(); viewers = List.of(); tickCount = 0; reportedPopulation = -1; settlementsDirty = false; regionsDirty = false;
    }
}
