package yadi.samuraiai.behavior;

import yadi.samuraiai.context.*;
import yadi.samuraiai.goal.Goal;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.task.*;
import yadi.samuraiai.world.SpawnLocation;
import java.util.List;

/** Deterministic square patrol around the NPC's home anchor (stable, so the loop does not drift). */
public final class PatrolBehavior implements Behavior {
    @Override public List<Task> plan(NPCContext context, WorldContext world, Goal goal) {
        // When the scheduler is patrolling this NPC it names each point (and the formation slot in a group); otherwise the home square.
        if (world != null && world.advice().filter(a -> a.routine() == yadi.samuraiai.ai.scheduler.routine.RoutineType.PATROL).isPresent())
            return List.of(new yadi.samuraiai.task.RoutineTask());
        SpawnLocation anchor = context.getInstance().getHome();
        if (anchor == null) return List.of(new RestTask());
        double radius = 4.0;
        return List.of(
                new MoveToTask(anchor.withPosition(anchor.x() + radius, anchor.y(), anchor.z()), 1.0, 1.0, 200),
                new MoveToTask(anchor.withPosition(anchor.x() + radius, anchor.y(), anchor.z() + radius), 1.0, 1.0, 200),
                new MoveToTask(anchor.withPosition(anchor.x(), anchor.y(), anchor.z() + radius), 1.0, 1.0, 200),
                new MoveToTask(anchor.withPosition(anchor.x(), anchor.y(), anchor.z()), 1.0, 1.0, 200));
    }
}
