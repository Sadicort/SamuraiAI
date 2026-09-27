package yadi.samuraiai.task;

import yadi.samuraiai.action.*;
import yadi.samuraiai.ai.navigation.engine.NavigationOptions;
import yadi.samuraiai.ai.navigation.movement.MovementMode;
import yadi.samuraiai.ai.navigation.world.NavigationService;
import yadi.samuraiai.config.SamuraiSettings;
import yadi.samuraiai.context.*;
import yadi.samuraiai.controller.*;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.world.SpawnLocation;

/**
 * Moves the NPC to a point. When the controller exposes a movement body and navigation is enabled, the journey
 * is delegated to the Navigation Engine (routing, doors, obstacles, recovery). Otherwise it falls back to the
 * controller's basic per-tick steering, exactly as before, so backends without a body keep working.
 */
public final class MoveToTask implements Task {
    private final SpawnLocation target; private final double speed, tolerance; private final int maxTicks;
    private int ticks;
    private NavigateTask delegate;

    public MoveToTask(SpawnLocation target, double speed, double tolerance, int maxTicks) {
        this.target = target; this.speed = speed; this.tolerance = tolerance; this.maxTicks = maxTicks;
    }

    @Override public TaskStatus tick(NPCContext context, WorldContext world, NPCRuntime runtime,
                                     NPCController controller, ActionExecutor executor) {
        if (!controller.requiresPhysicalBody()) return TaskStatus.SUCCESS;
        if (delegate == null && usesNavigation(runtime, controller)) delegate = new NavigateTask(target, optionsFor());
        if (delegate != null) return delegate.tick(context, world, runtime, controller, executor);
        if (!controller.isPhysicalPresent(runtime.getInstance())) return TaskStatus.FAILURE;
        controller.moveTo(runtime.getInstance(), target, speed); controller.synchronize(runtime.getInstance());
        SpawnLocation current = runtime.getInstance().getLocation();
        if (current != null && current.sameDimension(target) && current.distanceTo(target) <= tolerance) return TaskStatus.SUCCESS;
        return ++ticks >= maxTicks ? TaskStatus.FAILURE : TaskStatus.RUNNING;
    }

    @Override public void cancel() {
        ticks = maxTicks;
        if (delegate != null) delegate.cancel();
    }

    private static boolean usesNavigation(NPCRuntime runtime, NPCController controller) {
        return NavigationService.getInstance().enabled() && controller.movementBody(runtime.getInstance()).isPresent();
    }

    private NavigationOptions optionsFor() {
        MovementMode mode = speed >= 1.6D ? MovementMode.SPRINT : speed >= 1.15D ? MovementMode.RUN : speed <= 0.7D ? MovementMode.SNEAK : MovementMode.WALK;
        int timeoutTicks = (int) Math.min(Integer.MAX_VALUE / 2L, (long) Math.max(1, maxTicks) * Math.max(1, SamuraiSettings.brainTickInterval()));
        return new NavigationOptions(mode, Math.max(0.6D, tolerance), timeoutTicks, true, "MoveTo", "MoveTo");
    }
}
