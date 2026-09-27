package yadi.samuraiai.living.sim;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import yadi.samuraiai.ai.cognition.storage.LoadResult;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.event.NpcEvent;
import yadi.samuraiai.living.calendar.engine.CalendarEngine;
import yadi.samuraiai.living.calendar.engine.CalendarSettings;
import yadi.samuraiai.living.calendar.persistence.CalendarStorage;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.core.persistence.LivingStorage;
import yadi.samuraiai.living.economy.engine.EconomyEngine;
import yadi.samuraiai.living.economy.engine.EconomySettings;
import yadi.samuraiai.living.economy.persistence.EconomyStorage;
import yadi.samuraiai.living.family.engine.FamilyEngine;
import yadi.samuraiai.living.family.engine.FamilySettings;
import yadi.samuraiai.living.family.lifecycle.LifeState;
import yadi.samuraiai.living.family.persistence.FamilyStorage;
import yadi.samuraiai.living.quest.engine.QuestEngine;
import yadi.samuraiai.living.quest.engine.QuestSettings;
import yadi.samuraiai.living.quest.persistence.QuestStorage;
import yadi.samuraiai.living.village.citizens.Citizen;
import yadi.samuraiai.living.village.engine.VillageEngine;
import yadi.samuraiai.living.village.engine.VillageSettings;
import yadi.samuraiai.living.village.persistence.VillageStorage;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.world.engine.WorldEngine;
import yadi.samuraiai.living.world.engine.WorldPorts;
import yadi.samuraiai.living.world.engine.WorldSettings;
import yadi.samuraiai.living.world.persistence.WorldStorage;
import yadi.samuraiai.living.world.regions.Region;
import yadi.samuraiai.living.world.settlements.Settlement;
import yadi.samuraiai.living.world.settlements.SettlementType;
import yadi.samuraiai.living.world.streaming.StreamingEngine;

/**
 * The living world as one simulation: the hub that owns the six engines (Calendar, World, Villages, Economy, Quests, Family),
 * implements each engine's ports over the others ({@link LivingBridges}), carries the cross-system flows on events
 * ({@link LivingReactions}: a scarcity becomes a quest, an attack changes a village's security, a harvest reaches the markets,
 * a citizen gets a family...), spreads the work over tick buckets, and persists everything. The engines never see each other:
 * only this class and its helpers do. Pure Java; the Forge adapter drives it and supplies {@link Outside}.
 *
 * <p>The cycle of the specification — world → regions → settlements → villages → citizens → professions → production and
 * consumption → economy → trade → needs → quests → consequences → calendar → history → families → generations → legacy →
 * world — runs here, every day, whether a player is near or not.
 */
public final class LivingWorld {
    /** The settings of every engine (suppliers, so a reload is seen at the next tick). */
    public record Settings(Supplier<LivingSettings> living, Supplier<CalendarSettings> calendar, Supplier<WorldSettings> world, Supplier<VillageSettings> village,
                           Supplier<EconomySettings> economy, Supplier<QuestSettings> quest, Supplier<FamilySettings> family) {
        public static Settings current() {
            return new Settings(LivingSettings::current, CalendarSettings::current, WorldSettings::current, VillageSettings::current, EconomySettings::current, QuestSettings::current,
                    FamilySettings::current);
        }
        public static Settings defaults() {
            LivingSettings l = LivingSettings.defaults(); CalendarSettings c = CalendarSettings.defaults(); WorldSettings w = WorldSettings.defaults(); VillageSettings v = VillageSettings.defaults();
            EconomySettings e = EconomySettings.defaults(); QuestSettings q = QuestSettings.defaults(); FamilySettings f = FamilySettings.defaults();
            return new Settings(() -> l, () -> c, () -> w, () -> v, () -> e, () -> q, () -> f);
        }
    }

    /** A planned founding, read by the reaction to the SettlementFoundedEvent. */
    record FoundingPlan(boolean plan, boolean built) { }

    private final Settings settings;
    private final EventSink external;
    private final LivingMetrics metrics = new LivingMetrics();
    final CalendarEngine calendar;
    final WorldEngine world;
    final VillageEngine villages;
    final EconomyEngine economy;
    final QuestEngine quests;
    final FamilyEngine families;
    private final LivingReactions reactions;
    private final LivingBridges bridges;
    private final ConditionScanner scanner;
    private Outside outside = Outside.NEUTRAL;
    private LivingStorage storage;
    private long tickCount;
    private int depth;
    FoundingPlan pendingPlan;

    public LivingWorld(Settings settings, EventSink external, long seed) {
        this.settings = Objects.requireNonNull(settings);
        this.external = Objects.requireNonNull(external);
        EventSink dispatch = this::dispatch;
        this.calendar = new CalendarEngine(settings.calendar(), dispatch, seed);
        this.world = new WorldEngine(settings.world(), dispatch, calendar, seed);
        this.villages = new VillageEngine(settings.village(), dispatch, calendar, seed);
        this.economy = new EconomyEngine(settings.economy(), dispatch, calendar, seed);
        this.quests = new QuestEngine(settings.quest(), dispatch, calendar, seed);
        this.families = new FamilyEngine(settings.family(), dispatch, calendar, seed);
        this.bridges = new LivingBridges(this);
        this.reactions = new LivingReactions(this);
        this.scanner = new ConditionScanner(this);
        bridges.wire();
    }

