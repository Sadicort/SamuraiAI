package yadi.samuraiai.behavior;

import yadi.samuraiai.context.NPCContext;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.goal.Goal;
import yadi.samuraiai.task.RestTask;
import yadi.samuraiai.task.Task;

import java.util.List;

/**
 * Fallback behavior used for any Goal that does not yet have a dedicated
 * Behavior (patrol/combat/etc. are future work). Keeps the Brain always
 * having *something* sensible to do instead of crashing or idling forever.
 */
public class IdleBehavior implements Behavior {

    @Override
    public List<Task> plan(NPCContext context, WorldContext world, Goal goal) {
        return List.of(new RestTask());
    }
}
