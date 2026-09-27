package yadi.samuraiai.living.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.cognition.world.CognitionService;
import yadi.samuraiai.ai.perception.events.ThreatDetectedEvent;
import yadi.samuraiai.ai.scheduler.events.RoutineCompletedEvent;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.world.SchedulerService;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.event.EventSubscription;
import yadi.samuraiai.event.NPCEventBus;
import yadi.samuraiai.event.npc.NPCActivatedEvent;
import yadi.samuraiai.event.npc.NPCDeactivatedEvent;
import yadi.samuraiai.living.calendar.engine.CalendarEngine;
import yadi.samuraiai.living.calendar.engine.CalendarRuntime;
import yadi.samuraiai.living.calendar.events.FestivalStartedEvent;
import yadi.samuraiai.living.calendar.events.WeatherChangedEvent;
import yadi.samuraiai.living.core.CalendarDate;
import yadi.samuraiai.living.core.WeatherKind;
import yadi.samuraiai.living.family.model.Person;
import yadi.samuraiai.living.quest.events.QuestCreatedEvent;
import yadi.samuraiai.living.quest.runtime.Quest;
import yadi.samuraiai.living.sim.LivingSettings;
import yadi.samuraiai.living.sim.LivingWorld;
import yadi.samuraiai.living.village.citizens.Citizen;
import yadi.samuraiai.living.village.events.VillageEventStartedEvent;
import yadi.samuraiai.living.village.events.VillageSecurityChangedEvent;
import yadi.samuraiai.living.village.homes.HomeRecord;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.village.security.SecurityState;
import yadi.samuraiai.living.world.events_api.WorldEventPhaseEvent;
import yadi.samuraiai.living.world.streaming.StreamingEngine;
import yadi.samuraiai.logging.SamuraiLogger;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.runtime.ServerScheduler;
import yadi.samuraiai.world.ServerWorlds;
import yadi.samuraiai.world.SpawnLocation;

/**
 * Minecraft-facing front of the living world (Phase 5). It builds the {@link LivingWorld} for the server's world (seeded by
 * the world seed, stored under {@code data/samuraiai/living}), gives it the real map (biomes) and the rest of SamuraiAI
 * (through {@link CognitionOutside}), and then translates both ways: NPCs appearing, working, dying and leaving become
 * citizens, work hours and deaths; players walking, meditating, talking, killing and delivering become quest progress; the
 * village timetable becomes a routine bias the Behavior Scheduler weighs; homes become the NPC's bed; Deiliora's weather can
 * drive the vanilla sky; the Deiliora date and the NPC's village, family and problems enter the dialogue prompt. Every tick
 * the world advances within its own budgets. Nothing here decides for an engine. Server-thread only.
 */
public final class LivingService {
    private static final LivingService INSTANCE = new LivingService();
    public static LivingService getInstance() { return INSTANCE; }

    private record Stillness(String dimension, double x, double y, double z, long since) { }

    private LivingWorld world;
    private ResourceItems items = new ResourceItems(List.of());
    private List<String> itemSource = List.of();
    private final List<EventSubscription> subscriptions = new ArrayList<>();
    private final Set<UUID> debugViewers = new HashSet<>(), hudViewers = new HashSet<>();
    private final Map<UUID, Stillness> stillness = new HashMap<>();
    private final Map<UUID, Long> lastThreat = new HashMap<>(), toldOffers = new HashMap<>();
    /** NPCs whose home the living world already decided on: the bed it set, or null when someone else had given one. */
    private final Map<UUID, SpawnLocation> homesPushed = new HashMap<>();
    private final InteractionBridge interactions = new InteractionBridge();
    private final Map<String, Long> zonesImported = new HashMap<>();
    private long tick;
    private boolean installed;
    private volatile Boolean forced;
    private String lastError = "";

    private LivingService() { }

    public boolean installed() { return installed && world != null; }
    public boolean enabled() { return installed() && (forced != null ? forced : LivingSettings.current().enabled()) && ServerScheduler.getInstance().isRunning(); }

