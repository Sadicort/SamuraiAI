package yadi.samuraiai.task;

import yadi.samuraiai.action.ActionExecutor;
import yadi.samuraiai.ai.navigation.engine.NavigationHandle;
import yadi.samuraiai.ai.navigation.engine.NavigationOptions;
import yadi.samuraiai.ai.navigation.movement.MovementBody;
import yadi.samuraiai.ai.navigation.movement.MovementMode;
import yadi.samuraiai.ai.navigation.world.NavigationService;
import yadi.samuraiai.ai.scheduler.engine.SchedulerAdvice;
import yadi.samuraiai.ai.scheduler.personality.ResponseKind;
import yadi.samuraiai.ai.scheduler.world.SchedulerService;
import yadi.samuraiai.context.NPCContext;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.controller.NPCController;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.runtime.NPCTickService;
import yadi.samuraiai.world.SpawnLocation;

/**
 * Carries out what the scheduler advises: goes to the place its advice names and stays there while the advice stands. The
 * scheduler decides where and why; this task only asks Navigation to get there (following the place if it moves, as a
 * formation slot does) and reports back when a place cannot be reached. It ends when the advice does, so the Brain plans again.
 */
public final class RoutineTask implements Task {
    /** Minimum ticks between two navigation requests for the same task, so an unreachable place is not retried every brain tick. */
    private static final int RETRY_TICKS = 40;
    private static final int TIMEOUT_TICKS = 6000;

    private NavigationHandle handle;
    private SpawnLocation target;
    private long lastRequest = Long.MIN_VALUE / 2;

    @Override public TaskStatus tick(NPCContext context, WorldContext world, NPCRuntime runtime, NPCController controller, ActionExecutor executor) {
        SchedulerAdvice advice = world.advice().orElse(null);
        if (advice == null || !advice.active()) return TaskStatus.SUCCESS;
        if (advice.place() == null || !controller.requiresPhysicalBody()) return TaskStatus.RUNNING;
        var body = controller.movementBody(runtime.getInstance());
        if (body.isEmpty()) return TaskStatus.RUNNING;
        MovementBody b = body.get();
        var place = advice.place();
        double distance = Math.hypot(b.x() - place.x(), b.z() - place.z());
        if (distance <= place.radius()) {
            stop("arrived");
            return TaskStatus.RUNNING;
        }
        boolean moved = target == null || Math.hypot(target.x() - place.x(), target.z() - place.z()) > Math.max(2.5D, place.radius());
        long now = NPCTickService.currentTick();
        if (handle != null && !handle.done() && !moved) return TaskStatus.RUNNING;
        if (handle != null && handle.state() == yadi.samuraiai.ai.navigation.pathfinding.PathState.FAILED && !moved) {
            SchedulerService.getInstance().unreachable(runtime.getId());
            return TaskStatus.FAILURE;
        }
        if (!moved && now - lastRequest < RETRY_TICKS) return TaskStatus.RUNNING;
        stop("retarget");
        var here = runtime.getInstance().getLocation();
        String dimension = here != null ? here.dimensionKey() : place.dimension();
        target = new SpawnLocation(dimension, place.x(), place.y(), place.z(), here == null ? 0.0F : here.yaw());
        handle = NavigationService.getInstance().navigate(runtime, target, new NavigationOptions(modeFor(advice, distance), Math.max(0.8D, place.radius() * 0.8D),
                TIMEOUT_TICKS, true, "Routine", advice.label()));
        lastRequest = now;
        return TaskStatus.RUNNING;
    }

    private static MovementMode modeFor(SchedulerAdvice advice, double distance) {
        if (advice.response() == ResponseKind.FLEE || advice.emergency()) return MovementMode.RUN;
        if (advice.response() == ResponseKind.INVESTIGATE) return advice.score() >= 60.0D ? MovementMode.RUN : MovementMode.WALK;
        if (advice.formation() != null && distance > 10.0D) return MovementMode.RUN;
        return MovementMode.WALK;
    }

    private void stop(String reason) {
        if (handle != null && !handle.done()) handle.cancel(reason);
    }

    @Override public void cancel() { stop("task cancelled"); }
}
