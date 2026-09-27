package yadi.samuraiai.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import yadi.samuraiai.ai.navigation.world.NavigationService;
import yadi.samuraiai.ai.perception.hearing.SoundCategory;
import yadi.samuraiai.ai.perception.world.PerceptionService;
import yadi.samuraiai.ai.scheduler.engine.SchedulerAdvice;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.events.GroupLeaderChangedEvent;
import yadi.samuraiai.ai.scheduler.events.RoutineInterruptedEvent;
import yadi.samuraiai.ai.scheduler.events.RoutineStartedEvent;
import yadi.samuraiai.ai.scheduler.events.TimelineChangedEvent;
import yadi.samuraiai.ai.scheduler.personality.ResponseKind;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.world.SchedulerService;
import yadi.samuraiai.ai.scheduler.zone.Zone;
import yadi.samuraiai.ai.scheduler.zone.ZoneKind;
import yadi.samuraiai.emotion.Emotion;
import yadi.samuraiai.emotion.EmotionService;
import yadi.samuraiai.event.EventSubscription;
import yadi.samuraiai.event.NPCEventBus;
import yadi.samuraiai.goal.GoalType;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.npc.NPCTypeId;
import yadi.samuraiai.spawn.NPCSpawnRequest;
import yadi.samuraiai.spawn.NPCSpawnService;
import yadi.samuraiai.world.SpawnLocation;

/**
 * The behavior scheduler in a real server: the world clock decides what an NPC wants to be doing, the Brain turns that advice
 * into goals and tasks, Navigation walks the NPC to the place, and perception feeds back what it hears and feels. Each test
 * owns a patch of world far from the others, freezes the daylight cycle so that the hour is what the test says it is, and
 * restores the configuration afterwards. With CustomNPCs the NPCs really walk; without it the advice and the goals are checked.
 */
@GameTestHolder("samuraiai")
@PrefixGameTestTemplate(false)
public final class SchedulerGameTests {
    private static final int FLOOR = 1;
    private static SchedulerSettings previous;
    /** The world's clock before a test froze it, so that the next test (perception's sees hostiles at night) finds the daytime it expects. */
    private static Long originalDayTime;
    /** Whether the daylight cycle was running (a test server freezes it), restored exactly: leaving it running lets night fall mid-run. */
    private static Boolean originalDaylight;

    private SchedulerGameTests() { }

    private static void check(boolean condition, String message) { if (!condition) throw new GameTestAssertException(message); }

    // ------------------------------------------------------------------ the day follows the clock

