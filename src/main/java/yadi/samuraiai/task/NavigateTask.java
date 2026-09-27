package yadi.samuraiai.task;

import yadi.samuraiai.action.ActionExecutor;
import yadi.samuraiai.ai.navigation.engine.NavigationHandle;
import yadi.samuraiai.ai.navigation.engine.NavigationOptions;
import yadi.samuraiai.ai.navigation.world.NavigationService;
import yadi.samuraiai.context.NPCContext;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.controller.NPCController;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.world.SpawnLocation;

/**
 * Asks the Navigation Engine to take the NPC somewhere and reports how that goes. The task decides nothing
 * about routes: it requests, polls the handle once per brain tick and cancels when the brain moves on, which
 * is what finally stops an NPC that was told to abandon a patrol mid-walk.
 */
public final class NavigateTask implements Task {
    private final SpawnLocation target;
    private final NavigationOptions options;
    private NavigationHandle handle;

    public NavigateTask(SpawnLocation target, NavigationOptions options) {
        this.target = java.util.Objects.requireNonNull(target, "target");
        this.options = java.util.Objects.requireNonNull(options, "options");
    }

    public NavigationHandle handle() { return handle; }

    @Override public TaskStatus tick(NPCContext context, WorldContext world, NPCRuntime runtime,
                                     NPCController controller, ActionExecutor executor) {
        // A body-less NPC cannot walk: report failure honestly instead of pretending it arrived.
        if (!controller.requiresPhysicalBody()) return TaskStatus.FAILURE;
        if (handle == null) handle = NavigationService.getInstance().navigate(runtime, target, options);
        return switch (handle.state()) {
            case COMPLETED -> TaskStatus.SUCCESS;
            case FAILED, CANCELLED -> TaskStatus.FAILURE;
            default -> TaskStatus.RUNNING;
        };
    }

    @Override public void cancel() {
        if (handle != null && !handle.done()) handle.cancel("task cancelled");
    }
}
