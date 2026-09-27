package yadi.samuraiai.ai.cognition.world;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import yadi.samuraiai.ai.cognition.engine.CognitionEngine;
import yadi.samuraiai.ai.cognition.engine.CognitionOutcome;
import yadi.samuraiai.ai.cognition.engine.CognitionSettings;
import yadi.samuraiai.ai.cognition.engine.CognitiveAdvice;
import yadi.samuraiai.ai.cognition.engine.ExperienceInput;
import yadi.samuraiai.ai.cognition.engine.WorldFacts;
import yadi.samuraiai.ai.cognition.model.EntityKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.cognition.personality.PersonalityLedger;
import yadi.samuraiai.ai.cognition.storage.CognitionStorage;
import yadi.samuraiai.ai.emotion.model.Activity;
import yadi.samuraiai.ai.emotion.model.EmotionEffect;
import yadi.samuraiai.ai.emotion.model.EmotionTrigger;
import yadi.samuraiai.ai.emotion.model.TriggerSource;
import yadi.samuraiai.ai.emotion.propagation.EmotionNeighbor;
import yadi.samuraiai.ai.knowledge.model.AccessLevel;
import yadi.samuraiai.ai.knowledge.society.Community;
import yadi.samuraiai.ai.knowledge.society.CommunityKind;
import yadi.samuraiai.ai.memory.retrieval.RetrievalContext;
import yadi.samuraiai.ai.perception.events.ThreatDetectedEvent;
import yadi.samuraiai.ai.perception.events.VisionDetectedEvent;
import yadi.samuraiai.ai.scheduler.events.RoutineCompletedEvent;
import yadi.samuraiai.ai.scheduler.events.WorldScheduleEvent;
import yadi.samuraiai.ai.scheduler.personality.PersonalityTraits;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.world.SchedulerService;
import yadi.samuraiai.ai.scheduler.zone.Zone;
import yadi.samuraiai.ai.scheduler.zone.ZoneKind;
import yadi.samuraiai.emotion.Emotion;
import yadi.samuraiai.emotion.EmotionService;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.event.EventSubscription;
import yadi.samuraiai.event.NPCEventBus;
import yadi.samuraiai.event.npc.NPCActivatedEvent;
import yadi.samuraiai.event.npc.NPCDeactivatedEvent;
import yadi.samuraiai.logging.SamuraiLogger;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.npc.relationship.Relationship;
import yadi.samuraiai.npc.relationship.RelationshipService;
import yadi.samuraiai.runtime.ServerScheduler;
import yadi.samuraiai.world.ServerWorlds;
import yadi.samuraiai.world.SpawnLocation;

/**
 * Minecraft-facing front of the cognitive layer. It gives the pure {@link CognitionEngine} its world (a persistent clock, where
 * NPCs are, who is who), turns what happens in the game into experiences (combat, death, explosions, conversations, what
 * perception sees and the scheduler does), keeps each NPC's mind updated at a cost that follows how near a player is, lets
 * information travel between neighbours, projects the result onto the older emotion and relationship services that the rest of
 * the mod already reads, feeds personality back to the scheduler and persists everything under the world's data folder.
 * Server-thread only.
 */
public final class CognitionService {
    private static final CognitionService INSTANCE = new CognitionService();
    public static CognitionService getInstance() { return INSTANCE; }

    private final CognitionEngine engine = new CognitionEngine(CognitionSettings::current, yadi.samuraiai.ai.memory.engine.MemorySettings::current, yadi.samuraiai.ai.relationship.engine.RelationshipSettings::current,
            yadi.samuraiai.ai.emotion.engine.EmotionSettings::current, yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings::current, EventSink.eventBus());
    private final List<EventSubscription> subscriptions = new ArrayList<>();
    private final Set<UUID> debugViewers = new HashSet<>();
    private final Map<UUID, Long> lastMind = new HashMap<>(), lastEcho = new HashMap<>(), lastExplore = new HashMap<>(), lastRitual = new HashMap<>(), lastAdvice = new HashMap<>(), lastThrottle = new HashMap<>();
    private final Map<String, Long> metAt = new HashMap<>(), zoneVisits = new HashMap<>(), attended = new HashMap<>(), hitAt = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> attackers = new HashMap<>();
    private final Map<UUID, CognitiveAdvice> advice = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final Map<UUID, double[]> pushedEvolution = new HashMap<>();
    private Map<UUID, NPCRuntime> npcs = Map.of();
    private List<ServerPlayer> players = List.of();
    private long tick, gameTime;
    private boolean installed;

    private CognitionService() { }

    public CognitionEngine engine() { return engine; }
    public boolean enabled() { return CognitionConfig.enabled() && installed && ServerScheduler.getInstance().isRunning(); }
    public long now() { return gameTime; }
    public java.util.Collection<NPCRuntime> living() { return npcs.values(); }
    public Set<UUID> debugViewers() { return debugViewers; }
    public boolean toggleDebug(UUID player) { return debugViewers.add(player) || !debugViewers.remove(player); }

    // ------------------------------------------------------------------ lifecycle

