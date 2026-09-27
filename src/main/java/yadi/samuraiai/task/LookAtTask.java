package yadi.samuraiai.task;

import yadi.samuraiai.action.*;
import yadi.samuraiai.context.*;
import yadi.samuraiai.controller.*;
import yadi.samuraiai.npc.NPCRuntime;
import java.util.UUID;

public final class LookAtTask implements Task {
    private final UUID target;
    public LookAtTask(UUID target) { this.target = target; }
    @Override public TaskStatus tick(NPCContext c, WorldContext w, NPCRuntime r, NPCController controller, ActionExecutor e) {
        if (!controller.requiresPhysicalBody()) return TaskStatus.SUCCESS;
        return controller.lookAt(r.getInstance(), target) ? TaskStatus.SUCCESS : TaskStatus.FAILURE;
    }
}
