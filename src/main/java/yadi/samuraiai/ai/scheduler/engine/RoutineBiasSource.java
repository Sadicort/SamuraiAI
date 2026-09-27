package yadi.samuraiai.ai.scheduler.engine;

import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;

/**
 * Something outside the scheduler that has an opinion about what an NPC should be doing now (the living world's village
 * timetable, a festival, a raid). It only adds points to routines; the scheduler still weighs them with lifestyle,
 * personality, energy, mood and events and makes the decision. Sources are consulted once per evaluation, so they must be
 * cheap and must never touch an entity.
 */
@FunctionalInterface
public interface RoutineBiasSource {
    Map<RoutineType, Double> bias(UUID npc);
}
