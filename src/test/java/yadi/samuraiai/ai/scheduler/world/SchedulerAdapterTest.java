package yadi.samuraiai.ai.scheduler.world;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.scheduler.emotion.Mood;
import yadi.samuraiai.ai.scheduler.engine.SchedulerAdvice;
import yadi.samuraiai.ai.scheduler.optimize.TickBucket;
import yadi.samuraiai.ai.scheduler.personality.ResponseKind;
import yadi.samuraiai.ai.scheduler.personality.Temperament;
import yadi.samuraiai.ai.scheduler.priority.PriorityLayer;
import yadi.samuraiai.ai.scheduler.routine.RoutineInstance;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.time.DayPeriod;
import yadi.samuraiai.ai.scheduler.zone.Place;
import yadi.samuraiai.ai.scheduler.zone.Zone;
import yadi.samuraiai.ai.scheduler.zone.ZoneKind;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.decision.UtilityDecisionEngine;
import yadi.samuraiai.goal.Goal;
import yadi.samuraiai.goal.GoalType;

/** The seams between the scheduler and the Brain: the goal vocabulary, the decision engine's use of advice, and the zone file. */
class SchedulerAdapterTest {
    private static SchedulerAdvice advice(RoutineType routine, ResponseKind response, PriorityLayer layer) {
        return new SchedulerAdvice(new UUID(0, 1), 100, routine, response, layer, RoutineInstance.State.ACTIVE, new Place("d", 0, 64, 0, 3, null),
                layer == PriorityLayer.EMERGENCY, 60, DayPeriod.MORNING, Mood.CALM, List.of(), "test", 0, Temperament.NEUTRAL, TickBucket.VISIBLE, null, null, null);
    }

    private static WorldContext with(SchedulerAdvice advice) { return WorldContext.empty().withAdvice(advice); }

    @Test void everyRoutineHasItsOwnGoalAndEveryResponseThatActsHasOne() {
        EnumSet<GoalType> seen = EnumSet.noneOf(GoalType.class);
        for (RoutineType r : RoutineType.values()) assertTrue(seen.add(GoalMapper.goalFor(r)), r + " must map to a goal of its own");
        assertEquals(GoalType.INVESTIGATE, GoalMapper.goalFor(ResponseKind.INVESTIGATE));
        assertEquals(GoalType.FLEE, GoalMapper.goalFor(ResponseKind.FLEE));
        assertEquals(GoalType.PROTECT, GoalMapper.goalFor(ResponseKind.ASSIST));
        assertEquals(GoalType.GUARD, GoalMapper.goalFor(ResponseKind.WATCH));
        assertNull(GoalMapper.goalFor(ResponseKind.RAISE_ALARM), "raising an alarm is a side effect, not a goal");
        assertNull(GoalMapper.goalFor((SchedulerAdvice) null));
        assertEquals(GoalType.FLEE, GoalMapper.goalFor(advice(null, ResponseKind.FLEE, PriorityLayer.EMERGENCY)));
        assertEquals(GoalType.TRADE, GoalMapper.goalFor(advice(RoutineType.MERCHANT, null, PriorityLayer.BASELINE)));
    }

    @Test void higherLayersLeanHarderOnTheScale() {
        assertTrue(GoalMapper.bonus(advice(RoutineType.WORK, null, PriorityLayer.BASELINE)) < GoalMapper.bonus(advice(RoutineType.SLEEP, null, PriorityLayer.PERSONAL)));
        assertTrue(GoalMapper.bonus(advice(RoutineType.SLEEP, null, PriorityLayer.PERSONAL)) < GoalMapper.bonus(advice(null, ResponseKind.FLEE, PriorityLayer.EMERGENCY)));
    }

    @Test void adviceMakesTheAdvisedGoalWinInTheDecisionEngine() {
        var engine = new UtilityDecisionEngine();
        var world = with(advice(RoutineType.SLEEP, null, PriorityLayer.BASELINE));
        assertTrue(engine.score(new Goal(GoalType.SLEEP), null, world) > engine.score(new Goal(GoalType.PATROL), null, world));
        assertTrue(engine.score(new Goal(GoalType.SLEEP), null, world) > engine.score(new Goal(GoalType.TALK), null, world), "nobody chats while asleep");
        var unadvised = WorldContext.empty();
        assertTrue(engine.score(new Goal(GoalType.SLEEP), null, unadvised) < engine.score(new Goal(GoalType.PATROL), null, unadvised), "without advice nothing changes");
    }

    @Test void anEmergencyResponseOutweighsAnyRoutine() {
        var engine = new UtilityDecisionEngine();
        var world = with(advice(null, ResponseKind.FLEE, PriorityLayer.EMERGENCY));
        assertEquals(GoalType.FLEE, engine.decide(null, world, List.of(new Goal(GoalType.PATROL), new Goal(GoalType.TALK), new Goal(GoalType.WORK), new Goal(GoalType.FLEE), new Goal(GoalType.PROTECT))).getType());
    }

    @Test void theWorldContextKeepsItsPerceptionWhenAdviceIsAttached() {
        var base = WorldContext.empty();
        var advised = base.withAdvice(advice(RoutineType.WORK, null, PriorityLayer.BASELINE));
        assertTrue(base.advice().isEmpty());
        assertEquals(RoutineType.WORK, advised.advice().orElseThrow().routine());
        assertEquals(base.getWorldTime(), advised.getWorldTime());
        assertTrue(advised.withAdvice(null).advice().isEmpty());
    }

    @Test void zonesSurviveARoundTripThroughTheirFile() {
        Zone plain = new Zone("market", "minecraft:overworld", ZoneKind.MARKET, 10.5, 64, -3, 8, 6, null, null, null);
        Zone rich = new Zone("shrine", "minecraft:overworld", ZoneKind.TEMPLE, 0, 70, 0, 4, 2, EnumSet.of(DayPeriod.MORNING, DayPeriod.DAWN), "someone",
                List.of(new double[]{1, 2, 3}, new double[]{4, 5, 6}));
        for (Zone zone : List.of(plain, rich)) {
            Zone back = ZoneStore.fromJson(ZoneStore.toJson(zone));
            assertEquals(zone.id(), back.id());
            assertEquals(zone.kind(), back.kind());
            assertEquals(zone.radius(), back.radius(), 1e-9);
            assertEquals(zone.capacity(), back.capacity());
            assertEquals(zone.openPeriods(), back.openPeriods());
            assertEquals(zone.owner(), back.owner());
            assertEquals(zone.waypoints().size(), back.waypoints().size());
        }
        assertEquals(EnumSet.allOf(DayPeriod.class), ZoneStore.fromJson(ZoneStore.toJson(plain)).openPeriods(), "no listed periods means always open");
    }

    @Test void aCorruptZoneEntryIsRejectedNotAcceptedHalfWay() {
        var bad = ZoneStore.toJson(new Zone("z", "d", ZoneKind.HOME, 0, 0, 0, 3, 1, null, null, null));
        bad.addProperty("kind", "CASTLE");
        assertThrows(RuntimeException.class, () -> ZoneStore.fromJson(bad));
    }
}
