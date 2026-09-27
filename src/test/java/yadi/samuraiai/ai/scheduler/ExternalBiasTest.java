package yadi.samuraiai.ai.scheduler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.scheduler.engine.Candidate;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.testkit.SchedulerHarness;

/**
 * Outside opinions (the living world's village timetable) reach the routine planner as extra points: summed over sources,
 * clamped, shown in the candidate's reason, and a failing source never breaks an evaluation.
 */
class ExternalBiasTest {
    private static SchedulerSettings steady() { return SchedulerSettings.builder().set("personalityJitter", 0.0D).build(); }

    @Test void sourcesAreSummedClampedAndFailuresSkipped() {
        var h = new SchedulerHarness(steady());
        var npc = h.add("villager", 0, 0);
        h.scheduler.registerBiasSource("a", id -> Map.of(RoutineType.PRAYER, 60.0D, RoutineType.WORK, -10.0D));
        h.scheduler.registerBiasSource("b", id -> Map.of(RoutineType.PRAYER, 50.0D));
        h.scheduler.registerBiasSource("broken", id -> { throw new IllegalStateException("boom"); });
        Map<RoutineType, Double> bias = h.scheduler.externalBias(npc.id);
        assertEquals(80.0D, bias.get(RoutineType.PRAYER), 1e-9, "60 + 50 clamped to 80");
        assertEquals(-10.0D, bias.get(RoutineType.WORK), 1e-9);
        assertTrue(h.scheduler.metrics().snapshot().failures() >= 1, "the failing source was counted");
        h.scheduler.unregisterBiasSource("broken");
        assertEquals(java.util.Set.of("a", "b"), h.scheduler.biasSources());
    }

    @Test void thePlannerWeighsItAndSaysWhy() {
        var h = new SchedulerHarness(steady());
        var npc = h.add("villager", 0, 0);
        h.setWorldTime(6000);
        h.scheduler.registerBiasSource("living", id -> id.equals(npc.id) ? Map.of(RoutineType.SOCIAL, 70.0D) : Map.of());
        h.run(40);
        var schedule = h.scheduler.schedule(npc.id).orElseThrow();
        Candidate social = schedule.lastCandidates().stream().filter(c -> c.intent().routine() == RoutineType.SOCIAL).findFirst().orElse(null);
        assertNotNull(social, "the biased routine is a candidate: " + schedule.lastCandidates());
        assertTrue(social.reason().contains("life +70"), social.reason());
        h.scheduler.unregisterBiasSource("living");
        h.run(400);
        Candidate after = h.scheduler.schedule(npc.id).orElseThrow().lastCandidates().stream().filter(c -> c.intent().routine() == RoutineType.SOCIAL).findFirst().orElse(null);
        assertTrue(after == null || !after.reason().contains("life"), "without the source the reason is gone");
    }
}
