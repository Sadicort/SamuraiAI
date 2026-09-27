package yadi.samuraiai.ai.scheduler.world;

import yadi.samuraiai.ai.scheduler.engine.SchedulerAdvice;
import yadi.samuraiai.ai.scheduler.personality.ResponseKind;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.goal.GoalType;

/**
 * The single place where the scheduler's vocabulary (routines and responses) meets the Brain's (goals). The scheduler knows
 * nothing about goals; the Brain knows nothing about routines; this adapter translates, so neither depends on the other.
 */
public final class GoalMapper {
    private GoalMapper() { }

    public static GoalType goalFor(SchedulerAdvice advice) {
        if (advice == null) return null;
        if (advice.response() != null) return goalFor(advice.response());
        return advice.routine() == null ? null : goalFor(advice.routine());
    }

    public static GoalType goalFor(RoutineType routine) {
        return switch (routine) {
            case WAKE -> GoalType.WAKE;
            case PATROL -> GoalType.PATROL;
            case WORK -> GoalType.WORK;
            case REST -> GoalType.REST;
            case EAT -> GoalType.EAT;
            case MEDITATE -> GoalType.MEDITATE;
            case SLEEP -> GoalType.SLEEP;
            case SOCIAL -> GoalType.SOCIAL;
            case TRAINING -> GoalType.TRAINING;
            case PRAYER -> GoalType.PRAYER;
            case GUARD -> GoalType.GUARD;
            case MERCHANT -> GoalType.TRADE;
        };
    }

    public static GoalType goalFor(ResponseKind response) {
        return switch (response) {
            case INVESTIGATE -> GoalType.INVESTIGATE;
            case FLEE -> GoalType.FLEE;
            case ASSIST -> GoalType.PROTECT;
            case WATCH -> GoalType.GUARD;
            case RAISE_ALARM, IGNORE -> null;
        };
    }

    /** How strongly the scheduler's wish outweighs the goal's own priority, by the layer it came from. */
    public static int bonus(SchedulerAdvice advice) {
        return switch (advice.layer()) {
            case BASELINE -> 55;
            case PERSONAL -> 70;
            case SITUATIONAL -> 75;
            case EMERGENCY -> 110;
        };
    }
}
