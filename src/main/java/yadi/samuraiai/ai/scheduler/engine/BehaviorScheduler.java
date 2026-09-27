package yadi.samuraiai.ai.scheduler.engine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import yadi.samuraiai.ai.scheduler.events.SchedulerOptimizationEvent;
import yadi.samuraiai.ai.scheduler.events.TimelineChangedEvent;
import yadi.samuraiai.ai.scheduler.events.WorldScheduleEvent;
import yadi.samuraiai.ai.scheduler.group.Alarm;
import yadi.samuraiai.ai.scheduler.group.GroupCoordinator;
import yadi.samuraiai.ai.scheduler.group.GroupOrder;
import yadi.samuraiai.ai.scheduler.group.GroupRole;
import yadi.samuraiai.ai.scheduler.group.MemberSnapshot;
import yadi.samuraiai.ai.scheduler.lifestyle.Lifestyle;
import yadi.samuraiai.ai.scheduler.lifestyle.LifestyleCatalog;
import yadi.samuraiai.ai.scheduler.metrics.SchedulerMetrics;
import yadi.samuraiai.ai.scheduler.optimize.CrowdManager;
import yadi.samuraiai.ai.scheduler.optimize.OptimizationEngine;
import yadi.samuraiai.ai.scheduler.optimize.TickBucket;
import yadi.samuraiai.ai.scheduler.personality.DriftCause;
import yadi.samuraiai.ai.scheduler.response.EventResponsePlanner;
import yadi.samuraiai.ai.scheduler.routine.RoutineInstance;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.time.CalendarEvent;
import yadi.samuraiai.ai.scheduler.time.DayPeriod;
import yadi.samuraiai.ai.scheduler.time.Timeline;
import yadi.samuraiai.ai.scheduler.time.WorldCalendar;
import yadi.samuraiai.ai.scheduler.zone.PlaceResolver;
import yadi.samuraiai.ai.scheduler.zone.ZoneRegistry;
import yadi.samuraiai.ai.scheduler.zone.ZoneScheduler;
import yadi.samuraiai.event.EventSink;

/**
 * Behavior Scheduler AAA, the world tier: it owns the timeline and the world calendar, the zones and groups, and one
 * {@link NpcSchedule} per NPC. Each tick it advances the clock (announcing new periods and calendar events), sorts NPCs into
 * tick buckets by distance and urgency, lets the crowd manager pick which are due within the per-tick quota and time
 * budget, evaluates those through the {@link NpcScheduler}, and keeps groups and zones in step. It is independent of
 * Minecraft: facts come in through an {@link InputSource}, decisions go out as {@link SchedulerAdvice}. It never touches an
 * entity, a path or a task.
 */
public final class BehaviorScheduler implements NpcScheduler.Environment {
    private static final int BUCKET_REFRESH_TICKS = 20;
    private static final int AUTO_GROUP_EVERY = 5;
    static final double MAX_EXTERNAL_BIAS = 80.0D;

    private final Supplier<SchedulerSettings> settingsSupplier;
    private final SchedulerMetrics metrics = new SchedulerMetrics();
    private final ZoneRegistry zoneRegistry = new ZoneRegistry();
    private final ZoneScheduler zones = new ZoneScheduler(zoneRegistry);
    private final Map<UUID, NpcSchedule> schedules = new LinkedHashMap<>();
    private final Map<String, RoutineBiasSource> biasSources = new LinkedHashMap<>();
    private EventSink events;
    private final GroupCoordinator groups;

    private SchedulerSettings settings;
    private Engines engines;
    private NpcScheduler npcScheduler;
    private Timeline timeline;
    private WorldCalendar calendar;
    private LifestyleCatalog catalog;
    private PlaceResolver places;
    private OptimizationEngine optimizer;
    private CrowdManager crowd;

    private DayPeriod period;
    private List<CalendarEvent> activeEvents = List.of();
    private long lastGroupSync = Long.MIN_VALUE / 2, lastZoneScan = Long.MIN_VALUE / 2;
    private int groupSyncs;
    private long tick;
    private double lastCrowdScale = 1.0D;
    private long lastOptimizationEvent = Long.MIN_VALUE / 2;

    public BehaviorScheduler(Supplier<SchedulerSettings> settings, EventSink events) {
        this.settingsSupplier = Objects.requireNonNull(settings);
        this.events = Objects.requireNonNull(events);
        this.groups = new GroupCoordinator(settings.get(), events);
        rebuild(settings.get());
    }

    // ------------------------------------------------------------------ configuration