    /**
     * Overrides the configured switch ({@code null} goes back to the file). The GameTest server starts with the living world
     * dormant, so that the physical tests of other engines measure those engines alone; the living world's own tests wake it.
     */
    public void force(Boolean on) { forced = on; }
    /** The living world; empty until the server has started it. */
    public Optional<LivingWorld> world() { return Optional.ofNullable(world); }
    ResourceItems items() { return items; }
    public Set<UUID> debugViewers() { return debugViewers; }
    public boolean toggleDebug(UUID player) { return debugViewers.add(player) || !debugViewers.remove(player); }
    public boolean toggleHud(UUID player) { return hudViewers.add(player) || !hudViewers.remove(player); }
    public String lastError() { return lastError; }
    public long ticks() { return tick; }

    /** What the physical interactions did this session (scans, points found, beds placed, fires changed, crops tended). */
    public String interactionStats() {
        return String.format("Interacciones: %d escaneos, %d puntos, %d camas reales, %d fuegos, %d cultivos cuidados, %d puntos registrados",
                interactions.scans(), interactions.pointsFound(), interactions.bedsPlaced(), interactions.firesChanged(), interactions.cropsTended(),
                world == null ? 0 : world.world().interactions().count());
    }

    // ------------------------------------------------------------------ lifecycle

    public void install(MinecraftServer server) {
        if (installed) return;
        LivingSettings s = LivingSettings.current();
        ServerLevel overworld = server.overworld();
        if (Boolean.getBoolean("samuraiai.gameTests")) forced = false;
        world = new LivingWorld(LivingWorld.Settings.current(), EventSink.eventBus(), overworld.getSeed());
        refreshItems(s);
        world.useOutside(new CognitionOutside(items));
        world.useClassifier(new BiomeClassifier());
        Path root = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("samuraiai").resolve("living");
        world.useStorage(root);
        var results = world.load();
        results.forEach((file, r) -> { if (!r.usable() && r.status() != yadi.samuraiai.ai.cognition.storage.LoadResult.Status.MISSING) SamuraiLogger.PERSISTENCE.warn("Living world file {}: {} {}", file, r.status(), r.detail()); });
        CalendarEngine cal = world.calendar();
        cal.resume(overworld.getGameTime(), overworld.getDayTime(), sunMoves(overworld));
        long saved = cal.savedAtRealMillis();
        if (saved > 0) {
            long applied = cal.applyOffline(cal.realNow() - saved);
            if (applied > 0) SamuraiLogger.PERSISTENCE.info("Living world: {} Deiliora minutes passed while the server was stopped", applied);
        }
        NPCEventBus bus = NPCEventBus.getInstance();
        subscriptions.add(bus.subscribe(NPCActivatedEvent.class, e -> NPCManager.getInstance().find(e.npcId()).ifPresent(this::activated)));
        subscriptions.add(bus.subscribe(NPCDeactivatedEvent.class, e -> deactivated(e.npcId(), e.reason())));
        subscriptions.add(bus.subscribe(RoutineCompletedEvent.class, this::onRoutine));
        subscriptions.add(bus.subscribe(ThreatDetectedEvent.class, this::onThreat));
        subscriptions.add(bus.subscribe(yadi.samuraiai.event.npc.CombatStartedEvent.class, this::onCombat));
        subscriptions.add(bus.subscribe(WeatherChangedEvent.class, this::onWeather));
        subscriptions.add(bus.subscribe(QuestCreatedEvent.class, this::onQuestCreated));
        subscriptions.add(bus.subscribe(FestivalStartedEvent.class, e -> announceEverywhere("Comienza " + e.name() + ".")));
        subscriptions.add(bus.subscribe(VillageSecurityChangedEvent.class, this::onSecurity));
        subscriptions.add(bus.subscribe(VillageEventStartedEvent.class, e -> announceNear(e.villageId(), e.title())));
        subscriptions.add(bus.subscribe(WorldEventPhaseEvent.class, this::onWorldEvent));
        subscriptions.add(bus.subscribe(yadi.samuraiai.living.core.LivingEvent.class, e -> { if (debug(e.domain())) SamuraiLogger.EVENTS.info("[living/{}] {}", e.domain(), e); }));
        SchedulerService.getInstance().scheduler().registerBiasSource("living", this::bias);
        installed = true;
        if (enabled()) applyVanillaWeather(overworld);
        SamuraiLogger.CORE.info("Living world started: {} ({} regions, {} settlements, {} villages, {} people)", cal.today().describe(), world.world().regionCount(),
                world.world().settlements().size(), world.villages().villages().size(), world.families().people().size());
    }

