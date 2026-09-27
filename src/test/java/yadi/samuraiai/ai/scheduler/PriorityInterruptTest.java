package yadi.samuraiai.ai.scheduler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.scheduler.conflict.ConflictResolver;
import yadi.samuraiai.ai.scheduler.engine.Candidate;
import yadi.samuraiai.ai.scheduler.engine.Intent;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.engine.Source;
import yadi.samuraiai.ai.scheduler.interrupt.InterruptPolicy;
import yadi.samuraiai.ai.scheduler.interrupt.InterruptStack;
import yadi.samuraiai.ai.scheduler.personality.ResponseKind;
import yadi.samuraiai.ai.scheduler.priority.PriorityEngine;
import yadi.samuraiai.ai.scheduler.priority.PriorityLayer;
import yadi.samuraiai.ai.scheduler.routine.RoutineInstance;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.zone.Place;

class PriorityInterruptTest {
    private final SchedulerSettings cfg = SchedulerSettings.defaults();
    private final PriorityEngine engine = new PriorityEngine(cfg);
    private static final Place HERE = new Place("d", 0, 64, 0, 3, null);

    private static Candidate routine(RoutineType r, PriorityLayer layer, double score) { return new Candidate(Intent.of(r, HERE), layer, Source.ROUTINE, score, "test"); }
    private static Candidate response(ResponseKind r, PriorityLayer layer, double score) { return new Candidate(Intent.respond(r, HERE), layer, Source.EVENT, score, "test"); }

    @Test void theHighestQualifyingLayerWinsEvenWithALowerScore() {
        var sel = engine.select(List.of(routine(RoutineType.WORK, PriorityLayer.BASELINE, 90), response(ResponseKind.INVESTIGATE, PriorityLayer.SITUATIONAL, 40)), null, null, 0);
        assertEquals("S:INVESTIGATE", sel.winner().key());
        assertTrue(sel.conflict().contested());
    }

    @Test void aCandidateBelowItsLayersThresholdIsIgnored() {
        var sel = engine.select(List.of(routine(RoutineType.WORK, PriorityLayer.BASELINE, 50), response(ResponseKind.WATCH, PriorityLayer.SITUATIONAL, engine.threshold(PriorityLayer.SITUATIONAL) - 1)), null, null, 0);
        assertEquals("R:WORK", sel.winner().key());
        assertNull(engine.select(List.of(routine(RoutineType.WORK, PriorityLayer.BASELINE, 1)), null, null, 0));
    }

    @Test void anEmergencyOverridesEverythingIncludingHysteresisAndMinimumDwell() {
        var sel = engine.select(List.of(routine(RoutineType.SLEEP, PriorityLayer.BASELINE, 200), response(ResponseKind.FLEE, PriorityLayer.EMERGENCY, cfg.emergencyThreshold() + 1)),
                "R:SLEEP", PriorityLayer.BASELINE, 0);
        assertEquals("S:FLEE", sel.winner().key());
        assertTrue(sel.emergency());
        assertFalse(sel.keepCurrent());
    }

    @Test void whatIsBeingDoneIsKeptUnlessARivalBeatsItByTheMarginAfterTheMinimumDwell() {
        var current = routine(RoutineType.WORK, PriorityLayer.BASELINE, 50);
        var rival = routine(RoutineType.SOCIAL, PriorityLayer.BASELINE, 50 + cfg.switchMargin() + 1);
        assertTrue(engine.select(List.of(current, rival), "R:WORK", PriorityLayer.BASELINE, cfg.minRoutineTicks() - 1).keepCurrent(), "too soon");
        assertEquals("R:SOCIAL", engine.select(List.of(current, rival), "R:WORK", PriorityLayer.BASELINE, cfg.minRoutineTicks()).winner().key());
        var close = routine(RoutineType.SOCIAL, PriorityLayer.BASELINE, 50 + cfg.switchMargin() - 1);
        assertTrue(engine.select(List.of(current, close), "R:WORK", PriorityLayer.BASELINE, cfg.minRoutineTicks() * 10L).keepCurrent(), "not enough better");
    }

    @Test void aHigherLayerPreemptsAtOnceWithoutWaitingOutTheDwell() {
        var sel = engine.select(List.of(routine(RoutineType.WORK, PriorityLayer.BASELINE, 60), routine(RoutineType.SLEEP, PriorityLayer.PERSONAL, 70)), "R:WORK", PriorityLayer.BASELINE, 0);
        assertEquals("R:SLEEP", sel.winner().key());
        assertFalse(sel.keepCurrent());
    }