    public void install(MinecraftServer server) {
        if (installed) return;
        Path root = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("samuraiai").resolve("cognition");
        engine.useStorage(new CognitionStorage(root, CognitionSettings.current().compressStorage()));
        engine.useFacts(facts);
        engine.useTraitSource(this::traitsOf);
        var loaded = engine.storage().loadSociety(engine);
        SamuraiLogger.PERSISTENCE.info("Cognitive society: {} ({} communities)", loaded.status(), engine.society().communityCount());
        NPCEventBus bus = NPCEventBus.getInstance();
        subscriptions.add(bus.subscribe(NPCActivatedEvent.class, e -> NPCManager.getInstance().find(e.npcId()).ifPresent(this::activated)));
        subscriptions.add(bus.subscribe(NPCDeactivatedEvent.class, e -> removed(e.npcId(), e.reason())));
        subscriptions.add(bus.subscribe(VisionDetectedEvent.class, this::onVision));
        subscriptions.add(bus.subscribe(ThreatDetectedEvent.class, this::onThreat));
        subscriptions.add(bus.subscribe(RoutineCompletedEvent.class, this::onRoutine));
        subscriptions.add(bus.subscribe(WorldScheduleEvent.class, this::onWorldEvent));
        // What communities collectively know to be dangerous becomes a hazard navigation avoids; navigation still owns the route.
        yadi.samuraiai.ai.navigation.world.NavigationService.getInstance().registerDangerSource("cognition-knowledge", (dimension, navTick, out) -> {
            if (!enabled()) return;
            double threshold = CognitionSettings.current().dangerKnownThreshold();
            for (Community c : engine.society().communities())
                for (var r : yadi.samuraiai.ai.knowledge.places.PlaceKnowledge.dangerous(c.collective(), threshold)) {
                    if (!r.place().dimension().equals(dimension)) continue;
                    double danger = yadi.samuraiai.ai.knowledge.places.PlaceKnowledge.danger(r);
                    out.accept(new yadi.samuraiai.ai.navigation.zones.DangerZone(dimension, yadi.samuraiai.ai.navigation.graph.NavPos.ofBlock(r.place().x(), r.place().y(), r.place().z()), 8.0D,
                            danger * 30.0D, navTick + 60L, yadi.samuraiai.ai.navigation.zones.HazardType.CUSTOM, "knowledge:" + c.id()));
                }
        });
        installed = true;
    }

    /** Server stopping: everything is written, then the layer forgets the session. */
    public void shutdown() {
        if (installed) {
            try { engine.saveAll(gameTime); } catch (RuntimeException error) { SamuraiLogger.PERSISTENCE.error("Cognitive save failed", error); }
            subscriptions.forEach(EventSubscription::close);
        }
        subscriptions.clear();
        reset();
    }

    public void reset() {
        engine.reset();
        lastMind.clear(); lastEcho.clear(); lastExplore.clear(); lastRitual.clear(); lastAdvice.clear(); lastThrottle.clear(); metAt.clear(); zoneVisits.clear(); attended.clear(); hitAt.clear();
        attackers.clear(); advice.clear(); names.clear(); pushedEvolution.clear(); debugViewers.clear(); npcs = Map.of(); players = List.of(); tick = 0; installed = false;
    }

    private void activated(NPCRuntime runtime) {
        if (!enabled()) return;
        UUID id = runtime.getId();
        names.put(id, runtime.getName());
        engine.ensureLoaded(id, gameTime);
        ensureCommunity(runtime);
    }

    /** The NPC left the session: a permanent removal erases its mind; a server stop only saves it. */
    private void removed(UUID id, String reason) {
        if (!installed) return;
        if ("server stopped".equals(reason)) engine.unload(id, gameTime); else engine.erase(id);
        lastMind.remove(id); lastEcho.remove(id); lastExplore.remove(id); lastRitual.remove(id); lastAdvice.remove(id); advice.remove(id); attackers.remove(id); pushedEvolution.remove(id);
    }

    // ------------------------------------------------------------------ the world, as the engine sees it

    private final WorldFacts facts = new WorldFacts() {
        @Override public String weather() {
            return ServerWorlds.level("minecraft:overworld").map(l -> l.isThundering() ? "thunder" : l.isRaining() ? "rain" : "clear").orElse("");
        }

        @Override public PlaceRef placeOf(UUID npc) {
            NPCRuntime runtime = npcs.get(npc);
            if (runtime == null) runtime = NPCManager.getInstance().find(npc).orElse(null);
            if (runtime == null) return PlaceRef.unknown();
            SpawnLocation loc = runtime.getInstance().getLocation();
            return loc == null ? PlaceRef.unknown() : placeAt(loc.dimensionKey(), loc.x(), loc.y(), loc.z());
        }

        @Override public Set<String> goalTags(UUID npc) {
            Set<String> tags = new HashSet<>();
            SchedulerService.getInstance().adviceFor(npc).ifPresent(a -> { tags.add(a.label().toLowerCase(Locale.ROOT)); if (a.emergency()) tags.add("emergency"); });
            NPCManager.getInstance().find(npc).ifPresent(r -> { if (r.getCurrentGoal() != null) tags.add(r.getCurrentGoal().getType().name().toLowerCase(Locale.ROOT)); });
            return tags;
        }

        @Override public EntityRef refOf(UUID id) { return refFor(id); }
    };