    /** Server stopping: everything is written, then the session is forgotten. */
    public void shutdown() {
        if (installed && world != null) {
            try { int files = world.save(true); SamuraiLogger.PERSISTENCE.info("Living world saved ({} files)", files); }
            catch (RuntimeException error) { SamuraiLogger.PERSISTENCE.error("Living world save failed", error); }
        }
        subscriptions.forEach(EventSubscription::close);
        subscriptions.clear();
        SchedulerService.getInstance().scheduler().unregisterBiasSource("living");
        if (world != null) world.reset();
        world = null;
        debugViewers.clear(); hudViewers.clear(); stillness.clear(); lastThreat.clear(); toldOffers.clear(); homesPushed.clear(); zonesImported.clear(); interactions.reset();
        tick = 0; installed = false; forced = null; lastError = "";
    }

    private void refreshItems(LivingSettings s) {
        if (s.resourceItems() == itemSource && tick > 0) return;
        itemSource = s.resourceItems();
        items = new ResourceItems(itemSource);
        items.problems().forEach(p -> SamuraiLogger.CORE.warn("resourceItems: {}", p));
    }

    /** Each engine's {@code debugLogging} logs that engine's events; the hub's logs every engine's. */
    private static boolean debug(String domain) {
        if (LivingSettings.current().debugLogging()) return true;
        return switch (domain) {
            case "calendar" -> yadi.samuraiai.living.calendar.engine.CalendarSettings.current().debugLogging();
            case "world" -> yadi.samuraiai.living.world.engine.WorldSettings.current().debugLogging();
            case "village" -> yadi.samuraiai.living.village.engine.VillageSettings.current().debugLogging();
            case "economy" -> yadi.samuraiai.living.economy.engine.EconomySettings.current().debugLogging();
            case "quest" -> yadi.samuraiai.living.quest.engine.QuestSettings.current().debugLogging();
            case "family" -> yadi.samuraiai.living.family.engine.FamilySettings.current().debugLogging();
            default -> false;
        };
    }

