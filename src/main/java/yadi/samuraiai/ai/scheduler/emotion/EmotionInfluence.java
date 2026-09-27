package yadi.samuraiai.ai.scheduler.emotion;

import java.util.Map;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;

/** What a mood does to scheduling: added stress per evaluation, a bias on routines, and whether it is an outright emergency. */
public record EmotionInfluence(Mood mood, double strength, Map<RoutineType, Double> routineBias, double stressPerSecond, boolean panic) {
    public static final EmotionInfluence NONE = new EmotionInfluence(Mood.CALM, 0, Map.of(), 0, false);
    public double bias(RoutineType routine) { return routineBias.getOrDefault(routine, 0.0D); }
}