    private PlaceRef placeAt(String dimension, double x, double y, double z) {
        String zone = "";
        var zones = SchedulerService.getInstance().scheduler().zoneRegistry().containing(dimension, x, z);
        if (!zones.isEmpty()) zone = zones.get(0).id();
        return new PlaceRef(dimension, x, y, z, zone);
    }

    private EntityRef refFor(UUID id) {
        NPCRuntime npc = npcs.get(id);
        if (npc != null) return EntityRef.npc(id, npc.getName());
        Optional<ServerPlayer> player = ServerWorlds.playerById(id);
        if (player.isPresent()) return EntityRef.player(id, player.get().getGameProfile().getName());
        String name = names.get(id);
        return new EntityRef(id, name == null ? EntityKind.UNKNOWN : EntityKind.CREATURE, name == null ? "" : name);
    }

    EntityRef refOfEntity(Entity entity) {
        if (entity == null) return null;
        if (entity instanceof Player player) return EntityRef.player(player.getUUID(), player.getGameProfile().getName());
        for (NPCRuntime runtime : NPCManager.getInstance().getActive())
            if (runtime.getController().ownsEntity(runtime.getInstance(), entity.getUUID())) return EntityRef.npc(runtime.getId(), runtime.getName());
        String name = entity.getType().getDescription().getString();
        names.put(entity.getUUID(), name);
        return new EntityRef(entity.getUUID(), EntityKind.CREATURE, name);
    }

    private NPCRuntime npcOwning(UUID entity) {
        for (NPCRuntime runtime : NPCManager.getInstance().getActive()) if (runtime.isActive() && runtime.getController().ownsEntity(runtime.getInstance(), entity)) return runtime;
        return null;
    }

    private double[] traitsOf(UUID id) {
        var schedule = SchedulerService.getInstance().scheduler().schedule(id);
        if (schedule.isEmpty()) return null;
        double[] values = new double[Trait.values().length];
        java.util.Arrays.fill(values, 50.0D);
        for (Trait t : Trait.values()) {
            var scheduled = yadi.samuraiai.ai.scheduler.personality.Trait.parse(t.name());
            if (scheduled.isPresent()) values[t.ordinal()] = schedule.get().traits().get(scheduled.get());
        }
        return values;
    }

    // ------------------------------------------------------------------ the tick

    public void tick() {
        if (!enabled()) return;
        Optional<ServerLevel> overworld = ServerWorlds.level("minecraft:overworld");
        if (overworld.isEmpty()) return;
        tick++;
        gameTime = overworld.get().getGameTime();
        Map<UUID, NPCRuntime> living = new HashMap<>();
        for (NPCRuntime runtime : NPCManager.getInstance().getActive()) if (runtime.isActive()) living.put(runtime.getId(), runtime);
        npcs = living;
        players = ServerWorlds.onlinePlayers();
        CognitionSettings cs = CognitionSettings.current();
        try {
            if (tick % 20 == 0) engine.society(gameTime);
            if (tick % cs.gossipScanTicks() == 0) socialScan(cs);
            updateMinds(cs);
            engine.saveDirty(gameTime, false);
            if (tick % 6000 == 0) { attackers.values().forEach(m -> m.values().removeIf(t -> gameTime - t > 1200)); hitAt.values().removeIf(t -> gameTime - t > 1200); metAt.values().removeIf(t -> gameTime - t > 72000); }
        } catch (RuntimeException error) {
            engine.metrics().errors.incrementAndGet();
            engine.metrics().lastError = error.toString();
            SamuraiLogger.BRAIN.warn("Cognition tick failed: {}", error.toString());
        }
        if (!debugViewers.isEmpty() && tick % 10 == 0) CognitionDebugRenderer.render(this, debugViewers);
    }

    private record Actor(UUID id, String dimension, double x, double y, double z, boolean player) { }

    private List<Actor> actors() {
        List<Actor> list = new ArrayList<>();
        for (NPCRuntime r : npcs.values()) { SpawnLocation l = r.getInstance().getLocation(); if (l != null) list.add(new Actor(r.getId(), l.dimensionKey(), l.x(), l.y(), l.z(), false)); }
        for (ServerPlayer p : players) list.add(new Actor(p.getUUID(), p.level.dimension().location().toString(), p.getX(), p.getY(), p.getZ(), true));
        return list;
    }

    private double nearestPlayer(SpawnLocation l) {
        double best = Double.MAX_VALUE;
        for (ServerPlayer p : players) {
            if (!p.level.dimension().location().toString().equals(l.dimensionKey())) continue;
            best = Math.min(best, Math.sqrt(p.distanceToSqr(l.x(), l.y(), l.z())));
        }
        return best;
    }

    private int intervalFor(double distance, CognitionSettings cs) {
        if (distance <= cs.nearDistance()) return cs.nearInterval();
        if (distance <= cs.midDistance()) return cs.midInterval();
        if (distance <= cs.farDistance()) return cs.farInterval();
        return cs.hibernateInterval();
    }

