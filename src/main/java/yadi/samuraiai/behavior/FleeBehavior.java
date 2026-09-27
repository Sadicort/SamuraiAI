package yadi.samuraiai.behavior;

import yadi.samuraiai.context.*;
import yadi.samuraiai.goal.Goal;
import yadi.samuraiai.task.*;
import yadi.samuraiai.world.SpawnLocation;
import java.util.List;

/** Moves away from the nearest perceived player without blocking. */
public final class FleeBehavior implements Behavior {
    @Override public List<Task> plan(NPCContext context, WorldContext world, Goal goal) {
        // The scheduler picks where to run (away from the danger, or home if that is the safer side); without it, away from the nearest player.
        if (world != null && world.advice().filter(a -> a.response() == yadi.samuraiai.ai.scheduler.personality.ResponseKind.FLEE).isPresent())
            return List.of(new yadi.samuraiai.task.RoutineTask());
        SpawnLocation origin = context.getInstance().getLocation();
        if (origin == null || world == null || world.nearestPlayer().isEmpty()) return List.of(new RestTask());
        var observed = world.nearestPlayer().get().location();
        if (observed == null) return List.of(new RestTask());
        double dx = origin.x() - observed.x(), dz = origin.z() - observed.z();
        double length = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
        return List.of(new MoveToTask(origin.withPosition(origin.x() + dx / length * 8, origin.y(), origin.z() + dz / length * 8), 1.2, 1.0, 200));
    }
}
