package yadi.samuraiai.behavior;

import java.util.List;
import yadi.samuraiai.context.NPCContext;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.goal.Goal;
import yadi.samuraiai.task.RestTask;
import yadi.samuraiai.task.RoutineTask;
import yadi.samuraiai.task.Task;

/**
 * The behavior for every goal that comes from the scheduler's day (work, eat, sleep, meditate, guard...). It has no plan of its
 * own: the scheduler names the place, and the routine task takes the NPC there and keeps it there. Without advice there is
 * nothing to do and the NPC simply rests.
 */
public final class RoutineBehavior implements Behavior {
    @Override public List<Task> plan(NPCContext context, WorldContext world, Goal goal) {
        return world != null && world.advice().isPresent() ? List.of(new RoutineTask()) : List.of(new RestTask());
    }
}