    // ------------------------------------------------------------------ events: every engine's facts pass through the hub on their way to the bus

    private void dispatch(NpcEvent event) {
        if (depth < 24) {
            depth++;
            try { reactions.handle(event); }
            catch (RuntimeException error) { metrics.reactionFailures.incrementAndGet(); metrics.lastError = event.getName() + ": " + error; }
            finally { depth--; }
        } else metrics.droppedReactions.incrementAndGet();
        metrics.events.incrementAndGet();
        external.publish(event);
    }

    // ------------------------------------------------------------------ wiring

    public void useOutside(Outside o) { this.outside = Objects.requireNonNull(o); }
    public void useClassifier(WorldPorts.RegionClassifier c) { world.useClassifier(c); }
    Outside outside() { return outside; }
    LivingSettings living() { return settings.living().get(); }

    /** Persistence under a directory (the world's data folder): every engine registers its sections. */
    public void useStorage(Path root) {
        storage = new LivingStorage(root, living().compressStorage());
        CalendarStorage.sections(calendar).forEach(storage::register);
        WorldStorage.sections(world).forEach(storage::register);
        VillageStorage.install(storage, villages);
        EconomyStorage.sections(economy).forEach(storage::register);
        QuestStorage.sections(quests).forEach(storage::register);
        FamilyStorage.sections(families).forEach(storage::register);
    }

    /** Loads everything. Returns the result of each file; the genealogy is audited afterwards. */
    public Map<String, LoadResult> load() {
        if (storage == null) return Map.of();
        villages.loading(true);
        try { return storage.loadAll(); }
        finally { villages.loading(false); metrics.auditProblems = families.audit().size(); }
    }

    public int save(boolean all) { return storage == null ? 0 : all ? storage.saveAll() : storage.saveDirty(living().maxSavesPerTick()); }
    public LivingStorage storage() { return storage; }

    // ------------------------------------------------------------------ the tick

    /**
     * One server tick. The calendar advances every tick (cheap); the world streams and simulates the regions that are due
     * (its own cadence and budget); villages, economy, quests and markets each get one tick bucket in turn, so no two heavy
     * systems run in the same tick. A new day's work (world events, families, trade, quest conditions) runs one stage per tick
     * after the day begins. Dirty files are saved on the save interval.
     */
    public void tick(long gameTime, long dayTime, boolean sunMoves, List<StreamingEngine.Viewer> viewers) {
        LivingSettings s = living();
        if (!s.enabled()) return;
        long started = System.nanoTime();
        tickCount++;
        calendar.tick(gameTime, dayTime, sunMoves);
        long t1 = System.nanoTime();
        world.tick(viewers);
        long t2 = System.nanoTime();
        dayStep();
        switch ((int) (tickCount % s.buckets())) {
            case 0 -> { villages.tick(); metrics.villageNanos.addAndGet(System.nanoTime() - t2); }
            case 1 -> { economy.tick(); metrics.economyNanos.addAndGet(System.nanoTime() - t2); }
            case 2 -> { quests.tick(); metrics.questNanos.addAndGet(System.nanoTime() - t2); }
            case 3 -> economy.syncMarkets();
            default -> { }
        }
        if (storage != null && tickCount % s.saveIntervalTicks() == 0) save(false);
        metrics.calendarNanos.addAndGet(t1 - started);
        metrics.worldNanos.addAndGet(t2 - t1);
        metrics.tick(System.nanoTime() - started);
    }

    /** One day's work, done in stages on consecutive ticks so no single tick pays for a whole day. */
    private record DayWork(long dayIndex, long skippedDays) { }

    /** The daily stages, in order: spontaneous world events, families, trade planning, quest conditions. */
    static final int DAY_STAGES = 4;

    private final java.util.ArrayDeque<DayWork> dayQueue = new java.util.ArrayDeque<>();
    private int dayStage;

    /** A day began (called by the reactions from the calendar's DayChangedEvent): its work is queued, one stage per tick. */
    void newDay(long dayIndex, long skippedDays) {
        dayQueue.add(new DayWork(dayIndex, skippedDays));
    }

