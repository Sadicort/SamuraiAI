package yadi.samuraiai.ai.scheduler;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.testkit.SchedulerHarness;

/**
 * Cost of the scheduler at scale, measured in the simulated village (JIT warmed first). The limits are deliberately generous
 * so the test is stable on a loaded machine; the figures it prints are the ones the performance document quotes.
 */
class SchedulerPerformanceTest {
    private static void populate(SchedulerHarness h, int npcs) {
        String[] types = {"villager", "guard", "merchant", "samurai", "monk"};
        for (int i = 0; i < npcs; i++) {
            var s = h.add(types[i % types.length], (i % 50) * 3, (i / 50) * 3);
            // a realistic spread of distances to the nearest player: a few close, most far away
            s.playerDistance = i % 20 == 0 ? 15 : i % 5 == 0 ? 60 : i % 3 == 0 ? 120 : 400;
            s.canFight = i % 5 == 3;
        }
    }

    @Test void aThousandNpcsCostALittleOfATickAndNeverExceedTheirBudget() {
        var h = new SchedulerHarness(SchedulerSettings.defaults());
        populate(h, 1000);
        h.setWorldTime(0);
        h.run(600);                       // warm-up: registration, grouping, JIT
        h.scheduler.metrics().reset();
        h.run(2400);                      // two minutes of server time
        var m = h.scheduler.metrics().snapshot();
        System.out.printf("SCHED_PERF npcs=%d ticks=%d avgTick=%.1fus maxTick=%.1fus evaluations=%d avgEval=%.1fus deferred=%d crowdScale=%.2f buckets=%s%n",
                m.npcs(), m.ticks(), m.averageTickMicros(), m.maxTickMicros(), m.evaluations(), m.averageEvaluationMicros(), m.deferred(), m.crowdScale(), m.buckets());
        assertEquals(1000, m.npcs());
        assertTrue(m.evaluations() <= 2400L * h.scheduler.settings().maxEvaluationsPerTick(), "per-tick quota respected");
        assertTrue(m.averageTickMicros() < 2500.0D, "average tick " + m.averageTickMicros() + "us");
        assertTrue(m.averageEvaluationMicros() < 500.0D, "average evaluation " + m.averageEvaluationMicros() + "us");
        assertEquals(0, m.failures());
    }

    @Test void theWorkPerTickDoesNotGrowWithThePopulationBeyondTheQuota() {
        long small = averageTickMicros(100), large = averageTickMicros(1500);
        System.out.printf("SCHED_SCALING 100 npcs=%dus 1500 npcs=%dus%n", small, large);
        assertTrue(large < Math.max(4000, small * 40), "1500 NPCs cost " + large + "us per tick against " + small + "us for 100");
    }

    private static long averageTickMicros(int npcs) {
        var h = new SchedulerHarness(SchedulerSettings.defaults());
        populate(h, npcs);
        h.setWorldTime(0);
        h.run(400);
        h.scheduler.metrics().reset();
        h.run(800);
        return (long) h.scheduler.metrics().snapshot().averageTickMicros();
    }
}
