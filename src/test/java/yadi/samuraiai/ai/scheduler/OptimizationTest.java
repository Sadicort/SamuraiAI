package yadi.samuraiai.ai.scheduler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.optimize.CrowdManager;
import yadi.samuraiai.ai.scheduler.optimize.OptimizationEngine;
import yadi.samuraiai.ai.scheduler.optimize.TickBucket;

class OptimizationTest {
    private final SchedulerSettings cfg = SchedulerSettings.defaults();
    private final OptimizationEngine engine = new OptimizationEngine(cfg);

    @Test void distanceUrgencyAndSleepPlaceAnNpcInABucket() {
        assertEquals(TickBucket.VISIBLE, engine.classify(10, false, false, false));
        assertEquals(TickBucket.NEARBY, engine.classify(50, false, false, false));
        assertEquals(TickBucket.ZONE_ACTIVE, engine.classify(100, false, false, false));
        assertEquals(TickBucket.FAR, engine.classify(200, false, false, false));
        assertEquals(TickBucket.HIBERNATING, engine.classify(1000, false, false, false));
        assertEquals(TickBucket.SLEEPING, engine.classify(100, true, false, false));
        assertEquals(TickBucket.HIBERNATING, engine.classify(1000, true, false, false));
        assertEquals(TickBucket.VISIBLE, engine.classify(1000, true, true, false), "urgency always wins");
        assertEquals(TickBucket.VISIBLE, engine.classify(5, true, false, false), "someone watching a sleeper still sees it");
        assertEquals(TickBucket.ZONE_ACTIVE, engine.classify(200, false, false, true), "an active zone keeps its people alive");
    }

    @Test void theFurtherTheBucketTheLongerTheIntervalAndCrowdsStretchAllButTheVisibleOne() {
        int last = 0;
        for (TickBucket b : new TickBucket[]{TickBucket.VISIBLE, TickBucket.NEARBY, TickBucket.ZONE_ACTIVE, TickBucket.FAR}) {
            assertTrue(engine.interval(b, 1.0D) >= last, b.toString());
            last = engine.interval(b, 1.0D);
        }
        assertEquals(cfg.visibleInterval(), engine.interval(TickBucket.VISIBLE, 3.0D));
        assertEquals(Math.round(cfg.farInterval() * 2.0D), engine.interval(TickBucket.FAR, 2.0D));
    }

    @Test void crowdScaleIsNeutralBelowTheThresholdGrowsAboveItAndIsCapped() {
        assertEquals(1.0D, engine.crowdScale(cfg.crowdThreshold()), 1e-9);
        double a = engine.crowdScale(cfg.crowdThreshold() * 2), b = engine.crowdScale(cfg.crowdThreshold() * 4);
        assertTrue(a > 1.0D && b > a);
        assertTrue(engine.crowdScale(1_000_000) <= cfg.crowdIntervalScale() * 2.0D + 1e-9);
    }

    @Test void theCrowdManagerHandsOutTheQuotaMostOverdueFirstAndReportsTheRest() {
        SchedulerSettings small = cfg.toBuilder().set("maxEvaluationsPerTick", 3).build();
        CrowdManager crowd = new CrowdManager(small);
        List<CrowdManager.Candidate> cands = new ArrayList<>();
        for (int i = 0; i < 8; i++) cands.add(new CrowdManager.Candidate(new UUID(0, i), 10, 100 - i * 5L, false));
        var plan = crowd.plan(cands, 200);
        assertEquals(3, plan.evaluate().size());
        assertEquals(8, plan.due());
        assertEquals(5, plan.deferred());
        assertEquals(new UUID(0, 7), plan.evaluate().get(0), "the one waiting longest goes first");
        assertTrue(crowd.plan(cands, 100).evaluate().size() <= 3);
    }

    @Test void nobodyIsEvaluatedBeforeItsTimeAndUrgentOnesGoRegardless() {
        CrowdManager crowd = new CrowdManager(cfg.toBuilder().set("maxEvaluationsPerTick", 1).build());
        var plan = crowd.plan(List.of(new CrowdManager.Candidate(new UUID(0, 1), 10, 195, false), new CrowdManager.Candidate(new UUID(0, 2), 100, 199, true),
                new CrowdManager.Candidate(new UUID(0, 3), 100, 199, true)), 200);
        assertEquals(List.of(new UUID(0, 2), new UUID(0, 3)), plan.evaluate().stream().sorted().toList(), "urgent NPCs ignore the quota; the not-yet-due one waits");
        assertEquals(0, plan.deferred());
    }

    @Test void phasesSpreadNpcsAcrossTheInterval() {
        java.util.Set<Integer> phases = new java.util.HashSet<>();
        for (int i = 0; i < 200; i++) phases.add(CrowdManager.phaseOf(UUID.nameUUIDFromBytes(("npc-" + i).getBytes()), 20));
        assertTrue(phases.size() >= 15, "phases used: " + phases.size());
        for (int p : phases) assertTrue(p >= 0 && p < 20);
    }

    @Test void settingsAreClampedSoNoConfigurationCanBreakTheScheduler() {
        SchedulerSettings s = SchedulerSettings.builder().set("dayLength", 5).set("maxEvaluationsPerTick", -4).set("nearbyDistance", 1.0D).set("visibleInterval", 0)
                .set("personalSpace", Double.NaN).set("groupMaxSize", 1).build();
        assertEquals(1200, s.dayLength());
        assertEquals(1, s.maxEvaluationsPerTick());
        assertTrue(s.nearbyDistance() >= s.visibleDistance());
        assertEquals(1, s.visibleInterval());
        assertTrue(Double.isFinite(s.personalSpace()));
        assertEquals(2, s.groupMaxSize());
        assertTrue(SchedulerSettings.builder().set("lifestyles", List.of("x;types=y")).build().lifestyles().size() == 1);
        assertThrows(IllegalArgumentException.class, () -> SchedulerSettings.builder().set("nonsense", 1));
    }
}