    private static boolean sunMoves(ServerLevel level) { return level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT); }

    // ------------------------------------------------------------------ the tick

    public void tick() {
        if (!enabled()) return;
        ServerLevel overworld = ServerWorlds.level("minecraft:overworld").orElse(null);
        if (overworld == null) return;
        tick++;
        LivingSettings s = LivingSettings.current();
        try {
            List<ServerPlayer> players = ServerWorlds.onlinePlayers();
            List<StreamingEngine.Viewer> viewers = new ArrayList<>(players.size());
            for (ServerPlayer p : players) viewers.add(new StreamingEngine.Viewer(p.level.dimension().location().toString(), p.getX(), p.getZ()));
            world.tick(overworld.getGameTime(), overworld.getDayTime(), sunMoves(overworld), viewers);
            if (tick % s.playerSignalTicks() == 0) playerSignals(players, s);
            if (tick % 200 == 0) pushHomes();
            if (tick % s.zoneImportTicks() == 0) { refreshItems(s); ZoneBridge.importZones(world, zonesImported); }
            if (s.worldInteractions()) {
                if (tick % 20 == 0) interactions.scanNext(world, tick);
                if (tick % 100 == 0) interactions.act(world, v -> cellAt(v.dimension(), v.x(), v.z()));
            }
            if (tick % 6000 == 0) { toldOffers.values().removeIf(t -> tick - t > 12000); lastThreat.values().removeIf(t -> tick - t > 6000); }
        } catch (RuntimeException error) {
            lastError = error.toString();
            SamuraiLogger.BRAIN.warn("Living world tick failed: {}", error.toString());
        }
        if (!debugViewers.isEmpty() && tick % 20 == 0) LivingDebugRenderer.render(this, debugViewers);
    }

    // ------------------------------------------------------------------ NPCs

    private void activated(NPCRuntime runtime) {
        if (!enabled()) return;
        SpawnLocation loc = runtime.getInstance().getLocation();
        if (loc == null) return;
        int shift = 0;
        if (CognitionService.getInstance().enabled()) shift = (int) Math.round(-30.0D * CognitionService.getInstance().engine().personality(runtime.getId()).lean(Trait.DILIGENCE));
        String type = runtime.getDefinition().getType().value();
        world.npcArrived(runtime.getId(), runtime.getName(), type, loc.dimensionKey(), loc.x(), loc.y(), loc.z(), shift);
        homesPushed.remove(runtime.getId());
        pushHome(runtime);
    }

    private void deactivated(UUID id, String reason) {
        if (!installed()) return;
        homesPushed.remove(id);
        if ("server stopped".equals(reason)) { world.villages().citizen(id).ifPresent(c -> c.embodied(false)); return; }
        world.npcLeft(id, false, reason == null ? "se fue" : reason);
    }

    /** A real death in the world: the citizen dies and the family records it (never guessed, never simulated). */
    void onDeath(Entity victim, Entity killer) {
        if (!enabled()) return;
        NPCRuntime npc = owner(victim.getUUID());
        String by = killer == null ? "" : killer instanceof Player p ? p.getGameProfile().getName() : killer.getType().getDescription().getString();
        if (npc != null) {
            world.npcLeft(npc.getId(), true, by.isEmpty() ? "murió" : "murió a manos de " + by);
            return;
        }
        if (killer instanceof ServerPlayer player) {
            var key = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
            String kind = key == null ? "" : key.getPath();
            world.quests().killed(player.getUUID(), kind, victim instanceof Enemy, victim.level.dimension().location().toString(), victim.getX(), victim.getY(), victim.getZ());
        }
    }

    private static NPCRuntime owner(UUID entity) {
        for (NPCRuntime runtime : NPCManager.getInstance().getActive())
            if (runtime.isActive() && runtime.getController().ownsEntity(runtime.getInstance(), entity)) return runtime;
        return null;
    }

    private void onRoutine(RoutineCompletedEvent e) {
        if (enabled() && e.performedTicks() > 0) world.routineCompleted(e.npcId(), e.routine(), e.performedTicks());
    }

    private void onThreat(ThreatDetectedEvent e) {
        if (!enabled() || !e.level().atLeast(yadi.samuraiai.ai.perception.awareness.ThreatLevel.DANGER)) return;
        Village v = world.villages().villageOf(e.npcId()).orElse(null);
        if (v == null) return;
        Long last = lastThreat.get(v.id());
        if (last != null && tick - last < 200) return;
        lastThreat.put(v.id(), tick);
        // the village's threat is in points (alert and danger thresholds): each perceived level above SAFE is worth threatPerLevel
        double amount = yadi.samuraiai.living.village.engine.VillageSettings.current().threatPerLevel() * e.level().ordinal();
        world.villages().reportThreat(v.id(), amount, e.label());
    }

    /** A citizen fighting is a threat to its village, worth {@code combatThreat} points (throttled per village like perception). */
    private void onCombat(yadi.samuraiai.event.npc.CombatStartedEvent e) {
        if (!enabled()) return;
        Village v = world.villages().villageOf(e.npcId()).orElse(null);
        if (v == null) return;
        Long last = lastThreat.get(v.id());
        if (last != null && tick - last < 200) return;
        lastThreat.put(v.id(), tick);
        world.villages().reportThreat(v.id(), yadi.samuraiai.living.village.engine.VillageSettings.current().combatThreat(), "combate");
    }

    /** The village timetable, profession, events and security, as points for the scheduler's routines. */
    private Map<RoutineType, Double> bias(UUID npc) {
        if (!enabled()) return Map.of();
        Map<String, Double> raw = world.routineBias(npc);
        if (raw.isEmpty()) return Map.of();
        Map<RoutineType, Double> out = new EnumMap<>(RoutineType.class);
        raw.forEach((k, v) -> { try { out.put(RoutineType.valueOf(k), v); } catch (IllegalArgumentException ignored) { } });
        return out;
    }

    /** The bed the Village Engine gave an NPC becomes its home, unless it already has one (a player or another system set it). */
    private void pushHomes() {
        for (NPCRuntime runtime : NPCManager.getInstance().getActive()) if (runtime.isActive() && !homesPushed.containsKey(runtime.getId())) pushHome(runtime);
    }

    /** A house was scanned and a resident's bed moved onto a real bed: the NPC follows, if its home is still the one set here. */
    void bedMoved(UUID npc) {
        SpawnLocation previous = homesPushed.get(npc);
        NPCRuntime runtime = NPCManager.getInstance().find(npc).orElse(null);
        if (previous == null || runtime == null || !previous.equals(runtime.getInstance().getHome())) return;
        if (setBed(runtime)) SchedulerService.getInstance().rehome(npc);
    }

    private void pushHome(NPCRuntime runtime) {
        if (runtime.getInstance().homeAssigned()) { homesPushed.put(runtime.getId(), null); return; }
        setBed(runtime);
    }

    private boolean setBed(NPCRuntime runtime) {
        HomeRecord home = world.villages().home(runtime.getId()).orElse(null);
        Village v = world.villages().villageOf(runtime.getId()).orElse(null);
        if (home == null || v == null) return false;
        SpawnLocation bed = new SpawnLocation(v.dimension(), home.bedX(), home.bedY(), home.bedZ(), 0.0F);
        runtime.getInstance().setHome(bed);
        homesPushed.put(runtime.getId(), bed);
        return true;
    }

    // ------------------------------------------------------------------ players

    private void playerSignals(List<ServerPlayer> players, LivingSettings s) {
        double minutes = s.playerSignalTicks() * world.calendar().clock().minutesPerTick();
        for (ServerPlayer p : players) {
            String dim = p.level.dimension().location().toString();
            UUID id = p.getUUID();
            world.quests().arrived(id, dim, p.getX(), p.getY(), p.getZ());
            Stillness st = stillness.get(id);
            boolean still = p.isCrouching() && st != null && st.dimension().equals(dim) && Math.abs(st.x() - p.getX()) < 0.6 && Math.abs(st.z() - p.getZ()) < 0.6;
            if (still) world.quests().meditated(id, dim, p.getX(), p.getY(), p.getZ(), minutes);
            else stillness.put(id, new Stillness(dim, p.getX(), p.getY(), p.getZ(), tick));
            if (hudViewers.contains(id)) p.displayClientMessage(Component.literal(hud(dim, p.getX(), p.getZ())), true);
        }
        stillness.keySet().removeIf(id -> players.stream().noneMatch(p -> p.getUUID().equals(id)));
    }

    /** A player spoke with an NPC and got an answer: TALK objectives, and the NPC mentions the problems it needs help with. */
    public void conversation(NPCRuntime runtime, UUID player, String playerName) {
        if (!enabled() || player == null) return;
        String role = world.villages().citizen(runtime.getId()).map(Citizen::profession).orElse("");
        world.quests().talked(player, runtime.getId(), role);
        List<Quest> offers = world.quests().givenBy(runtime.getId()).stream().filter(q -> q.state() == Quest.State.OFFERED).toList();
        if (offers.isEmpty()) return;
        Long told = toldOffers.get(runtime.getId());
        if (told != null && tick - told < 6000) return;
        toldOffers.put(runtime.getId(), tick);
        ServerWorlds.playerById(player).ifPresent(p -> {
            for (Quest q : offers) p.sendSystemMessage(Component.literal("§6" + runtime.getName() + " necesita ayuda: §f" + q.title() + " §7(/samuraiai living quest accept " + q.id().toString().substring(0, 8) + ")"));
        });
    }

    // ------------------------------------------------------------------ announcements

    private void onQuestCreated(QuestCreatedEvent e) {
        if (!LivingSettings.current().announceToPlayers() || e.settlementId() == null) return;
        announceNear(e.settlementId(), "Se habla de un problema: " + e.title() + " (/samuraiai living quest list)", yadi.samuraiai.living.quest.engine.QuestSettings.current().offerRadius());
    }

    private void onSecurity(VillageSecurityChangedEvent e) {
        if (!SecurityState.PEACE.name().equals(e.to())) announceNear(e.villageId(), "La aldea está en " + e.to().toLowerCase(Locale.ROOT) + ": " + e.reason());
    }

    private void onWorldEvent(WorldEventPhaseEvent e) {
        if (!"ACTIVE".equals(e.phase()) && !"RESOLVED".equals(e.phase())) return;
        if (e.settlementId() != null) announceNear(e.settlementId(), e.title() + ("ACTIVE".equals(e.phase()) ? "" : " — ha terminado."));
    }

    private void announceNear(UUID villageOrSettlement, String text) { announceNear(villageOrSettlement, text, LivingSettings.current().announceRadius()); }

    /** Tells players within the village radius plus {@code extra} blocks (quest offers use the quest engine's {@code offerRadius}). */
    private void announceNear(UUID villageOrSettlement, String text, double extra) {
        if (!installed() || !LivingSettings.current().announceToPlayers()) return;
        Village v = world.villages().village(villageOrSettlement).orElse(null);
        if (v == null) return;
        double radius = v.radius() + extra;
        for (ServerPlayer p : ServerWorlds.onlinePlayers())
            if (p.level.dimension().location().toString().equals(v.dimension()) && Math.hypot(p.getX() - v.x(), p.getZ() - v.z()) <= radius)
                p.sendSystemMessage(Component.literal("§e[" + v.name() + "] §f" + text));
    }

    private void announceEverywhere(String text) {
        if (!installed() || !LivingSettings.current().announceToPlayers()) return;
        for (ServerPlayer p : ServerWorlds.onlinePlayers()) p.sendSystemMessage(Component.literal("§e[Deiliora] §f" + text));
    }

    // ------------------------------------------------------------------ weather

    private void onWeather(WeatherChangedEvent e) {
        if (!enabled() || !CalendarEngine.WORLD_CELL.equals(e.cell())) return;
        ServerWorlds.level("minecraft:overworld").ifPresent(this::applyVanillaWeather);
    }

    /** The world cell's weather becomes the overworld sky until its next change (a player's /weather holds until then). */
    private void applyVanillaWeather(ServerLevel level) {
        if (!yadi.samuraiai.living.calendar.engine.CalendarSettings.current().driveVanillaWeather() || world == null) return;
        var cell = world.calendar().weatherCell(CalendarEngine.WORLD_CELL);
        WeatherKind kind = cell.current();
        double perTick = Math.max(1e-6, world.calendar().clock().minutesPerTick());
        int ticks = (int) Math.max(1200, Math.min(72000, (cell.nextChange() - world.calendar().now()) / perTick));
        boolean rain = kind.wet(), thunder = kind == WeatherKind.STORM || kind == WeatherKind.HAIL;
        level.setWeatherParameters(rain ? 0 : ticks, rain ? ticks : 0, rain, thunder);
    }

    // ------------------------------------------------------------------ what dialogue and the HUD read

    /** The weather cell for a position: its region's, when the region has its own climate, else the world's. */
    public String cellAt(String dimension, double x, double z) {
        if (world == null) return CalendarEngine.WORLD_CELL;
        return world.world().regionAt(dimension, x, z).map(r -> "region:" + r.id()).filter(c -> world.calendar().weather().cell(c).isPresent()).orElse(CalendarEngine.WORLD_CELL);
    }

    String hud(String dimension, double x, double z) {
        CalendarRuntime r = world.calendar().snapshot(cellAt(dimension, x, z));
        CalendarDate d = r.date();
        StringBuilder sb = new StringBuilder();
        sb.append(d.day()).append(" de ").append(d.monthName()).append(" · ").append(CalendarDate.seasonName(d.season())).append(" · ")
                .append(String.format("%02d:%02d", d.hour(), d.minuteOfHour())).append(" · ").append(r.weather().label()).append(String.format(" %.0f°C", r.temperature()))
                .append(" · ").append(r.moon().label());
        if (!r.festivals().isEmpty()) sb.append(" · ").append(String.join(", ", r.festivals()));
        return sb.toString();
    }

    /** "Año 3, 12 de Uzuki (primavera), 14:05, tarde" for an NPC's prompt; empty when the living world is off. */
    public Optional<String> timeOfDay(UUID npc) {
        if (!enabled()) return Optional.empty();
        CalendarDate d = world.calendar().today();
        return Optional.of(d.describe() + ", " + d.phase().label());
    }

    /** What an NPC knows about its world right now, for the dialogue prompt (Spanish, one fact per line). */
    public List<String> promptLines(UUID npc, UUID player) {
        if (!enabled()) return List.of();
        List<String> out = new ArrayList<>();
        try {
            Village v = world.villages().villageOf(npc).orElse(null);
            String cell = v == null ? CalendarEngine.WORLD_CELL : cellAt(v.dimension(), v.x(), v.z());
            CalendarRuntime r = world.calendar().snapshot(cell);
            out.add(String.format("Es %s; hace %s y %.0f°C; esta noche habrá %s.", CalendarDate.seasonName(r.date().season()), r.weather().label(), r.temperature(), r.moon().label()));
            if (!r.festivals().isEmpty()) out.add("Hoy se celebra " + String.join(" y ", r.festivals()) + ".");
            if (!r.holidays().isEmpty()) out.add("Hoy es día de " + String.join(" y ", r.holidays()) + ".");
            Citizen c = world.villages().citizen(npc).orElse(null);
            if (v != null && c != null) {
                String job = world.world().professions().get(c.profession()).map(p -> p.name().toLowerCase(Locale.ROOT)).orElse(c.profession());
                out.add("Vives en " + v.name() + " (" + world.villages().census(v.id()).residents() + " habitantes) y eres " + job + ".");
                if (v.security().state() != SecurityState.PEACE) out.add("La aldea está en " + v.security().state().name().toLowerCase(Locale.ROOT) + ": todos andan con cuidado.");
                world.economy().settlement(v.id()).ifPresent(se -> se.balances().values().stream()
                        .filter(b -> b.state() == yadi.samuraiai.living.economy.scarcity.MarketBalance.State.SCARCE).limit(2)
                        .forEach(b -> out.add("Escasea " + world.economy().resources().get(b.resource()).map(d -> d.name().toLowerCase(Locale.ROOT)).orElse(b.resource()) + " en la aldea.")));
            }
            Person me = world.families().person(npc).orElse(null);
            if (me != null) {
                List<String> kin = new ArrayList<>();
                for (UUID p : world.families().partners(npc)) world.families().person(p).ifPresent(x -> kin.add("tu pareja " + x.name().full()));
                for (UUID p : world.families().parents(npc)) world.families().person(p).ifPresent(x -> kin.add((x.state() == yadi.samuraiai.living.family.lifecycle.LifeState.DECEASED_FUTURE || x.state() == yadi.samuraiai.living.family.lifecycle.LifeState.HISTORICAL ? "tu difunto progenitor " : "tu progenitor ") + x.name().full()));
                for (UUID p : world.families().children(npc)) world.families().person(p).ifPresent(x -> kin.add("tu hijo/a " + x.name().full()));
                world.families().familyOf(npc).ifPresent(f -> out.add("Perteneces a la familia " + f.name() + (kin.isEmpty() ? "." : ": " + String.join(", ", kin.subList(0, Math.min(4, kin.size()))) + ".")));
            }
            for (Quest q : world.quests().givenBy(npc))
                out.add((q.state() == Quest.State.OFFERED ? "Buscas quien te ayude con: " : "Te están ayudando con: ") + q.title() + ".");
            if (player != null)
                for (Quest q : world.quests().active(player))
                    if (v != null && v.id().equals(q.settlement()) && !npc.equals(q.giver())) { out.add("Sabes que este viajero ayuda a la aldea con: " + q.title() + "."); break; }
        } catch (RuntimeException error) {
            lastError = error.toString();
        }
        return out;
    }
}
