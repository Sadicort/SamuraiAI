package yadi.samuraiai.ai.scheduler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.scheduler.emotion.EmotionInput;
import yadi.samuraiai.ai.scheduler.emotion.Mood;
import yadi.samuraiai.ai.scheduler.engine.Investigation;
import yadi.samuraiai.ai.scheduler.engine.Perceived;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.events.*;
import yadi.samuraiai.ai.scheduler.group.GroupRole;
import yadi.samuraiai.ai.scheduler.personality.DriftCause;
import yadi.samuraiai.ai.scheduler.personality.ResponseKind;
import yadi.samuraiai.ai.scheduler.routine.RoutineInstance;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.testkit.SchedulerHarness;
import yadi.samuraiai.ai.scheduler.time.DayPeriod;
import yadi.samuraiai.ai.scheduler.zone.Zone;
import yadi.samuraiai.ai.scheduler.zone.ZoneKind;

/**
 * The scheduler running as a whole in a simulated village: a stand-in for the Brain and Navigation walks each NPC to the
 * place its advice names, so the loop advice, movement, arrival and next advice is exercised without Minecraft.
 */
class SchedulerRuntimeTest {
    private static SchedulerSettings steady() { return SchedulerSettings.builder().set("personalityJitter", 0.0D).build(); }

    private static Perceived heard(double x, double z) { return new Perceived(0, 0, 2, 30, true, new Investigation(x, 64, z, 2.0D, 60.0D), false, Double.NaN, Double.NaN, Double.NaN); }
    private static Perceived threat(int level, boolean damaged) { return new Perceived(level, 80, 5, 90, true, null, damaged, 10, 64, 0); }

    // ------------------------------------------------------------------ a day in the village

    @Test void aVillagerLivesAWholeDayFollowingTheTimeline() {
        var h = new SchedulerHarness(steady());
        var npc = h.add("villager", 0, 0);
        h.setWorldTime(0);
        Set<String> started = new HashSet<>();
        List<DayPeriod> sleepingPeriods = new ArrayList<>(), workingPeriods = new ArrayList<>();
        for (int i = 0; i < 24000; i += 50) {
            h.run(50);
            var a = h.advice(npc);
            if (a == null || !a.active()) continue;
            started.add(a.label());
            if (a.routine() == RoutineType.SLEEP && a.state() == RoutineInstance.State.ACTIVE) sleepingPeriods.add(a.period());
            if (a.routine() == RoutineType.WORK && a.state() == RoutineInstance.State.ACTIVE) workingPeriods.add(a.period());
        }
        assertTrue(started.contains("WORK") && started.contains("SLEEP") && started.contains("EAT"), "routines seen: " + started);
        assertTrue(sleepingPeriods.contains(DayPeriod.LATE_NIGHT) || sleepingPeriods.contains(DayPeriod.NIGHT), "slept at night: " + sleepingPeriods);
        assertTrue(workingPeriods.contains(DayPeriod.MORNING) || workingPeriods.contains(DayPeriod.AFTERNOON), "worked by day: " + workingPeriods);
        assertFalse(workingPeriods.contains(DayPeriod.LATE_NIGHT), "nobody works in the dead of night");
        assertTrue(h.events(TimelineChangedEvent.class).size() >= 5, "the timeline announced its periods");
        assertTrue(h.events(RoutineStartedEvent.class).size() < 40, "no flapping: " + h.events(RoutineStartedEvent.class).size() + " routines in a day");
        assertTrue(npc.walked > 5, "it actually went places (in the stand-in for navigation)");
        assertTrue(h.events(RoutineCompletedEvent.class).size() >= 2);
    }

    @Test void aWorkingDayWearsAnNpcDownSoItSeeksRestOnItsOwnAccount() {
        var h = new SchedulerHarness(SchedulerSettings.builder().set("personalityJitter", 0.0D)
                .set("lifestyles", List.of("grinder;types=*;group=VILLAGE;traits=diligence:70;MORNING=WORK:80;AFTERNOON=WORK:80;EVENING=WORK:80;NIGHT=WORK:80;LATE_NIGHT=WORK:80;DAWN=WORK:80")).build());
        var npc = h.add("anyone", 0, 0);
        h.setWorldTime(1000);
        h.run(20000);
        var personal = h.events(RoutineStartedEvent.class).stream().filter(e -> e.layer().equals("PERSONAL")).toList();
        assertFalse(personal.isEmpty(), "tiredness raised a personal need");
        assertTrue(personal.stream().anyMatch(e -> e.routine().equals("SLEEP") || e.routine().equals("REST") || e.routine().equals("EAT")), personal.toString());
        var interrupted = h.events(RoutineInterruptedEvent.class);
        assertTrue(interrupted.stream().anyMatch(e -> e.routine().equals("WORK")), "work was set aside for it");
        assertTrue(h.scheduler.schedule(npc.id).orElseThrow().energy().fatigue() < 100);
    }

