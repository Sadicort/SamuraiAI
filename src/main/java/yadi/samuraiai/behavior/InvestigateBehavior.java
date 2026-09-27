package yadi.samuraiai.behavior;

import yadi.samuraiai.ai.perception.engine.InvestigationTarget;
import yadi.samuraiai.context.*;
import yadi.samuraiai.goal.Goal;
import yadi.samuraiai.task.*;
import yadi.samuraiai.world.SpawnLocation;
import java.util.List;

/**
 * Goes to look at what the perception engine flagged: the last place a lost target was seen, where a sound came from (allowing
 * for how unsure the NPC is about it) or something curious. Without such evidence it falls back to facing the player it sees.
 * Arrival tolerance grows with the uncertainty of the evidence, and urgent evidence is approached at a run.
 */
public final class InvestigateBehavior implements Behavior {
    @Override public List<Task> plan(NPCContext context, WorldContext world, Goal goal) {
        if (world == null) return List.of(new RestTask());
        // No evidence of its own (an ally's alarm, a sound reported by the group) but the scheduler sent it to look: go there.
        if (world.snapshot().flatMap(s -> s.investigationTarget()).isEmpty()
                && world.advice().filter(a -> a.response() == yadi.samuraiai.ai.scheduler.personality.ResponseKind.INVESTIGATE && a.place() != null).isPresent())
            return List.of(new yadi.samuraiai.task.RoutineTask());
        var here = context.getInstance().getLocation();
        InvestigationTarget target = world.snapshot().flatMap(s -> s.investigationTarget()).orElse(null);
        if (target != null && here != null) {
            SpawnLocation destination = new SpawnLocation(here.dimensionKey(), target.x(), target.y(), target.z(), here.yaw());
            double tolerance = Math.max(1.5D, Math.min(6.0D, target.uncertainty() + 1.5D));
            double speed = target.urgency() >= 60.0D ? 1.25D : 1.0D;
            return List.of(new MoveToTask(destination, speed, tolerance, 300), new RestTask());
        }
        if (world.nearestPlayer().isEmpty()) return List.of(new RestTask());
        var observed = world.nearestPlayer().get();
        return observed.location() == null
                ? List.of(new LookAtTask(observed.id()))
                : List.of(new LookAtTask(observed.id()), new MoveToTask(observed.location(), 0.8, 1.5, 120));
    }
}
