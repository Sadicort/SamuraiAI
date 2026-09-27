package yadi.samuraiai.task;

import yadi.samuraiai.action.ActionExecutor;
import yadi.samuraiai.context.NPCContext;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.controller.NPCController;
import yadi.samuraiai.npc.NPCRuntime;

/**
 * Placeholder task executed while the NPC has nothing more specific to do.
 */
public class RestTask implements Task {

    @Override
    public TaskStatus tick(NPCContext context, WorldContext world, NPCRuntime runtime,
                            NPCController controller, ActionExecutor executor) {
        return TaskStatus.SUCCESS;
    }
}