    private void updateMinds(CognitionSettings cs) {
        record Due(NPCRuntime runtime, long overdue, double distance) { }
        List<Due> due = new ArrayList<>();
        for (NPCRuntime runtime : npcs.values()) {
            SpawnLocation l = runtime.getInstance().getLocation();
            if (l == null || !engine.isLoaded(runtime.getId())) continue;
            double distance = nearestPlayer(l);
            int interval = intervalFor(distance, cs);
            Long last = lastMind.get(runtime.getId());
            if (last == null) { lastMind.put(runtime.getId(), gameTime - interval + Math.floorMod(runtime.getId().hashCode(), interval)); continue; }
            long overdue = gameTime - last - interval;
            if (overdue >= 0) due.add(new Due(runtime, overdue, distance));
        }
        if (due.isEmpty()) return;
        due.sort((a, b) -> Long.compare(b.overdue(), a.overdue()));
        List<Actor> actors = null;
        long started = System.nanoTime();
        int done = 0;
        for (Due d : due) {
            if (done >= cs.maxMindsPerTick() || (System.nanoTime() - started) / 1000 > cs.budgetMicros()) break;
            if (actors == null) actors = actors();
            mind(d.runtime(), d.distance(), actors, cs);
            done++;
        }
    }

    private void mind(NPCRuntime runtime, double distance, List<Actor> actors, CognitionSettings cs) {
        UUID id = runtime.getId();
        lastMind.put(id, gameTime);
        try {
            var result = engine.tick(id, gameTime, activityOf(id));
            projectEmotions(runtime);
            SpawnLocation loc = runtime.getInstance().getLocation();
            if (loc == null) return;
            PlaceRef place = placeAt(loc.dimensionKey(), loc.x(), loc.y(), loc.z());
            List<UUID> nearby = nearby(id, loc, actors, cs.familiarRadius());
            if (distance <= cs.midDistance() && gameTime - lastEcho.getOrDefault(id, 0L) >= cs.echoCheckTicks()) {
                lastEcho.put(id, gameTime);
                if (engine.context(id, new RetrievalContext(place, nearby, engine.emotions().blend(id).dominant(), Set.of()), gameTime) > 0) projectEmotions(runtime);
            }
            if (distance <= cs.midDistance() && gameTime - lastAdvice.getOrDefault(id, 0L) >= cs.contextRefreshTicks()) {
                lastAdvice.put(id, gameTime);
                String zoneKind = zoneKindOf(place);
                var period = SchedulerService.getInstance().scheduler().currentPeriod();
                advice.put(id, engine.advice(id, new RetrievalContext(place, nearby, engine.emotions().blend(id).dominant(), engine.facts().goalTags(id)), gameTime, period == null ? "" : period.name(), zoneKind, 48.0D));
            }
            if (gameTime - lastRitual.getOrDefault(id, 0L) >= cs.ritualCheckTicks()) { lastRitual.put(id, gameTime); ritual(runtime, place); }
            if (distance <= cs.midDistance() && gameTime - lastExplore.getOrDefault(id, 0L) >= cs.exploreScanTicks()) { lastExplore.put(id, gameTime); explore(runtime, place); }
            syncPersonality(id);
            if (result.previousMood() != null) SchedulerService.getInstance().scheduler().disturb(id);
        } catch (RuntimeException error) {
            engine.metrics().errors.incrementAndGet();
            engine.metrics().lastError = error.toString();
            SamuraiLogger.BRAIN.warn("Mind of {} failed: {}", runtime.getName(), error.toString());
        }
    }

    private List<UUID> nearby(UUID self, SpawnLocation loc, List<Actor> actors, double radius) {
        List<UUID> result = new ArrayList<>();
        double r2 = radius * radius;
        for (Actor a : actors) {
            if (a.id().equals(self) || !a.dimension().equals(loc.dimensionKey())) continue;
            double dx = a.x() - loc.x(), dy = a.y() - loc.y(), dz = a.z() - loc.z();
            if (dx * dx + dy * dy + dz * dz <= r2) result.add(a.id());
        }
        return result;
    }

    private Activity activityOf(UUID id) {
        var a = SchedulerService.getInstance().adviceFor(id);
        if (a.isEmpty()) return Activity.NONE;
        if (a.get().response() != null) return a.get().response().name().contains("FIGHT") ? Activity.FIGHTING : Activity.NONE;
        RoutineType routine = a.get().routine();
        if (routine == null) return Activity.NONE;
        return switch (routine) {
            case SLEEP -> Activity.SLEEPING; case MEDITATE, PRAYER -> Activity.MEDITATING; case SOCIAL -> Activity.TALKING; case REST, EAT, WAKE -> Activity.RESTING;
            default -> Activity.WORKING;
        };
    }

    private String zoneKindOf(PlaceRef place) {
        if (place.zone().isEmpty()) return "ANY";
        return SchedulerService.getInstance().scheduler().zoneRegistry().get(place.zone()).map(z -> z.kind().name()).orElse("ANY");
    }

    // ------------------------------------------------------------------ what the NPC does with its day