    @Test void whenTheCurrentNoLongerQualifiesTheBestQualifyingOneTakesOver() {
        var sel = engine.select(List.of(routine(RoutineType.EAT, PriorityLayer.BASELINE, 40)), "R:SLEEP", PriorityLayer.PERSONAL, 5);
        assertEquals("R:EAT", sel.winner().key());
    }

    @Test void theResolverExplainsWhyEachLoserLost() {
        var resolver = new ConflictResolver(4);
        var res = resolver.resolve(List.of(routine(RoutineType.WORK, PriorityLayer.BASELINE, 50), new Candidate(Intent.of(RoutineType.PATROL, HERE), PriorityLayer.BASELINE, Source.GROUP, 48, "order"),
                response(ResponseKind.WATCH, PriorityLayer.SITUATIONAL, 40)));
        assertEquals("S:WATCH", res.winner().key());
        assertEquals(2, res.losers().size());
        assertTrue(res.losers().get(0).reason().contains("layer"));
        var tie = resolver.resolve(List.of(routine(RoutineType.WORK, PriorityLayer.BASELINE, 50), new Candidate(Intent.of(RoutineType.PATROL, HERE), PriorityLayer.BASELINE, Source.GROUP, 48, "order")));
        assertEquals("R:PATROL", tie.winner().key(), "within the margin the more authoritative source wins");
        assertNull(resolver.resolve(List.of()));
    }

    // ------------------------------------------------------------------ interrupt stack

    private RoutineInstance instance(RoutineType type) { return new RoutineInstance(Intent.of(type, HERE), PriorityLayer.BASELINE, 0, 1000, "test"); }

    @Test void cancelledRoutinesAreNeverStacked() {
        InterruptStack stack = new InterruptStack(() -> cfg);
        var dropped = stack.push(instance(RoutineType.MEDITATE), InterruptPolicy.CANCEL, "x", 0);
        assertEquals(1, dropped.size());
        assertTrue(stack.isEmpty());
    }

    @Test void aPausedRoutineResumesWhereItStoppedWithinItsTimeLimit() {
        InterruptStack stack = new InterruptStack(() -> cfg);
        RoutineInstance r = instance(RoutineType.PATROL);
        r.markArrived(0);
        r.advance(300);
        stack.push(r, InterruptPolicy.PAUSE, "sound", 100);
        r.pause(false, 100);
        var resumed = stack.resume(100 + cfg.pauseMaxTicks() - 1).orElseThrow();
        assertSame(r, resumed.instance());
        assertEquals(300, r.doneTicks());
        r.resume(200);
        assertEquals(RoutineInstance.State.ACTIVE, r.state());
    }

    @Test void aSuspendedRoutineLapsesAfterItsShorterLimit() {
        InterruptStack stack = new InterruptStack(() -> cfg);
        stack.push(instance(RoutineType.SLEEP), InterruptPolicy.SUSPEND, "noise", 0);
        var result = stack.resume(cfg.suspendMaxTicks() + 1).orElseThrow();
        assertNull(result.instance());
        assertEquals(1, result.lapsed().size());
        assertTrue(stack.isEmpty());
    }

    @Test void aRestartedRoutineBeginsAgainFromNothing() {
        InterruptStack stack = new InterruptStack(() -> cfg);
        RoutineInstance r = instance(RoutineType.EAT);
        r.markArrived(0);
        r.advance(500);
        stack.push(r, InterruptPolicy.RESTART, "x", 10);
        var resumed = stack.resume(20).orElseThrow();
        assertEquals(0, resumed.instance().doneTicks());
        assertEquals(1, resumed.instance().restarts());
        assertEquals(RoutineInstance.State.TRAVELLING, resumed.instance().state());
    }

    @Test void theMostRecentInterruptionComesBackFirstAndTheDepthIsLimited() {
        SchedulerSettings small = cfg.toBuilder().set("maxInterruptDepth", 2).build();
        InterruptStack stack = new InterruptStack(() -> small);
        RoutineInstance a = instance(RoutineType.WORK), b = instance(RoutineType.REST), c = instance(RoutineType.SOCIAL);
        stack.push(a, InterruptPolicy.RESUME, "1", 0);
        stack.push(b, InterruptPolicy.RESUME, "2", 1);
        var dropped = stack.push(c, InterruptPolicy.RESUME, "3", 2);
        assertEquals(1, dropped.size());
        assertSame(a, dropped.get(0).instance(), "the oldest frame is dropped");
        assertSame(c, stack.resume(3).orElseThrow().instance());
        assertSame(b, stack.resume(3).orElseThrow().instance());
        assertTrue(stack.resume(3).isEmpty());
    }
}
