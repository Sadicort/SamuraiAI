package yadi.samuraiai.task;

import yadi.samuraiai.action.ActionExecutor;
import yadi.samuraiai.context.NPCContext;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.controller.NPCController;
import yadi.samuraiai.npc.NPCRuntime;

/**
 * A concrete unit of work within a Behavior (e.g. "go to point A", "request
 * a dialogue reply"). Ticked once per Brain update until it reports
 * something other than RUNNING.
 */
public interface Task {

    default void cancel() {}

    TaskStatus tick(NPCContext context, WorldContext world, NPCRuntime runtime,
                     NPCController controller, ActionExecutor executor);

}