    @GameTest(template = "empty", batch = "sched_day", timeoutTicks = 2400)
    public static void theClockShapesTheDayOfAMerchant(GameTestHelper helper) {
        GameTestIsolation.withScheduler();
        configure(b -> b.set("personalityJitter", 0.0D).set("minRoutineTicks", 100).set("groupsEnabled", false));
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0)).offset(10000, 0, 0);
        ServerLevel level = helper.getLevel();
        arena(level, origin, 44, 18);
        freezeTime(level, 3000);
        String dimension = level.dimension().location().toString();
        var zones = SchedulerService.getInstance().scheduler().zoneRegistry();
        zones.add(new Zone("gt_stalls", dimension, ZoneKind.MARKET, origin.getX() + 26.5D, origin.getY() + 1, origin.getZ() + 8.5D, 3, 4, null, null, null));
        NPCRuntime npc = spawn(level, origin, "merchant", "sched_day_npc", 4, 8);
        boolean physical = NPCSpawnService.getInstance().getController().requiresPhysicalBody();
        double homeX = origin.getX() + 4.5D;
        cleanupAt(helper, 2300, level, List.of("gt_stalls"), npc);
        trace(helper, "day", npc);
        boolean[] traded = {false};
        helper.succeedWhen(() -> {
            var advice = SchedulerService.getInstance().adviceFor(npc.getId());
            if (!traded[0]) {
                check(advice.isPresent() && advice.get().routine() == RoutineType.MERCHANT, "a market morning should put the merchant to trade: " + advice + " " + why(npc));
                check("gt_stalls".equals(advice.get().place().zoneId()), "at the stalls: " + advice.get().place());
                check(npc.getCurrentGoal() != null && npc.getCurrentGoal().getType() == GoalType.TRADE, "the brain follows: goal " + npc.getCurrentGoal());
                if (physical) check(distanceTo(npc, origin.getX() + 26.5D, origin.getZ() + 8.5D) <= 6.0D, "not at the stalls yet: " + describe(npc));
                traded[0] = true;
                freezeTime(level, 19000);
                System.out.println("SCHED_DAY trade ok at " + describe(npc));
            }
            check(advice.isPresent() && advice.get().routine() == RoutineType.SLEEP, "the dead of night should put the merchant to sleep: " + advice + " " + why(npc));
            check(npc.getCurrentGoal() != null && npc.getCurrentGoal().getType() == GoalType.SLEEP, "the brain follows into sleep: " + npc.getCurrentGoal());
            if (physical) check(distanceTo(npc, homeX, origin.getZ() + 8.5D) <= 6.0D, "not home yet: " + describe(npc));
            finish(helper, level, List.of("gt_stalls"), npc);
            System.out.println("PHASE24_SCHED_DAY_OK physical=" + physical);
        });
    }

    // ------------------------------------------------------------------ patrol -> sound -> investigate -> back to patrol

    @GameTest(template = "empty", batch = "sched_patrol", timeoutTicks = 2400)
    public static void aPatrolBrokenBySoundInvestigatesAndReturns(GameTestHelper helper) {
        GameTestIsolation.withScheduler();
        configure(b -> b.set("personalityJitter", 0.0D).set("minRoutineTicks", 100).set("groupsEnabled", false).set("responseHoldTicks", 60));
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0)).offset(12000, 0, 0);
        ServerLevel level = helper.getLevel();
        arena(level, origin, 36, 14);
        freezeTime(level, 7000);
        NPCRuntime npc = spawn(level, origin, "samurai", "sched_patrol_npc", 3, 5);
        boolean physical = NPCSpawnService.getInstance().getController().requiresPhysicalBody();
        double startX = origin.getX() + 3.5D;
        List<RoutineInterruptedEvent> interruptions = new ArrayList<>();
        List<RoutineStartedEvent> starts = new ArrayList<>();
        EventSubscription a = NPCEventBus.getInstance().subscribe(RoutineInterruptedEvent.class, e -> { if (e.npcId().equals(npc.getId())) interruptions.add(e); });
        EventSubscription b = NPCEventBus.getInstance().subscribe(RoutineStartedEvent.class, e -> { if (e.npcId().equals(npc.getId())) starts.add(e); });
        cleanupAt(helper, 2300, level, List.of(), npc);
        trace(helper, "patrol", npc);
        boolean[] emitted = {false}, investigated = {false};
        int[] noise = {0};
        helper.succeedWhen(() -> {
            var advice = SchedulerService.getInstance().adviceFor(npc.getId());
            if (!emitted[0]) {
                check(advice.isPresent() && advice.get().routine() == RoutineType.PATROL, "an afternoon patrol first: " + advice + " " + why(npc));
                emitted[0] = true;
            }
            // A noise is only heard if the NPC's ears happen to be scanning while it lasts, so it is repeated until it has been heard
            // (a quiet, harmless sound: repeated explosions would rightly be treated as danger).
            if (!investigated[0] && ++noise[0] % 15 == 1)
                PerceptionService.getInstance().sound(level, SoundCategory.BLOCK_BREAK, null, origin.getX() + 14.5D, origin.getY() + 1, origin.getZ() + 5.5D, 1.0D);
            if (!investigated[0]) {
                check(advice.isPresent() && advice.get().response() == ResponseKind.INVESTIGATE, "the sound should send it to look: " + advice + " threat=" + PerceptionService.getInstance().snapshot(npc.getId()).map(s -> s.threatLevel().name()).orElse("-"));
                check(advice.get().interrupted() >= 1, "the patrol waits on the interrupt stack: " + advice);
                check(npc.getCurrentGoal() != null && npc.getCurrentGoal().getType() == GoalType.INVESTIGATE, "the brain follows: goal " + npc.getCurrentGoal());
                if (physical) check(distanceTo(npc, startX, origin.getZ() + 5.5D) >= 6.0D, "it should be walking towards the sound: " + describe(npc));
                investigated[0] = true;
                System.out.println("SCHED_PATROL investigating " + describe(npc));
            }
            check(advice.isPresent() && advice.get().routine() == RoutineType.PATROL && advice.get().response() == null, "and then back to its patrol: " + advice + " " + why(npc));
            check(interruptions.stream().anyMatch(e -> e.routine().equals("PATROL") && e.by().equals("INVESTIGATE") && e.policy().equals("PAUSE")), "the patrol was interrupted with PAUSE: " + interruptions);
            check(starts.stream().anyMatch(e -> e.reason().startsWith("resumed after INVESTIGATE")), "and resumed through the stack: " + starts.stream().map(RoutineStartedEvent::reason).toList());
            a.close(); b.close();
            finish(helper, level, List.of(), npc);
            System.out.println("PHASE24_SCHED_PATROL_OK physical=" + physical);
        });
    }

    // ------------------------------------------------------------------ emergency

    @GameTest(template = "empty", batch = "sched_emergency", timeoutTicks = 1500)
    public static void fearOverridesTheNightsSleep(GameTestHelper helper) {
        GameTestIsolation.withScheduler();
        configure(b -> b.set("personalityJitter", 0.0D).set("minRoutineTicks", 100).set("groupsEnabled", false));
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0)).offset(14000, 0, 0);
        ServerLevel level = helper.getLevel();
        arena(level, origin, 30, 14);
        freezeTime(level, 19000);
        NPCRuntime npc = spawn(level, origin, "merchant", "sched_fear_npc", 4, 5);
        List<RoutineInterruptedEvent> interruptions = new ArrayList<>();
        EventSubscription a = NPCEventBus.getInstance().subscribe(RoutineInterruptedEvent.class, e -> { if (e.npcId().equals(npc.getId())) interruptions.add(e); });
        cleanupAt(helper, 1400, level, List.of(), npc);
        boolean[] frightened = {false};
        helper.succeedWhen(() -> {
            var advice = SchedulerService.getInstance().adviceFor(npc.getId());
            if (!frightened[0]) {
                check(advice.isPresent() && advice.get().routine() == RoutineType.SLEEP, "asleep at night first: " + advice + " " + why(npc));
                EmotionService.getInstance().adjust(npc, Emotion.FEAR, 95);
                EmotionService.getInstance().adjust(npc, Emotion.ANXIETY, 60);
                frightened[0] = true;
            }
            check(advice.isPresent() && advice.get().emergency() && advice.get().response() == ResponseKind.FLEE, "panic is an emergency and a civilian runs: " + advice);
            check(npc.getCurrentGoal() != null && npc.getCurrentGoal().getType() == GoalType.FLEE, "the brain runs: goal " + npc.getCurrentGoal());
            check(interruptions.stream().anyMatch(e -> e.routine().equals("SLEEP") && e.policy().equals("SUSPEND")), "sleep was suspended, not lost: " + interruptions);
            a.close();
            finish(helper, level, List.of(), npc);
            System.out.println("PHASE24_SCHED_EMERGENCY_OK");
        });
    }

    // ------------------------------------------------------------------ groups and formation

    @GameTest(template = "empty", batch = "sched_group", timeoutTicks = 2400)
    public static void guardsPatrolAsAGroupAndReelectALeader(GameTestHelper helper) {
        GameTestIsolation.withScheduler();
        configure(b -> b.set("personalityJitter", 0.0D).set("minRoutineTicks", 100).set("responseHoldTicks", 60).set("groupSyncTicks", 20).set("lifestyles", List.of(
                "watch;types=*;group=PATROL;traits=discipline:70,loyalty:70,courage:60,curiosity:50;MORNING=PATROL:70;AFTERNOON=PATROL:70;EVENING=PATROL:70;NIGHT=PATROL:70;LATE_NIGHT=PATROL:70;DAWN=PATROL:70")));
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0)).offset(16000, 0, 0);
        ServerLevel level = helper.getLevel();
        arena(level, origin, 40, 30);
        freezeTime(level, 7000);
        NPCRuntime g1 = spawn(level, origin, "guard", "sched_g1", 10, 12);
        NPCRuntime g2 = spawn(level, origin, "guard", "sched_g2", 12, 12);
        NPCRuntime g3 = spawn(level, origin, "guard", "sched_g3", 11, 14);
        boolean physical = NPCSpawnService.getInstance().getController().requiresPhysicalBody();
        List<GroupLeaderChangedEvent> changes = new ArrayList<>();
        EventSubscription sub = NPCEventBus.getInstance().subscribe(GroupLeaderChangedEvent.class, changes::add);
        cleanupAt(helper, 2300, level, List.of(), g1, g2, g3);
        boolean[] formed = {false};
        NPCRuntime[] leader = {null};
        helper.succeedWhen(() -> {
            var scheduler = SchedulerService.getInstance().scheduler();
            if (!formed[0]) {
                // Other tests (and the cognitive ones) may have NPCs of their own running beside these guards, so only this test's group is examined.
                var mine = scheduler.groupCoordinator().groupOf(g1.getId());
                check(mine.isPresent() && mine.get().size() == 3 && scheduler.groupCoordinator().groupOf(g2.getId()).equals(mine) && scheduler.groupCoordinator().groupOf(g3.getId()).equals(mine), "three guards form one group: " + scheduler.groupCoordinator().groups().stream().map(g -> g.id() + ":" + g.size()).toList());
                var group = mine.get();
                check(group.leader() != null, "a leader was elected");
                int followersInFormation = 0;
                for (NPCRuntime g : List.of(g1, g2, g3)) {
                    var advice = SchedulerService.getInstance().adviceFor(g.getId());
                    check(advice.isPresent() && advice.get().groupId() != null && advice.get().routine() == RoutineType.PATROL, "every guard patrols with its group: " + advice);
                    if (!g.getId().equals(group.leader())) { check(advice.get().formation() != null, "followers are given a formation slot: " + advice); followersInFormation++; }
                }
                check(followersInFormation == 2, "two followers");
                if (physical) {
                    NPCRuntime lead = NPCManager.getInstance().find(group.leader()).orElseThrow();
                    for (NPCRuntime g : List.of(g1, g2, g3)) {
                        var l = lead.getInstance().getLocation();
                        check(distanceTo(g, l.x(), l.z()) <= 14.0D, "followers stay with their leader: " + describe(g) + " leader " + describe(lead));
                    }
                }
                leader[0] = NPCManager.getInstance().find(group.leader()).orElseThrow();
                formed[0] = true;
                System.out.println("SCHED_GROUP formed leader=" + leader[0].getName());
                NPCSpawnService.getInstance().remove(leader[0].getId(), "gametest leader leaves", true);
            }
            check(changes.size() >= 2, "a new leader took over: " + changes.size() + " election(s)");
            NPCRuntime survivor = List.of(g1, g2, g3).stream().filter(x -> !x.getId().equals(leader[0].getId())).findFirst().orElseThrow();
            var group = scheduler.groupCoordinator().groupOf(survivor.getId()).orElseThrow(() -> new GameTestAssertException("the survivors still have a group"));
            check(group.size() == 2 && group.leader() != null && !group.leader().equals(leader[0].getId()), "the group carries on under someone else: size " + group.size());
            sub.close();
            finish(helper, level, List.of(), g1, g2, g3);
            System.out.println("PHASE24_SCHED_GROUP_OK physical=" + physical + " elections=" + changes.size());
        });
    }

    // ------------------------------------------------------------------ timeline and commands

    @GameTest(template = "empty", batch = "sched_commands", timeoutTicks = 900)
    public static void theTimelineAnnouncesPeriodsAndCommandsRespond(GameTestHelper helper) {
        GameTestIsolation.withScheduler();
        configure(b -> b.set("personalityJitter", 0.0D).set("groupsEnabled", false));
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0)).offset(18000, 0, 0);
        ServerLevel level = helper.getLevel();
        arena(level, origin, 20, 12);
        freezeTime(level, 5990);
        NPCRuntime npc = spawn(level, origin, "merchant", "sched_cmd_npc", 4, 4);
        List<TimelineChangedEvent> changes = new ArrayList<>();
        EventSubscription sub = NPCEventBus.getInstance().subscribe(TimelineChangedEvent.class, changes::add);
        var commands = level.getServer().getCommands();
        var source = level.getServer().createCommandSourceStack().withPermission(4);
        cleanupAt(helper, 850, level, List.of("gt_cmd_zone"), npc);
        helper.runAfterDelay(30, () -> level.setDayTime(6100));
        helper.runAfterDelay(120, () -> {
            try {
                check(changes.stream().anyMatch(e -> e.from().equals("MORNING") && e.to().equals("AFTERNOON")), "the timeline announced the afternoon: " + changes);
                check(commands.performPrefixedCommand(source, "samuraiai scheduler status") > 0, "scheduler status");
                check(commands.performPrefixedCommand(source, "samuraiai scheduler inspect sched_cmd_npc") > 0, "scheduler inspect");
                check(commands.performPrefixedCommand(source, "samuraiai scheduler zone add gt_cmd_zone MARKET 5 3") > 0, "zone add");
                check(SchedulerService.getInstance().scheduler().zoneRegistry().get("gt_cmd_zone").isPresent(), "the zone exists");
                check(commands.performPrefixedCommand(source, "samuraiai scheduler zones") > 0, "zones");
                check(commands.performPrefixedCommand(source, "samuraiai scheduler groups") > 0, "groups");
                check(commands.performPrefixedCommand(source, "samuraiai scheduler zone remove gt_cmd_zone") > 0, "zone remove");
                check(SchedulerService.getInstance().scheduler().zoneRegistry().get("gt_cmd_zone").isEmpty(), "the zone is gone");
            } finally {
                sub.close();
                finish(helper, level, List.of("gt_cmd_zone"), npc);
            }
            System.out.println("PHASE24_SCHED_COMMANDS_OK");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ helpers

    private static void configure(UnaryOperator<SchedulerSettings.Builder> change) {
        if (previous == null) previous = SchedulerSettings.current();
        // A test world has no player, so by distance every NPC would be hibernating (evaluated every 30 s): evaluate everyone briskly.
        SchedulerSettings.Builder base = previous.toBuilder().set("nearbyInterval", 10).set("zoneActiveInterval", 10).set("farInterval", 10)
                .set("sleepingInterval", 10).set("hibernatingInterval", 10);
        SchedulerSettings.apply(change.apply(base).build());
    }

    private static void freezeTime(ServerLevel level, long dayTime) {
        if (originalDayTime == null) originalDayTime = level.getDayTime();
        if (originalDaylight == null) originalDaylight = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.setDayTime(dayTime);
    }

    /** Puts the world and the configuration back as they were and removes the test's NPCs and zones. Safe to call twice. */
    private static void finish(GameTestHelper helper, ServerLevel level, List<String> zones, NPCRuntime... npcs) {
        for (NPCRuntime npc : npcs) if (NPCManager.getInstance().find(npc.getId()).isPresent()) NPCSpawnService.getInstance().remove(npc.getId(), "gametest", true);
        for (String zone : zones) SchedulerService.getInstance().scheduler().zoneRegistry().remove(zone);
        if (originalDaylight != null) { level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(originalDaylight, level.getServer()); originalDaylight = null; }
        if (originalDayTime != null) { level.setDayTime(originalDayTime); originalDayTime = null; }
        if (previous != null) { SchedulerSettings.apply(previous); previous = null; }
    }

    private static void cleanupAt(GameTestHelper helper, long tick, ServerLevel level, List<String> zones, NPCRuntime... npcs) {
        helper.runAfterDelay(tick, () -> finish(helper, level, zones, npcs));
    }

    private static double distanceTo(NPCRuntime npc, double x, double z) {
        npc.getController().synchronize(npc.getInstance());
        var at = npc.getInstance().getLocation();
        return at == null ? Double.MAX_VALUE : Math.hypot(at.x() - x, at.z() - z);
    }

    /** Everything worth reading when a check fails: is the scheduler on, what does it think, what does the NPC perceive and choose. */
    private static String why(NPCRuntime npc) {
        var service = SchedulerService.getInstance();
        String threat = PerceptionService.getInstance().snapshot(npc.getId()).map(x -> x.threatLevel() + "/" + x.awareness() + " inv=" + x.investigationTarget().isPresent()).orElse("no snapshot");
        String inspected = service.scheduler().schedule(npc.getId()).map(st -> String.join(" | ", yadi.samuraiai.ai.scheduler.debug.SchedulerInspector.inspect(st, service.currentTick()))).orElse("no schedule");
        return "[enabled=" + service.enabled() + " goal=" + npc.getCurrentGoal() + " perceives " + threat + " :: " + inspected + "]";
    }

    /** Prints what the scheduler thinks every 100 ticks (only when tracing is on), so a failure can be read from the log. */
    private static void trace(GameTestHelper helper, String label, NPCRuntime npc) {
        if (!Boolean.getBoolean("samuraiai.navTrace")) return;
        long[] n = {0};
        helper.onEachTick(() -> { if (++n[0] % 100 == 0 && NPCManager.getInstance().find(npc.getId()).isPresent()) System.out.println("SCHED_TRACE " + label + " t=" + n[0] + " " + describe(npc) + " " + why(npc)); });
    }

    private static String describe(NPCRuntime npc) {
        var at = npc.getInstance().getLocation();
        return npc.getName() + "@" + (at == null ? "?" : String.format("%.1f,%.1f", at.x(), at.z()));
    }

    /** A flat stone floor with air above, its chunks kept loaded, and navigation told its cached terrain here is stale. */
    private static void arena(ServerLevel level, BlockPos origin, int width, int depth) {
        TestTerrain.flatten(level, origin, width, depth);
        for (int cx = (origin.getX() - 10) >> 4; cx <= (origin.getX() + width + 10) >> 4; cx++)
            for (int cz = (origin.getZ() - 10) >> 4; cz <= (origin.getZ() + depth + 10) >> 4; cz++) NavigationService.getInstance().chunkChanged(level, cx, cz);
    }

    private static NPCRuntime spawn(ServerLevel level, BlockPos origin, String type, String name, int dx, int dz) {
        var result = NPCSpawnService.getInstance().spawn(new NPCSpawnRequest(NPCTypeId.of(type), name,
                new SpawnLocation(level.dimension().location().toString(), origin.getX() + dx + 0.5D, origin.getY() + 1, origin.getZ() + dz + 0.5D, 0.0F)));
        if (!result.success()) throw new AssertionError("spawn failed: " + result.message());
        return NPCManager.getInstance().find(result.instance().getIdentity().id()).orElseThrow();
    }
}