    private void rebuild(SchedulerSettings next) {
        this.settings = next;
        this.timeline = new Timeline(next);
        this.calendar = WorldCalendar.parse(next.calendar());
        this.catalog = new LifestyleCatalog(next.lifestyles());
        this.engines = Engines.create(next, calendar);
        this.npcScheduler = new NpcScheduler(engines);
        this.places = new PlaceResolver(next, zones);
        this.optimizer = new OptimizationEngine(next);
        this.crowd = new CrowdManager(next);
    }

    public void useEventSink(EventSink sink) { this.events = Objects.requireNonNull(sink); groups.useEventSink(sink); }
    public SchedulerSettings settings() { return settings; }
    public SchedulerMetrics metrics() { return metrics; }
    public ZoneRegistry zoneRegistry() { return zoneRegistry; }
    public GroupCoordinator groupCoordinator() { return groups; }
    public Timeline timeline() { return timeline; }
    public WorldCalendar worldCalendar() { return calendar; }
    public LifestyleCatalog lifestyles() { return catalog; }
    public Engines engines() { return engines; }
    public DayPeriod currentPeriod() { return period; }
    public long currentTick() { return tick; }
    public List<CalendarEvent> currentEvents() { return activeEvents; }
    public Optional<NpcSchedule> schedule(UUID id) { return Optional.ofNullable(schedules.get(id)); }
    public Optional<SchedulerAdvice> advice(UUID id) { return Optional.ofNullable(schedules.get(id)).map(NpcSchedule::advice); }
    public List<UUID> tracked() { return List.copyOf(schedules.keySet()); }
    public int zoneOccupancy(String zoneId) { return zones.occupancy(zoneId); }
    public boolean zoneAlerted(String zoneId, long now) { return zones.alerted(zoneId, now); }

    // ------------------------------------------------------------------ the tick

    public void tick(long now, long worldTime, InputSource source) {
        long started = System.nanoTime();
        this.tick = now;
        SchedulerSettings current = settingsSupplier.get();
        if (current != settings) rebuild(current);

        advanceClock(worldTime);
        Set<UUID> present = new HashSet<>(source.npcs());
        register(present, source, now);
        forgetMissing(present);

        double scale = optimizer.crowdScale(schedules.size());
        List<CrowdManager.Candidate> due = new ArrayList<>();
        for (NpcSchedule st : schedules.values()) {
            refreshBucket(st, source, now);
            int interval = optimizer.interval(st.bucket, scale);
            due.add(new CrowdManager.Candidate(st.id, interval, st.lastEvaluated, st.forced));
        }
        CrowdManager.Plan plan = crowd.plan(due, now);
        long budgetNanos = settings.budgetMicros() * 1000L;
        int evaluated = 0;
        for (UUID id : plan.evaluate()) {
            NpcSchedule st = schedules.get(id);
            if (st == null) continue;
            if (!st.forced && evaluated > 0 && System.nanoTime() - started > budgetNanos) break;
            long t0 = System.nanoTime();
            try {
                Optional<SchedulerInput> input = source.input(id, now, worldTime);
                if (input.isEmpty()) continue;
                npcScheduler.evaluate(st, input.get(), this, now, st.bucket);
            } catch (RuntimeException error) {
                metrics.failure(error);
                st.lastEvaluated = now; // do not retry a broken evaluation every tick
                if (settings.debugLogging()) error.printStackTrace();
            }
            metrics.evaluation(System.nanoTime() - t0);
            evaluated++;
        }
        int deferred = Math.max(0, plan.due() - evaluated);
        if (deferred > 0) metrics.deferred(deferred);
        announceOptimization(scale, deferred, now);

        housekeeping(now);
        publishPopulation(scale);
        metrics.tick(System.nanoTime() - started);
    }

    private void advanceClock(long worldTime) {
        DayPeriod now = timeline.periodAt(worldTime);
        long day = timeline.dayNumber(worldTime);
        List<CalendarEvent> active = calendar.activeAt(day, now);
        if (period != null && now != period) {
            metrics.timelineChanged();
            events.publish(new TimelineChangedEvent(period.name(), now.name(), day));
        }
        Set<String> before = new HashSet<>(), after = new HashSet<>();
        activeEvents.forEach(ev -> before.add(ev.name()));
        active.forEach(ev -> after.add(ev.name()));
        for (CalendarEvent ev : active) if (!before.contains(ev.name())) events.publish(new WorldScheduleEvent(ev.name(), true, now.name(), day));
        for (CalendarEvent ev : activeEvents) if (!after.contains(ev.name())) events.publish(new WorldScheduleEvent(ev.name(), false, now.name(), day));
        this.period = now;
        this.activeEvents = active;
    }