    // ------------------------------------------------------------------ patrol -> sound -> investigate -> return

    @Test void aPatrolInterruptedBySoundInvestigatesItAndThenReturnsToThePatrol() {
        var h = new SchedulerHarness(steady());
        var samurai = h.add("samurai", 0, 0);
        h.canFight(samurai);
        h.setWorldTime(7000);
        h.run(60);
        assertEquals(RoutineType.PATROL, h.advice(samurai).routine(), "an afternoon patrol: " + h.advice(samurai));

        samurai.perceived = heard(40, 0);
        h.runUntil(() -> h.advice(samurai).response() == ResponseKind.INVESTIGATE, 200);
        var a = h.advice(samurai);
        assertEquals(ResponseKind.INVESTIGATE, a.response());
        assertEquals(40.0D, a.place().x(), 1e-6);
        assertEquals(1, a.interrupted(), "the patrol is waiting on the interrupt stack");
        var interruption = h.events(RoutineInterruptedEvent.class).stream().filter(e -> e.routine().equals("PATROL")).findFirst().orElseThrow();
        assertEquals("PAUSE", interruption.policy());
        assertEquals("INVESTIGATE", interruption.by());

        h.runUntil(() -> Math.hypot(samurai.x - 40, samurai.z) < 8, 600);
        assertTrue(samurai.x > 25, "it went to look");
        samurai.perceived = Perceived.CALM;
        h.runUntil(() -> h.advice(samurai).routine() == RoutineType.PATROL && h.advice(samurai).response() == null, 900);
        var back = h.advice(samurai);
        assertEquals(RoutineType.PATROL, back.routine(), "and then it went back to patrolling");
        assertEquals(0, back.interrupted());
        assertTrue(h.events(RoutineStartedEvent.class).stream().anyMatch(e -> e.reason().startsWith("resumed after INVESTIGATE")), "resumed through the stack, not planned afresh");
        assertTrue(h.events(RoutineCompletedEvent.class).stream().anyMatch(e -> e.routine().equals("INVESTIGATE") && e.outcome().equals("trigger gone")));
    }

    @Test void anNpcThatCannotWalkStillResumesItsPatrolOnceTheTriggerFades() {
        // A body-less NPC never arrives anywhere: its investigation only ends because perception stops reporting it.
        var h = new SchedulerHarness(SchedulerSettings.builder().set("personalityJitter", 0.0D).set("minRoutineTicks", 100).set("responseHoldTicks", 60).build());
        var samurai = h.add("samurai", 0, 0);
        samurai.speed = 0;
        h.canFight(samurai);
        h.setWorldTime(7000);
        h.run(60);
        assertEquals(RoutineType.PATROL, h.advice(samurai).routine());
        samurai.perceived = heard(30, 0);
        h.runUntil(() -> h.advice(samurai).response() == ResponseKind.INVESTIGATE, 200);
        h.run(400);
        assertEquals(ResponseKind.INVESTIGATE, h.advice(samurai).response(), "still looking while the sound is reported");
        samurai.perceived = Perceived.CALM;
        h.run(300);
        var after = h.advice(samurai);
        assertNotNull(after, "advice exists");
        assertEquals(RoutineType.PATROL, after.routine(), "back on patrol: " + after + " stack=" + h.scheduler.schedule(samurai.id).orElseThrow().stack().frames());
    }

    // ------------------------------------------------------------------ emergency

