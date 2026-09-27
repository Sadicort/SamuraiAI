package yadi.samuraiai.behavior;

import yadi.samuraiai.context.*;
import yadi.samuraiai.goal.Goal;
import yadi.samuraiai.task.*;
import java.util.List;

public final class ProtectBehavior implements Behavior {
    @Override public List<Task> plan(NPCContext context, WorldContext world, Goal goal) {
        // Answering an alarm or a threat: go to where the trouble is (the scheduler names the place).
        if (world != null && world.advice().filter(a -> a.response() == yadi.samuraiai.ai.scheduler.personality.ResponseKind.ASSIST).isPresent())
            return List.of(new yadi.samuraiai.task.RoutineTask());
        return world != null && world.nearestPlayer().isPresent()
                ? List.of(new LookAtTask(world.nearestPlayer().get().id())) : List.of(new RestTask());
    }
}
