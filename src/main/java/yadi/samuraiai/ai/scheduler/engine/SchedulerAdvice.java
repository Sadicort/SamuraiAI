package yadi.samuraiai.ai.scheduler.engine;

import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.scheduler.emotion.Mood;
import yadi.samuraiai.ai.scheduler.group.GroupRole;
import yadi.samuraiai.ai.scheduler.optimize.TickBucket;
import yadi.samuraiai.ai.scheduler.personality.ResponseKind;
import yadi.samuraiai.ai.scheduler.personality.Temperament;
import yadi.samuraiai.ai.scheduler.priority.PriorityLayer;
import yadi.samuraiai.ai.scheduler.routine.RoutineInstance;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.stack.BackgroundBehavior;
import yadi.samuraiai.ai.scheduler.time.DayPeriod;
import yadi.samuraiai.ai.scheduler.zone.Place;

/**
 * The scheduler's answer for one NPC, and its whole contract with the Brain: what the NPC should be doing (a routine or a
 * response), where, in which priority layer and why, which background behaviours accompany it, and how the NPC's
 * personality colours perception and movement. It is advice; the Brain decides, the Behavior plans, Navigation moves.
 */
public record SchedulerAdvice(UUID npcId, long tick, RoutineType routine, ResponseKind response, PriorityLayer layer, RoutineInstance.State state,
                              Place place, boolean emergency, double score, DayPeriod period, Mood mood, List<BackgroundBehavior> background,
                              String reason, int interrupted, Temperament temperament, TickBucket bucket, String groupId, GroupRole role, String formation) {
    public SchedulerAdvice { background = List.copyOf(background); }

    public static SchedulerAdvice idle(UUID npcId, long tick, DayPeriod period, Mood mood, Temperament temperament, TickBucket bucket, String reason) {
        return new SchedulerAdvice(npcId, tick, null, null, PriorityLayer.BASELINE, null, null, false, 0, period, mood, List.of(), reason, 0, temperament, bucket, null, null, null);
    }

    public boolean active() { return routine != null || response != null; }
    public String label() { return routine != null ? routine.name() : response != null ? response.name() : "NONE"; }
}