    @Test void anEmergencyOverridesEvenASleepingNpcAndTheSleepIsSuspendedNotLost() {
        var h = new SchedulerHarness(steady());
        var villager = h.add("villager", 0, 0);
        h.setWorldTime(19000);
        h.run(400);
        assertEquals(RoutineType.SLEEP, h.advice(villager).routine());
        assertEquals(RoutineInstance.State.ACTIVE, h.advice(villager).state());

        villager.perceived = threat(3, true);
        h.runUntil(() -> h.advice(villager).emergency(), 40);
        var a = h.advice(villager);
        assertTrue(a.emergency(), "within a few ticks: " + a);
        assertEquals(ResponseKind.FLEE, a.response(), "a villager cannot fight");
        assertNotNull(a.place());
        assertTrue(a.place().x() < 0.5D, "it runs away from the threat that stands at x=10: " + a.place());
        var interruption = h.events(RoutineInterruptedEvent.class).stream().filter(e -> e.routine().equals("SLEEP")).findFirst().orElseThrow();
        assertEquals("SUSPEND", interruption.policy());
        assertEquals(1, a.interrupted());

        villager.perceived = Perceived.CALM;
        h.runUntil(() -> h.advice(villager).routine() == RoutineType.SLEEP, 900);
        assertEquals(RoutineType.SLEEP, h.advice(villager).routine(), "back to sleep");
    }

    @Test void aFighterAnswersDangerByAssistingWhereACivilianFlees() {
        var h = new SchedulerHarness(steady());
        var samurai = h.add("samurai", 0, 0);
        var villager = h.add("villager", 3, 0);
        h.canFight(samurai);
        h.setWorldTime(7000);
        samurai.perceived = threat(3, false);
        villager.perceived = threat(3, false);
        h.run(60);
        assertEquals(ResponseKind.ASSIST, h.advice(samurai).response());
        assertEquals(ResponseKind.FLEE, h.advice(villager).response());
        assertTrue(h.advice(samurai).emergency() && h.advice(villager).emergency());
    }

    @Test void panicFromFearAloneIsAnEmergencyAndShowsInTheMood() {
        var h = new SchedulerHarness(steady());
        var npc = h.add("villager", 0, 0);
        h.setWorldTime(7000);
        h.run(60);
        npc.emotion = new EmotionInput(95, 0, 0, 0, 5, 70, 0);
        h.run(60);
        assertEquals(Mood.PANICKED, h.advice(npc).mood());
        assertTrue(h.events(EmotionPriorityChangedEvent.class).stream().anyMatch(e -> e.to().equals("PANICKED")));
        assertTrue(h.scheduler.schedule(npc.id).orElseThrow().energy().stress() > 0, "fear adds stress");
    }

    @Test void slowNpcsReactWithinTheirReactionDelayNotInstantlyButNotNever() {
        var h = new SchedulerHarness(steady());
        var npc = h.add("villager", 0, 0);
        h.setWorldTime(7000);
        h.run(60);
        npc.perceived = heard(15, 0);
        h.run(1);
        assertNull(h.advice(npc).response(), "not on the very tick");
        h.runUntil(() -> h.advice(npc).response() != null, 200);
        assertEquals(ResponseKind.INVESTIGATE, h.advice(npc).response());
    }

    // ------------------------------------------------------------------ groups

    private static SchedulerSettings watchmen() {
        return SchedulerSettings.builder().set("personalityJitter", 0.0D).set("lifestyles", List.of(
                "watch;types=*;group=PATROL;traits=discipline:70,loyalty:70,courage:60,curiosity:50;"
                        + "MORNING=PATROL:70;AFTERNOON=PATROL:70;EVENING=PATROL:70;NIGHT=PATROL:70;LATE_NIGHT=PATROL:70;DAWN=PATROL:70")).build();
    }

    @Test void guardsFormAGroupWithALeaderAndFollowInFormation() {
        var h = new SchedulerHarness(watchmen());
        var a = h.add("guard", 0, 0);
        var b = h.add("guard", 2, 0);
        var c = h.add("guard", 0, 2);
        var d = h.add("guard", 2, 2);
        h.setWorldTime(7000);
        h.run(1200);
        assertEquals(1, h.scheduler.groupCoordinator().groups().size());
        var group = h.scheduler.groupCoordinator().groups().get(0);
        assertEquals(4, group.size());
        assertNotNull(group.leader());
        assertFalse(h.events(GroupLeaderChangedEvent.class).isEmpty());
        long leaders = h.npcs.values().stream().filter(s -> h.advice(s) != null && h.advice(s).role() == GroupRole.LEADER).count();
        assertEquals(1, leaders);
        var leader = h.npcs.get(group.leader());
        for (var s : h.npcs.values()) {
            if (s == leader) continue;
            var adv = h.advice(s);
            assertEquals(group.id(), adv.groupId());
            assertNotNull(adv.formation(), "followers are given a formation slot");
            assertTrue(Math.hypot(s.x - leader.x, s.z - leader.z) < 4 * h.scheduler.settings().formationSpacing() + 12, "the follower stays with the leader");
        }
        for (var s : List.of(a, b, c, d)) assertNotNull(h.scheduler.groupCoordinator().roleOf(s.id));
    }