    private void register(Set<UUID> present, InputSource source, long now) {
        for (UUID id : present) {
            if (schedules.containsKey(id)) continue;
            Optional<Light> light = source.light(id);
            if (light.isEmpty()) continue;
            Lifestyle lifestyle = catalog.forType(light.get().typeId());
            long seed = id.getMostSignificantBits() ^ id.getLeastSignificantBits();
            var traits = lifestyle.baseline().individual(seed, settings.personalityJitter());
            int interval = TickBucket.FAR.interval(settings);
            NpcSchedule st = new NpcSchedule(id, light.get().typeId(), lifestyle, traits, lifestyle.shiftFor(seed), settingsSupplier, now - interval + CrowdManager.phaseOf(id, interval));
            st.light = light.get();
            schedules.put(id, st);
        }
    }

    private void forgetMissing(Set<UUID> present) {
        for (UUID id : new ArrayList<>(schedules.keySet())) {
            if (present.contains(id)) continue;
            forget(id);
        }
    }

    /** Drops everything the scheduler knows about an NPC (it was removed from the world). */
    public void forget(UUID id) {
        NpcSchedule st = schedules.remove(id);
        zones.release(id);
        groups.leave(id);
        if (st != null && st.current != null) st.current.cancel();
    }

    private void refreshBucket(NpcSchedule st, InputSource source, long now) {
        if (now - st.bucketTick < BUCKET_REFRESH_TICKS && !st.forced) return;
        st.bucketTick = now;
        source.light(st.id).ifPresent(l -> st.light = l);
        Light l = st.light;
        double distance = l == null ? Double.MAX_VALUE : l.playerDistance();
        RoutineInstance cur = st.current;
        boolean asleep = cur != null && cur.routine() == RoutineType.SLEEP && cur.state() == RoutineInstance.State.ACTIVE;
        boolean urgent = st.advice != null && (st.advice.emergency() || st.advice.response() != null);
        boolean zoneActive = cur != null && cur.place() != null && cur.place().zoneId() != null && zones.alerted(cur.place().zoneId(), now);
        TickBucket bucket = optimizer.classify(distance, asleep, urgent, zoneActive);
        st.bucket = bucket;
    }

    private void announceOptimization(double scale, int deferred, long now) {
        boolean scaleChanged = Math.abs(scale - lastCrowdScale) > 0.05D;
        boolean cooled = now - lastOptimizationEvent >= 200;
        if ((scaleChanged || deferred > 0) && cooled) {
            lastOptimizationEvent = now;
            events.publish(new SchedulerOptimizationEvent(scaleChanged ? "crowd scale" : "deferred evaluations", schedules.size(), deferred, scale));
        }
        lastCrowdScale = scale;
    }

    private void housekeeping(long now) {
        if (now - lastGroupSync >= settings.groupSyncTicks()) {
            lastGroupSync = now;
            List<MemberSnapshot> snapshots = snapshots();
            if (groupSyncs++ % AUTO_GROUP_EVERY == 0) groups.autoGroup(snapshots, now);
            groups.sync(snapshots, now);
        }
        if (now - lastZoneScan >= settings.zoneScanTicks()) {
            lastZoneScan = now;
            zones.prune(now, schedules.keySet());
            for (NpcSchedule st : schedules.values()) st.cooldowns.prune(now);
        }
    }

    private List<MemberSnapshot> snapshots() {
        List<MemberSnapshot> out = new ArrayList<>();
        for (NpcSchedule st : schedules.values()) {
            Light l = st.light;
            if (l == null) continue;
            RoutineInstance cur = st.current;
            boolean emergency = st.advice != null && st.advice.emergency();
            out.add(new MemberSnapshot(st.id, l.dimension(), l.x(), l.y(), l.z(), st.traits, st.lifestyle.groupType(), cur == null ? null : cur.routine(),
                    cur == null ? null : cur.place(), cur != null && cur.state() == RoutineInstance.State.TRAVELLING, emergency));
        }
        return out;
    }

    private void publishPopulation(double scale) {
        Map<TickBucket, Integer> distribution = new EnumMap<>(TickBucket.class);
        for (NpcSchedule st : schedules.values()) distribution.merge(st.bucket, 1, Integer::sum);
        metrics.population(schedules.size(), groups.groups().size(), zoneRegistry.size(), scale, distribution);
    }

    // ------------------------------------------------------------------ interaction from outside

    /** Something happened to this NPC (it was hurt, a call reached it): evaluate it on the next tick whatever its bucket. */
    public void disturb(UUID id) { NpcSchedule st = schedules.get(id); if (st != null) st.force(); }

    /** The Brain reports that a routine's place could not be reached; the routine is abandoned and put on cooldown. */
    public void unreachable(UUID id) {
        NpcSchedule st = schedules.get(id);
        if (st == null || st.current == null || st.current.routine() == null) return;
        st.cooldowns.start(yadi.samuraiai.ai.scheduler.routine.RoutinePlanner.cooldownKey(st.current.routine()), tick, settings.routineCooldownTicks());
        st.current.cancel();
        zones.release(id);
        st.current = null;
        st.force();
    }