    private void ensureCommunity(NPCRuntime runtime) {
        UUID id = runtime.getId();
        if (!engine.society().communitiesOf(id).isEmpty()) return;
        SpawnLocation home = runtime.getInstance().getHome() != null ? runtime.getInstance().getHome() : runtime.getInstance().getLocation();
        if (home == null) return;
        PlaceRef place = new PlaceRef(home.dimensionKey(), home.x(), home.y(), home.z(), "");
        var settings = yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings.current();
        Optional<Community> found = engine.society().nearestCommunity(place, settings.memberJoinRadius());
        if (found.isPresent()) { engine.society().join(id, found.get().id(), AccessLevel.MEMBERS); return; }
        if (!settings.autoVillage()) return;
        String key = "village-" + Math.floorDiv((int) home.x(), 128) + "_" + Math.floorDiv((int) home.z(), 128);
        Community village = engine.society().create(key, "Aldea " + key.substring(8), CommunityKind.VILLAGE, "village", place, settings.memberJoinRadius());
        engine.society().join(id, village.id(), village.members().isEmpty() ? AccessLevel.LEADERS : AccessLevel.MEMBERS);
    }

    private void ritual(NPCRuntime runtime, PlaceRef place) {
        UUID id = runtime.getId();
        var period = SchedulerService.getInstance().scheduler().currentPeriod();
        if (period == null) return;
        String zoneKind = zoneKindOf(place);
        var due = engine.society().dueTradition(id, period.name(), zoneKind);
        if (due.isEmpty() || place.zone().isEmpty()) return;
        String key = id + ":" + due.get().id() + ":" + (gameTime / 24000L);
        if (attended.putIfAbsent(key, gameTime) != null) return;
        experience(id, ExperienceInput.of(id, ExperienceKind.ATTENDED_RITUAL, gameTime).place(place).context("placeName", place.zone()).tag("tradition").note(due.get().name()));
        engine.society().observeTradition(id, due.get(), gameTime);
    }

    private void explore(NPCRuntime runtime, PlaceRef place) {
        UUID id = runtime.getId();
        if (!place.zone().isEmpty()) {
            String key = id + ":" + place.zone();
            Long last = zoneVisits.get(key);
            if (last != null && gameTime - last < 6000L) return;
            zoneVisits.put(key, gameTime);
            Optional<Zone> zone = SchedulerService.getInstance().scheduler().zoneRegistry().get(place.zone());
            String landmark = zone.map(z -> switch (z.kind()) { case TEMPLE -> "TEMPLE"; case MARKET -> "MARKET"; case HOME -> "HOME"; case GUARD_POST -> "GUARD_POST"; case TRAINING -> "TRAINING"; default -> "OTHER"; }).orElse("OTHER");
            String category = zone.map(z -> switch (z.kind()) { case TEMPLE -> "TEMPLE"; case MARKET -> "MARKET"; case HOME -> "HOUSE"; case GUARD_POST -> "CASTLE"; default -> "GENERAL"; }).orElse("GENERAL");
            boolean known = !engine.memory().runtime(id).spatial().byKind(yadi.samuraiai.ai.memory.spatial.LandmarkKind.parse(landmark)).stream().filter(n -> n.place().zone().equals(place.zone())).toList().isEmpty();
            experience(id, ExperienceInput.of(id, known ? ExperienceKind.VISITED_PLACE : ExperienceKind.DISCOVERED_PLACE, gameTime).place(place).context("placeName", place.zone())
                    .context("placeKey", place.zone()).context("landmark", landmark).context("category", category));
            return;
        }
        var spatial = engine.memory().runtime(id).spatial();
        if (spatial.nearest(place, 28.0D, null).isPresent()) return;
        Optional<ServerLevel> level = ServerWorlds.level(place.dimension());
        if (level.isEmpty()) return;
        PlaceClassifier.classify(level.get(), new BlockPos(place.x(), place.y(), place.z())).ifPresent(found ->
                experience(id, ExperienceInput.of(id, ExperienceKind.DISCOVERED_PLACE, gameTime).place(place).context("placeName", found.label() + " " + (int) place.x() + "," + (int) place.z())
                        .context("category", found.category()).context("landmark", "LANDMARK")));
    }

    // ------------------------------------------------------------------ neighbours: gossip and contagion

    private void socialScan(CognitionSettings cs) {
        var ks = yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings.current();
        List<NPCRuntime> list = new ArrayList<>(npcs.values());
        int pairs = 0;
        for (NPCRuntime a : list) {
            if (pairs >= 24) break;
            if (!engine.isLoaded(a.getId())) continue;
            SpawnLocation la = a.getInstance().getLocation();
            if (la == null) continue;
            List<EmotionNeighbor> neighbours = new ArrayList<>();
            for (NPCRuntime b : list) {
                if (a == b || !engine.isLoaded(b.getId())) continue;
                SpawnLocation lb = b.getInstance().getLocation();
                if (lb == null || !lb.dimensionKey().equals(la.dimensionKey())) continue;
                double d = Math.sqrt(la.distanceSquaredTo(lb.x(), lb.y(), lb.z()));
                if (d > ks.gossipRadius() && d > yadi.samuraiai.ai.emotion.engine.EmotionSettings.current().contagionRadius()) continue;
                if (d <= ks.gossipRadius() && pairs < 24 && engine.society().gossip(a.getId(), b.getId(), d, engine.sourceView(), gameTime) > 0) pairs++;
                double trust = engine.relationships().find(b.getId(), a.getId()).map(r -> r.trust()).orElse(engine.relationships().settings().initialTrust());
                neighbours.add(new EmotionNeighbor(b.getId(), d, trust, engine.personality(b.getId()).lean(Trait.SOCIABILITY)));
            }
            if (neighbours.isEmpty()) continue;
            for (var transfer : engine.emotions().contagion(a.getId(), neighbours, gameTime)) {
                NPCRuntime target = npcs.get(transfer.target());
                if (target == null) continue;
                engine.emotions().trigger(new EmotionTrigger(transfer.target(), TriggerSource.CONTAGION, List.of(new EmotionEffect(transfer.kind(), transfer.intensity())), null, "contagion:" + a.getId(), null, gameTime,
                        facts.placeOf(transfer.target()), EntityRef.npc(a.getId(), a.getName()), false, 0.5D, false, "", "caught from " + a.getName()));
                projectEmotions(target);
            }
        }
    }