    @Test void whenTheLeaderIsRemovedAnotherTakesOverAndTheGroupKeepsPatrolling() {
        var h = new SchedulerHarness(watchmen());
        for (int i = 0; i < 4; i++) h.add("guard", i, 0);
        h.setWorldTime(7000);
        h.run(600);
        var group = h.scheduler.groupCoordinator().groups().get(0);
        var oldLeader = group.leader();
        h.remove(h.npcs.get(oldLeader));
        h.run(600);
        assertEquals(1, h.scheduler.groupCoordinator().groups().size());
        assertNotEquals(oldLeader, h.scheduler.groupCoordinator().groups().get(0).leader());
        assertEquals(2, h.events(GroupLeaderChangedEvent.class).size());
        assertEquals(3, h.scheduler.groupCoordinator().groups().get(0).size());
        assertTrue(h.scheduler.schedule(oldLeader).isEmpty(), "the departed NPC is forgotten");
    }

    @Test void oneGuardRaisingTheAlarmBringsTheOthersToHelpOrTheCivilianAway() {
        var h = new SchedulerHarness(watchmen());
        var a = h.add("guard", 0, 0);
        var b = h.add("guard", 3, 0);
        h.canFight(a);
        h.canFight(b);
        h.setWorldTime(7000);
        h.run(600);
        a.perceived = new Perceived(2, 70, 4, 80, true, null, false, 30, 64, 0);
        h.run(300);
        assertTrue(h.scheduler.metrics().snapshot().alarms() >= 1, "an alarm went out");
        assertNotNull(h.advice(b).response(), "the other guard reacted: " + h.advice(b));
        assertEquals(ResponseKind.ASSIST, h.advice(b).response());
    }

    // ------------------------------------------------------------------ calendar, zones, cooldowns, character

    @Test void theCalendarAnnouncesWhenAWorldEventStartsAndEnds() {
        var h = new SchedulerHarness(steady());
        h.add("merchant", 0, 0);
        h.setWorldTime(11490);
        h.run(40);
        var events = h.events(WorldScheduleEvent.class);
        assertTrue(events.stream().anyMatch(e -> e.name().equals("market_day") && e.started()), "began on the first tick it was active");
        assertTrue(events.stream().anyMatch(e -> e.name().equals("market_day") && !e.started()), "ended when the evening began");
        assertTrue(h.events(TimelineChangedEvent.class).stream().anyMatch(e -> e.from().equals("AFTERNOON") && e.to().equals("EVENING")));
    }

    @Test void aMarketDayPullsMerchantsToTheirStalls() {
        var h = new SchedulerHarness(steady());
        var merchant = h.add("merchant", 0, 0);
        h.scheduler.zoneRegistry().add(new Zone("stalls", SchedulerHarness.DIM, ZoneKind.MARKET, 30, 64, 0, 6, 5, null, null, null));
        h.setWorldTime(2000);
        h.run(1500);
        var a = h.advice(merchant);
        assertEquals(RoutineType.MERCHANT, a.routine());
        assertEquals("stalls", a.place().zoneId());
        assertTrue(Math.hypot(merchant.x - 30, merchant.z) < 8, "it walked to the market");
        assertEquals(1, h.scheduler.zoneRegistry().get("stalls").map(z -> h.scheduler.zoneOccupancy(z.id())).orElse(-1));
    }

    @Test void aZoneThatIsFullSendsTheNextNpcToAnotherOne() {
        var h = new SchedulerHarness(steady().toBuilder().set("groupsEnabled", false).build());
        var one = h.add("merchant", 0, 0);
        var two = h.add("merchant", 1, 0);
        h.scheduler.zoneRegistry().add(new Zone("stall-a", SchedulerHarness.DIM, ZoneKind.MARKET, 20, 64, 0, 4, 1, null, null, null));
        h.scheduler.zoneRegistry().add(new Zone("stall-b", SchedulerHarness.DIM, ZoneKind.MARKET, 60, 64, 0, 4, 1, null, null, null));
        h.setWorldTime(2000);
        h.run(600);
        Set<String> zones = new HashSet<>();
        zones.add(h.advice(one).place().zoneId());
        zones.add(h.advice(two).place().zoneId());
        assertEquals(Set.of("stall-a", "stall-b"), zones, "each stall holds one merchant: " + h.advice(one) + " / " + h.advice(two));
    }