    /** An experience shaped this NPC's character. */
    public void experience(UUID id, DriftCause cause, double magnitude) {
        NpcSchedule st = schedules.get(id);
        if (st != null) npcScheduler.applyDrift(st, cause, magnitude, this);
    }

    /** Forgets every NPC and group (the world session ended); zones stay because they describe the world. */
    public void reset() {
        schedules.clear();
        groups.clear();
        period = null;
        activeEvents = List.of();
        lastGroupSync = Long.MIN_VALUE / 2;
        lastZoneScan = Long.MIN_VALUE / 2;
        for (String zone : zoneRegistry.all().stream().map(z -> z.id()).toList()) zones.alert(zone, 0);
        metrics.reset();
    }

    // ------------------------------------------------------------------ outside opinions

    /** Registers (or replaces) a routine bias source under an id; the living world uses "living". */
    public void registerBiasSource(String id, RoutineBiasSource source) { biasSources.put(Objects.requireNonNull(id), Objects.requireNonNull(source)); }
    public void unregisterBiasSource(String id) { biasSources.remove(id); }
    public java.util.Set<String> biasSources() { return java.util.Set.copyOf(biasSources.keySet()); }

    /** Sums every source, each routine clamped to +-{@value #MAX_EXTERNAL_BIAS}; a failing source is skipped and counted as a scheduler failure. */
    @Override public Map<yadi.samuraiai.ai.scheduler.routine.RoutineType, Double> externalBias(UUID npc) {
        if (biasSources.isEmpty()) return Map.of();
        Map<yadi.samuraiai.ai.scheduler.routine.RoutineType, Double> sum = new java.util.EnumMap<>(yadi.samuraiai.ai.scheduler.routine.RoutineType.class);
        for (RoutineBiasSource source : biasSources.values()) {
            Map<yadi.samuraiai.ai.scheduler.routine.RoutineType, Double> part;
            try { part = source.bias(npc); } catch (RuntimeException error) { metrics.failure(error); continue; }
            if (part != null) part.forEach((r, v) -> { if (r != null && v != null && Double.isFinite(v)) sum.merge(r, v, Double::sum); });
        }
        sum.replaceAll((r, v) -> Math.max(-MAX_EXTERNAL_BIAS, Math.min(MAX_EXTERNAL_BIAS, v)));
        return sum;
    }

    // ------------------------------------------------------------------ NpcScheduler.Environment

    @Override public DayPeriod period(long worldTime) { return timeline.periodAt(worldTime); }
    @Override public List<CalendarEvent> activeEvents() { return activeEvents; }
    @Override public PlaceResolver places() { return places; }
    @Override public ZoneScheduler zones() { return zones; }
    @Override public Optional<GroupOrder> groupOrder(UUID npc) { return groups.orderFor(npc); }
    @Override public Optional<Alarm> alarm(UUID npc, String dimension, double x, double z, long now) { return groups.alarmFor(npc, dimension, x, z, now); }
    @Override public GroupRole role(UUID npc) { return groups.roleOf(npc); }
    @Override public String groupId(UUID npc) { return groups.groupOf(npc).map(g -> g.id()).orElse(null); }
    @Override public EventSink events() { return events; }

    @Override public void raiseAlarm(UUID npc, EventResponsePlanner.AlarmRequest request, String dimension, long now) {
        Optional<Alarm> existing = groups.groupOf(npc).map(g -> g.alarm());
        if (existing.isPresent() && existing.get().source().equals(npc) && existing.get().level() >= request.level() && now - existing.get().tick() < settings.groupSyncTicks()) return;
        Alarm alarm = new Alarm(groupId(npc), npc, dimension, request.x(), request.y(), request.z(), request.level(), now);
        groups.raiseAlarm(alarm, now);
        metrics.alarm();
        for (var zone : zoneRegistry.containing(dimension, request.x(), request.z())) zones.alert(zone.id(), now + settings.groupSyncTicks() * 12L);
        groups.groupOf(npc).ifPresent(g -> g.members().keySet().forEach(this::disturb));
    }

    @Override public List<double[]> neighbours(UUID npc, String dimension, double x, double z, double radius) {
        List<double[]> out = new ArrayList<>();
        for (NpcSchedule other : schedules.values()) {
            if (other.id.equals(npc) || other.light == null || !other.light.dimension().equals(dimension)) continue;
            if (Math.hypot(other.light.x() - x, other.light.z() - z) <= radius) out.add(new double[]{other.light.x(), other.light.z()});
        }
        return out;
    }
}
