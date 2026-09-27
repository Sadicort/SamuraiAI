package yadi.samuraiai.behavior;

import yadi.samuraiai.context.NPCContext;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.goal.Goal;
import yadi.samuraiai.task.Task;

import java.util.List;

/**
 * A strategy for pursuing a Goal, expressed as an ordered list of Tasks.
 * Independent of any specific NPC type so the same Behavior (e.g. patrol)
 * can be reused across samurai, guards, or anyone else with CAN_PATROL.
 */
public interface Behavior {

    List<Task> plan(NPCContext context, WorldContext world, Goal goal);

}