    @Test void aRoutineThatCannotBeReachedIsAbandonedAndNotRetriedAtOnce() {
        var h = new SchedulerHarness(steady());
        var npc = h.add("villager", 0, 0);
        h.setWorldTime(2000);
        h.run(100);
        var first = h.advice(npc).routine();
        assertNotNull(first);
        h.scheduler.unreachable(npc.id);
        h.run(30);
        var after = h.advice(npc);
        assertNotEquals(first, after.routine(), "it tries something else");
        assertFalse(h.scheduler.schedule(npc.id).orElseThrow().cooldowns().ready("R:" + first, h.tick));
    }

    @Test void experienceChangesCharacterAndPublishesIt() {
        var h = new SchedulerHarness(steady());
        var npc = h.add("villager", 0, 0);
        h.run(20);
        double before = h.scheduler.schedule(npc.id).orElseThrow().traits().get(yadi.samuraiai.ai.scheduler.personality.Trait.CAUTION);
        h.scheduler.experience(npc.id, DriftCause.FRIGHTENED, 10.0D);
        double after = h.scheduler.schedule(npc.id).orElseThrow().traits().get(yadi.samuraiai.ai.scheduler.personality.Trait.CAUTION);
        assertTrue(after > before);
        assertEquals(2, h.events(PersonalityUpdatedEvent.class).size());
        assertEquals(1, h.scheduler.metrics().snapshot().personalityChanges() / 2);
    }

    @Test void differentKindsOfNpcLiveDifferentDays() {
        var h = new SchedulerHarness(steady());
        var monk = h.add("monk", 0, 0);
        var merchant = h.add("merchant", 200, 0);
        h.setWorldTime(0);
        h.run(1400);
        Set<RoutineType> monks = new HashSet<>(), merchants = new HashSet<>();
        for (int i = 0; i < 60; i++) { h.run(100); monks.add(h.advice(monk).routine()); merchants.add(h.advice(merchant).routine()); }
        assertTrue(monks.contains(RoutineType.MEDITATE) || monks.contains(RoutineType.PRAYER), monks.toString());
        assertTrue(merchants.contains(RoutineType.MERCHANT), merchants.toString());
    }

    // ------------------------------------------------------------------ optimisation

    @Test void farNpcsAreEvaluatedMuchLessOftenThanNearOnes() {
        var near = new SchedulerHarness(steady());
        var far = new SchedulerHarness(steady());
        for (int i = 0; i < 20; i++) { near.add("villager", i, 0).playerDistance = 10; far.add("villager", i, 0).playerDistance = 200; }
        near.run(1000);
        far.run(1000);
        long n = near.scheduler.metrics().snapshot().evaluations(), f = far.scheduler.metrics().snapshot().evaluations();
        assertTrue(n > f * 4, "near " + n + " vs far " + f);
        assertTrue(f > 0);
    }

    @Test void aCrowdNeverExceedsThePerTickQuotaAndEveryoneStillGetsAttention() {
        var h = new SchedulerHarness(steady());
        for (int i = 0; i < 400; i++) h.add("villager", i % 40, i / 40);
        h.run(400);
        var m = h.scheduler.metrics().snapshot();
        assertTrue(m.evaluations() <= 400L * h.scheduler.settings().maxEvaluationsPerTick(), "evaluations " + m.evaluations());
        assertTrue(m.deferred() > 0, "more was due than the quota allows");
        assertEquals(400, m.npcs());
        assertTrue(m.crowdScale() >= 1.0D);
        long neglected = h.npcs.values().stream().filter(s -> h.scheduler.schedule(s.id).orElseThrow().lastEvaluated() < h.tick - 250).count();
        assertEquals(0, neglected, "nobody waited far beyond its interval");
        assertTrue(h.events(SchedulerOptimizationEvent.class).size() >= 1);
    }

    @Test void sleepersFarFromPlayersAreLookedAfterRarely() {
        var h = new SchedulerHarness(steady());
        var npc = h.add("villager", 0, 0);
        npc.playerDistance = 150;
        h.setWorldTime(19000);
        h.run(1500);
        assertEquals(RoutineType.SLEEP, h.advice(npc).routine());
        assertEquals(yadi.samuraiai.ai.scheduler.optimize.TickBucket.SLEEPING, h.scheduler.schedule(npc.id).orElseThrow().bucket());
    }

