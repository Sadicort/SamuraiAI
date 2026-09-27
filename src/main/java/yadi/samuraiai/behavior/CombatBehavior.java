package yadi.samuraiai.behavior;

import yadi.samuraiai.context.*;
import yadi.samuraiai.goal.Goal;
import yadi.samuraiai.task.*;
import java.util.List;

public final class CombatBehavior implements Behavior {
    @Override public List<Task> plan(NPCContext context, WorldContext world, Goal goal) {
        if (world == null || world.nearestPlayer().isEmpty()) return List.of(new RestTask());
        var target = world.nearestPlayer().get().id();
        return List.of(new LookAtTask(target), new AttackTask(target));
    }
}