    // ------------------------------------------------------------------ experiences from the world

    /** Runs one experience and applies its consequences to the legacy services and the scheduler. */
    public CognitionOutcome experience(UUID npc, ExperienceInput input) {
        if (!enabled()) return CognitionOutcome.rejected(UUID.randomUUID(), UUID.randomUUID());
        engine.ensureLoaded(npc, gameTime);
        CognitionOutcome outcome = engine.experience(input);
        if (outcome.kept()) {
            NPCRuntime runtime = npcs.get(npc) != null ? npcs.get(npc) : NPCManager.getInstance().find(npc).orElse(null);
            if (runtime != null && runtime.isActive()) {
                projectEmotions(runtime);
                if (outcome.relationship() != null) projectRelationship(npc, outcome.relationship().record().target().id());
                if (outcome.emotion() != null && outcome.emotion().any()) SchedulerService.getInstance().scheduler().disturb(npc);
                syncPersonality(npc);
            }
        }
        return outcome;
    }

    /** A player spoke with an NPC and got an answer. */
    public void conversation(NPCRuntime runtime, UUID playerId, String playerName) {
        if (!enabled() || playerId == null) return;
        names.put(playerId, playerName);
        experience(runtime.getId(), ExperienceInput.of(runtime.getId(), ExperienceKind.CONVERSATION, gameTime).actor(EntityRef.player(playerId, playerName)).context("placeName", "conversación").source("dialogue"));
    }

    private boolean throttled(String key, long ticks) {
        Long last = hitAt.get(key);
        if (last != null && gameTime - last < ticks) return true;
        hitAt.put(key, gameTime);
        return false;
    }

    void onHurt(Entity victim, Entity attacker, float amount) {
        if (!enabled() || amount <= 0.0F) return;
        NPCRuntime npc = npcOwning(victim.getUUID());
        if (npc == null) return;
        EntityRef attackerRef = refOfEntity(attacker);
        PlaceRef place = facts.placeOf(npc.getId());
        if (attackerRef != null) attackers.computeIfAbsent(npc.getId(), k -> new HashMap<>()).put(attackerRef.id(), gameTime);
        if (attackerRef != null && !throttled("hit:" + npc.getId() + ":" + attackerRef.id(), 60)) {
            experience(npc.getId(), ExperienceInput.of(npc.getId(), ExperienceKind.ATTACKED_ME, gameTime).actor(attackerRef).place(place).scale(Math.max(0.6D, Math.min(1.5D, amount / 6.0D))).source("combat"));
        }
        if (attackerRef != null && attackerRef.kind() != EntityKind.CREATURE)
            witnesses(npc, ExperienceKind.WITNESSED_ATTACK, attackerRef, refFor(npc.getId()), false);
    }

    void onDeath(Entity victim, Entity killer) {
        if (!enabled()) return;
        EntityRef killerRef = refOfEntity(killer);
        NPCRuntime dead = npcOwning(victim.getUUID());
        if (dead != null) { witnesses(dead, ExperienceKind.WITNESSED_DEATH, killerRef, EntityRef.npc(dead.getId(), dead.getName()), true); return; }
        if (killerRef != null && killerRef.kind() == EntityKind.PLAYER) {
            // A player killed something that was attacking an NPC: a rescue.
            for (var entry : new ArrayList<>(attackers.entrySet())) {
                Long at = entry.getValue().get(victim.getUUID());
                if (at == null || gameTime - at > 400L) continue;
                NPCRuntime rescued = npcs.get(entry.getKey());
                if (rescued == null) continue;
                experience(rescued.getId(), ExperienceInput.of(rescued.getId(), ExperienceKind.HELPED_ME, gameTime).actor(killerRef).place(facts.placeOf(rescued.getId())).context("placeName", "rescate").source("combat"));
                entry.getValue().remove(victim.getUUID());
            }
        } else if (killerRef != null && killerRef.kind() == EntityKind.NPC) {
            experience(killerRef.id(), ExperienceInput.of(killerRef.id(), ExperienceKind.WON_BATTLE, gameTime).target(refOfEntity(victim)).place(facts.placeOf(killerRef.id())).source("combat"));
        }
    }