    /** Runs the next stage of the oldest pending day, if any. */
    private void dayStep() {
        DayWork d = dayQueue.peek();
        if (d == null) return;
        long started = System.nanoTime();
        switch (dayStage) {
            case 0 -> world.daily(d.dayIndex());
            case 1 -> {
                List<UUID> villageIds = new ArrayList<>();
                for (Village v : villages.villages()) villageIds.add(v.id());
                families.simulate(villageIds, 1 + Math.min(30, d.skippedDays()));
            }
            case 2 -> economy.planTrade();
            default -> { if (living().conditionScan()) scanner.scan(d.dayIndex()); }
        }
        long spent = System.nanoTime() - started;
        metrics.dayNanos.addAndGet(spent);
        metrics.dayStageNanos[dayStage].addAndGet(spent);
        metrics.maxDayStageNanos.accumulateAndGet(spent, Math::max);
        if (++dayStage >= DAY_STAGES) { dayStage = 0; dayQueue.poll(); metrics.days.incrementAndGet(); }
    }

    /** Days whose work is still being done (a long jump queues many). */
    public int pendingDays() { return dayQueue.size(); }

    // ------------------------------------------------------------------ what the adapter tells the living world

    /**
     * An NPC is in the world (spawned or restored): it becomes a citizen of the village it stands in (or the nearest one), a
     * village is founded for it if there is none and automatic villages are on, and — through the reactions — it gets a family
     * and, if it trades, a stall.
     */
    public Optional<Citizen> npcArrived(UUID npc, String name, String npcType, String dimension, double x, double y, double z, int personalityShift) {
        if (!living().enabled()) return Optional.empty();
        Optional<Village> village = villages.villageAt(dimension, x, z).or(() -> villages.nearest(dimension, x, z, living().villageJoinRadius()));
        if (village.isEmpty() && living().autoVillages()) {
            Region region = world.ensureRegion(dimension, x, z);
            Settlement s = foundSettlement(nameForNewVillage(region), SettlementType.VILLAGE, dimension, x, y, z, living().autoVillageRadius(), living().planAutoVillages(), false,
                    Provenance.of("npc", npc.toString(), "llegó " + name, calendar.now()));
            village = villages.village(s.id());
        }
        if (village.isEmpty()) return Optional.empty();
        return Optional.of(villages.admit(village.get().id(), npc, name, npcType, true, personalityShift));
    }

    private String nameForNewVillage(Region region) {
        String base = region.name().split(" ")[0];
        long same = world.settlements().stream().filter(s -> s.name().startsWith(base)).count();
        return same == 0 ? base + " no Sato" : base + " no Sato " + (same + 1);
    }

    /** An NPC left: died (a real death in the world), or was removed for good, or moved to another village. */
    public void npcLeft(UUID npc, boolean died, String reason) {
        villages.depart(npc, died ? Citizen.Status.DECEASED : Citizen.Status.MISSING, reason);
    }

    /** Founds a settlement; its village and economy follow (see {@link LivingReactions}). {@code plan}: lay out the village. */
    public Settlement foundSettlement(String name, SettlementType type, String dimension, double x, double y, double z, double radius, boolean plan, boolean built, Provenance origin) {
        pendingPlan = new FoundingPlan(plan, built);
        try { return world.foundSettlement(name, type, dimension, x, y, z, radius, origin); }
        finally { pendingPlan = null; }
    }

    /** The routine bias the living world asks of the Behavior Scheduler for an NPC (routine name → points). */
    public Map<String, Double> routineBias(UUID npc) {
        if (!living().enabled()) return Map.of();
        return villages.routineBias(npc).bias();
    }

    /** The scheduler reported a finished routine: work routines become profession hours (and so, later, goods). */
    public void routineCompleted(UUID npc, String routine, long ticks) {
        if (!"WORK".equals(routine) && !"MERCHANT".equals(routine) && !"GUARD".equals(routine) && !"PATROL".equals(routine) && !"TRAINING".equals(routine)) return;
        double hours = ticks * (calendar.clock().minutesPerTick()) / 60.0D;
        villages.recordWork(npc, hours);
    }

    /** A player handed goods to a settlement (a quest turn-in): they are stored with the player as origin and count for quests. */
    public double deliver(UUID player, String playerName, UUID settlement, String resource, double quantity, double quality) {
        double stored = economy.donate(settlement, resource, quantity, quality, Provenance.of("player", player.toString(), playerName, calendar.now()));
        return quests.delivered(player, settlement, resource, stored);
    }

    public void familyDeath(UUID npc, String cause) { families.lifeState(npc, LifeState.DECEASED_FUTURE, cause); }

    // ------------------------------------------------------------------ accessors

    public CalendarEngine calendar() { return calendar; }
    public WorldEngine world() { return world; }
    public VillageEngine villages() { return villages; }
    public EconomyEngine economy() { return economy; }
    public QuestEngine quests() { return quests; }
    public FamilyEngine families() { return families; }
    public LivingMetrics metrics() { return metrics; }
    public ConditionScanner scanner() { return scanner; }
    public long tickCount() { return tickCount; }

    /** Forgets the session (the server is stopping, after saving). */
    public void reset() {
        calendar.reset(); world.reset(); villages.reset(); economy.reset(); quests.reset(); families.reset(); metrics.reset(); tickCount = 0; storage = null;
        dayQueue.clear(); dayStage = 0;
    }
}
