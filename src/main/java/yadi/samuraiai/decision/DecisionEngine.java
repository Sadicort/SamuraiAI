package yadi.samuraiai.decision;

import yadi.samuraiai.context.NPCContext;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.goal.Goal;

import java.util.List;

public interface DecisionEngine {

    Goal decide(NPCContext context, WorldContext world, List<Goal> candidates);

}