    void onExplosion(ServerLevel level, double x, double y, double z) {
        if (!enabled()) return;
        String dimension = level.dimension().location().toString();
        for (NPCRuntime npc : npcs.values()) {
            SpawnLocation l = npc.getInstance().getLocation();
            if (l == null || !l.dimensionKey().equals(dimension) || l.distanceSquaredTo(x, y, z) > 32.0D * 32.0D) continue;
            if (throttled("boom:" + npc.getId(), 200)) continue;
            experience(npc.getId(), ExperienceInput.of(npc.getId(), ExperienceKind.WITNESSED_EXPLOSION, gameTime).place(new PlaceRef(dimension, x, y, z, placeAt(dimension, x, y, z).zone())).context("placeName", "explosión").source("world"));
        }
    }

    /** Neighbours within the witness radius see what happened (a cap keeps a brawl in a crowd cheap). */
    private void witnesses(NPCRuntime subject, ExperienceKind kind, EntityRef actor, EntityRef target, boolean deathAlly) {
        SpawnLocation at = subject.getInstance().getLocation();
        if (at == null) return;
        double radius = CognitionSettings.current().witnessRadius();
        int seen = 0;
        for (NPCRuntime observer : npcs.values()) {
            if (observer == subject || seen >= 6) continue;
            SpawnLocation l = observer.getInstance().getLocation();
            if (l == null || !l.dimensionKey().equals(at.dimensionKey()) || l.distanceSquaredTo(at.x(), at.y(), at.z()) > radius * radius) continue;
            if (kind == ExperienceKind.WITNESSED_ATTACK && throttled("wit:" + observer.getId() + ":" + (actor == null ? "" : actor.id()), 100)) continue;
            boolean ally = deathAlly && engine.relationships().find(observer.getId(), subject.getId()).map(r -> r.trust() >= 55 || r.affinity() >= 60).orElse(false);
            ExperienceInput input = ExperienceInput.of(observer.getId(), kind, gameTime).actor(actor).target(target).place(facts.placeOf(observer.getId())).witnessed(true).publicEvent(true).source("witness");
            if (deathAlly) input.traumatic(ally).context("placeName", "muerte de " + subject.getName());
            experience(observer.getId(), input);
            seen++;
        }
    }

    private void onVision(VisionDetectedEvent e) {
        if (!enabled() || e.targetId() == null) return;
        EntityRef target = refFor(e.targetId());
        if (target.kind() != EntityKind.PLAYER && target.kind() != EntityKind.NPC) return;
        String key = e.npcId() + ":" + e.targetId();
        Long last = metAt.get(key);
        if (last != null && gameTime - last < 72000L) return;
        metAt.put(key, gameTime);
        experience(e.npcId(), ExperienceInput.of(e.npcId(), ExperienceKind.MET_PERSON, gameTime).actor(target).place(facts.placeOf(e.npcId())).source("perception"));
    }

    private void onThreat(ThreatDetectedEvent e) {
        if (!enabled() || !e.level().atLeast(yadi.samuraiai.ai.perception.awareness.ThreatLevel.DANGER)) return;
        if (throttled("threat:" + e.npcId(), 200)) return;
        PlaceRef place = new PlaceRef(facts.placeOf(e.npcId()).dimension(), e.x(), e.y(), e.z(), "");
        experience(e.npcId(), ExperienceInput.of(e.npcId(), ExperienceKind.THREAT_SEEN, gameTime).place(place).context("placeName", "peligro (" + e.label() + ")").scale(Math.min(1.5D, e.score() / 60.0D)).source("perception"));
    }

    private void onRoutine(RoutineCompletedEvent e) {
        if (!enabled() || e.performedTicks() < 200) return;
        ExperienceKind kind = switch (e.routine()) {
            case "PATROL", "GUARD" -> ExperienceKind.PATROLLED; case "MEDITATE", "PRAYER" -> ExperienceKind.MEDITATED; case "SLEEP" -> ExperienceKind.SLEPT; case "TRAINING" -> ExperienceKind.TRAINED_TOGETHER;
            default -> null;
        };
        if (kind == null) return;
        experience(e.npcId(), ExperienceInput.of(e.npcId(), kind, gameTime).duration(e.performedTicks()).place(facts.placeOf(e.npcId())).source("scheduler"));
        if (kind == ExperienceKind.SLEPT) engine.sleep(e.npcId(), gameTime);
    }

    private void onWorldEvent(WorldScheduleEvent e) {
        if (!enabled() || !e.started()) return;
        String name = e.name().toLowerCase(Locale.ROOT);
        if (!name.contains("festival") && !name.contains("fiesta")) return;
        int count = 0;
        for (NPCRuntime npc : npcs.values()) {
            if (count++ >= 40) break;
            experience(npc.getId(), ExperienceInput.of(npc.getId(), ExperienceKind.CELEBRATED, gameTime).place(facts.placeOf(npc.getId())).context("placeName", e.name()).source("scheduler"));
        }
    }

    // ------------------------------------------------------------------ projection into the rest of the mod

