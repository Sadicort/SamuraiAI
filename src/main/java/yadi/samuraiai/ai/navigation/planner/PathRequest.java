package yadi.samuraiai.ai.navigation.planner;

import java.util.UUID;
import yadi.samuraiai.ai.navigation.graph.NavPos;

/**
 * A behavior's ask: "get this NPC there". Says nothing about how; routing, validation and recovery
 * are the navigation engine's business.
 *
 * @param arrivalRadius horizontal distance at which the walker counts as arrived
 * @param timeoutTicks  hard deadline for the whole session, including recalculations
 * @param allowPartial  accept a path that gets closer but does not reach the goal (then keep replanning)
 */
public record PathRequest(UUID npcId, String dimension, NavPos start, NavPos goal, PathPreferences prefs,
                          String behavior, String goalName, double arrivalRadius, int timeoutTicks, boolean allowPartial) {
    public PathRequest {
        if (npcId == null || dimension == null || start == null || goal == null)
            throw new IllegalArgumentException("npc, dimension, start and goal required");
        prefs = prefs == null ? PathPreferences.defaults() : prefs;
        behavior = behavior == null ? "unknown" : behavior;
        goalName = goalName == null ? "unknown" : goalName;
        arrivalRadius = Double.isFinite(arrivalRadius) ? Math.max(0.5D, arrivalRadius) : 1.0D;
        timeoutTicks = Math.max(20, timeoutTicks);
    }
    public PathRequest withStart(NavPos value) {
        return new PathRequest(npcId, dimension, value, goal, prefs, behavior, goalName, arrivalRadius, timeoutTicks, allowPartial);
    }
    public PathRequest withGoal(NavPos value) {
        return new PathRequest(npcId, dimension, start, value, prefs, behavior, goalName, arrivalRadius, timeoutTicks, allowPartial);
    }
    public PathRequest withPrefs(PathPreferences value) {
        return new PathRequest(npcId, dimension, start, goal, value, behavior, goalName, arrivalRadius, timeoutTicks, allowPartial);
    }
}