    // ------------------------------------------------------------------ robustness and determinism

    @Test void theSameWorldProducesTheSameSchedule() {
        List<String> first = day(), second = day();
        assertEquals(first, second);
        assertFalse(first.isEmpty());
    }

    private List<String> day() {
        var h = new SchedulerHarness(SchedulerSettings.defaults());
        h.add("guard", 0, 0); h.add("guard", 4, 0); h.add("merchant", 80, 80); h.add("villager", -40, 20);
        h.setWorldTime(0);
        h.run(12000);
        List<String> out = new ArrayList<>();
        for (var e : h.events(RoutineStartedEvent.class)) out.add(e.npcId().getLeastSignificantBits() + ":" + e.routine() + ":" + e.layer());
        return out;
    }

    @Test void removedNpcsAreForgottenEverywhereAndAResetStartsClean() {
        var h = new SchedulerHarness(steady());
        var a = h.add("guard", 0, 0);
        h.add("guard", 3, 0);
        h.setWorldTime(7000);
        h.run(300);
        h.remove(a);
        h.run(30);
        assertTrue(h.scheduler.schedule(a.id).isEmpty());
        assertEquals(1, h.scheduler.tracked().size());
        h.scheduler.reset();
        assertTrue(h.scheduler.groupCoordinator().groups().isEmpty());
        assertEquals(0, h.scheduler.metrics().snapshot().routinesStarted());
    }

    @Test void aBrokenInputDoesNotStopTheOthers() {
        var h = new SchedulerHarness(steady());
        var bad = h.add("villager", 0, 0);
        var good = h.add("villager", 5, 0);
        bad.home = null;
        h.setWorldTime(7000);
        h.run(200);
        assertTrue(h.advice(good).active(), "the healthy NPC carries on");
        assertFalse(h.advice(bad).active(), "an NPC with nowhere to be gets no schedule, and no exception");
    }

    @Test void aFailingEvaluationIsCountedAndReportedAndDoesNotStopTheOthers() {
        var h = new SchedulerHarness(steady());
        var good = h.add("villager", 5, 0);
        var bad = h.add("villager", 0, 0);
        var poisoned = new yadi.samuraiai.ai.scheduler.engine.InputSource() {
            @Override public java.util.Collection<java.util.UUID> npcs() { return h.npcs(); }
            @Override public java.util.Optional<yadi.samuraiai.ai.scheduler.engine.Light> light(java.util.UUID id) { return h.light(id); }
            @Override public java.util.Optional<yadi.samuraiai.ai.scheduler.engine.SchedulerInput> input(java.util.UUID id, long tick, long worldTime) {
                if (id.equals(bad.id)) throw new IllegalStateException("simulated sensor failure");
                return h.input(id, tick, worldTime);
            }
        };
        h.setWorldTime(7000);
        for (int i = 0; i < 200; i++) { h.tick++; h.worldTime++; h.scheduler.tick(h.tick, h.worldTime, poisoned); }
        var m = h.scheduler.metrics().snapshot();
        assertTrue(m.failures() >= 1, "the failure was counted");
        assertTrue(h.scheduler.metrics().lastFailure().contains("simulated sensor failure"), h.scheduler.metrics().lastFailure());
        assertTrue(h.advice(good) != null && h.advice(good).active(), "the healthy NPC carries on");
        assertTrue(m.failures() < 200, "a broken NPC is not retried every tick: " + m.failures());
    }

    @Test void configurationChangesTakeEffectWithoutRestartingTheScheduler() {
        java.util.concurrent.atomic.AtomicReference<SchedulerSettings> live = new java.util.concurrent.atomic.AtomicReference<>(steady());
        List<yadi.samuraiai.event.NpcEvent> events = new ArrayList<>();
        var scheduler = new yadi.samuraiai.ai.scheduler.engine.BehaviorScheduler(live::get, events::add);
        var h = new SchedulerHarness(steady());
        assertEquals(24000, scheduler.settings().dayLength());
        live.set(steady().toBuilder().set("dayLength", 12000).set("morningStart", 500).set("afternoonStart", 3000).set("eveningStart", 5750).set("nightStart", 7000)
                .set("lateNightStart", 9000).set("dawnStart", 11250).build());
        scheduler.tick(1, 3500, h);
        assertEquals(12000, scheduler.settings().dayLength());
        assertEquals(DayPeriod.AFTERNOON, scheduler.currentPeriod());
    }
}