    /** The cognitive emotions become the values the older emotion state, the scheduler and the prompts read. Only raises: the legacy state decays by itself. */
    void projectEmotions(NPCRuntime runtime) {
        if (!CognitionSettings.current().projectEmotions() || !runtime.isActive()) return;
        var em = engine.emotions();
        UUID id = runtime.getId();
        double fear = em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.FEAR), anger = em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.ANGER);
        double sadness = Math.max(em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.SADNESS), 0.8D * em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.LONELINESS));
        double joy = Math.max(Math.max(em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.JOY), em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.GRATITUDE)), 0.8D * em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.PRIDE));
        double anxiety = Math.max(em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.ANXIETY), 0.5D * em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.DISTRUST));
        double shame = Math.max(em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.SHAME), em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.GUILT));
        double trust = Math.max(em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.HOPE), Math.max(em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.RESPECT), 0.6D * em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.GRATITUDE)));
        double calm = Math.max(em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.CALM), 70.0D - 0.5D * (fear + anger + sadness + anxiety));
        raise(runtime, Emotion.FEAR, fear); raise(runtime, Emotion.ANGER, anger); raise(runtime, Emotion.SADNESS, sadness); raise(runtime, Emotion.HAPPINESS, joy);
        raise(runtime, Emotion.ANXIETY, anxiety); raise(runtime, Emotion.SHAME, shame); raise(runtime, Emotion.TRUST, trust); raise(runtime, Emotion.CALM, calm);
        raise(runtime, Emotion.SURPRISE, em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.SURPRISE));
        raise(runtime, Emotion.FRUSTRATION, Math.max(0.5D * anger, em.intensity(id, yadi.samuraiai.ai.cognition.model.EmotionKind.EMOTIONAL_FATIGUE)));
    }

    private static void raise(NPCRuntime runtime, Emotion emotion, double target) {
        int wanted = (int) Math.round(Math.max(0.0D, Math.min(100.0D, target)));
        int current = runtime.getEmotionState().get(emotion);
        if (wanted > current) EmotionService.getInstance().adjust(runtime, emotion, wanted - current);
    }

    /** What the cognitive relationship says, on the older -100..100 scale the dialogue prompts read. */
    void projectRelationship(UUID npc, UUID target) {
        if (!CognitionSettings.current().projectRelationships()) return;
        var found = engine.relationships().find(npc, target);
        if (found.isEmpty()) return;
        var r = found.get();
        Relationship legacy = RelationshipService.getInstance().find(npc, target).orElse(new Relationship());
        int trust = clamp((r.trust() - 50.0D) * 2.0D), respect = clamp((r.respect() - 50.0D) * 2.0D), hostility = clamp(Math.max(r.fear(), r.rivalry())),
                gratitude = clamp(r.affinity() > 40.0D ? (r.affinity() - 40.0D) * 1.5D : 0.0D), loyalty = clamp(r.loyalty());
        int dt = trust - legacy.getTrust(), dr = respect - legacy.getRespect(), dh = hostility - legacy.getHostility(), dg = gratitude - legacy.getGratitude(), dl = loyalty - legacy.getLoyalty();
        if (dt != 0 || dr != 0 || dh != 0 || dg != 0 || dl != 0) RelationshipService.getInstance().adjust(npc, target, dt, dr, dh, dg, dl);
    }

    private static int clamp(double v) { return (int) Math.round(Math.max(-100.0D, Math.min(100.0D, v))); }

    /** Long-term personality changes (never the base) are added to the scheduler's traits as they happen and, after a restart, restored once. */
    void syncPersonality(UUID id) {
        PersonalityLedger ledger = engine.ledgers().get(id);
        if (ledger == null || !ledger.seeded()) return;
        var schedule = SchedulerService.getInstance().scheduler().schedule(id);
        if (schedule.isEmpty()) return;
        double[] now = ledger.evolutionValues();
        double[] pushed = pushedEvolution.computeIfAbsent(id, k -> new double[now.length]);
        PersonalityTraits traits = schedule.get().traits();
        boolean changed = false;
        for (Trait t : Trait.values()) {
            double delta = now[t.ordinal()] - pushed[t.ordinal()];
            if (Math.abs(delta) < 1e-6) continue;
            var scheduled = yadi.samuraiai.ai.scheduler.personality.Trait.parse(t.name());
            if (scheduled.isPresent()) { traits = traits.with(scheduled.get(), traits.get(scheduled.get()) + delta); changed = true; }
            pushed[t.ordinal()] = now[t.ordinal()];
        }
        if (changed) schedule.get().replaceTraits(traits);
    }

    // ------------------------------------------------------------------ what the Brain reads

    /** The latest advice for an NPC (refreshed at a distance-dependent interval), if the layer is on and has looked at it. */
    public Optional<CognitiveAdvice> adviceFor(UUID npcId) { return enabled() ? Optional.ofNullable(advice.get(npcId)) : Optional.empty(); }

    public Optional<NPCRuntime> npc(UUID id) { return Optional.ofNullable(npcs.get(id)); }
    public Map<UUID, NPCRuntime> npcs() { return npcs; }
    public EntityRef ref(UUID id) { return refFor(id); }

    /** Applies the emotion projection immediately (commands and tests use it after triggering feelings by hand). */
    public void project(NPCRuntime runtime) { projectEmotions(runtime); }
}
