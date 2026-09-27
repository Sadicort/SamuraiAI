package yadi.samuraiai.ai.navigation.engine;

import yadi.samuraiai.ai.navigation.movement.MovementMode;

/**
 * How a behavior wants a journey done. Everything not stated here (profile preferences, budgets, recovery)
 * comes from the NPC's navigation profile and the configuration.
 *
 * @param timeoutTicks server ticks before the session fails with TIMEOUT
 */
public record NavigationOptions(MovementMode mode, double arrivalRadius, int timeoutTicks, boolean allowPartial,
                                String behavior, String goal) {
    public NavigationOptions {
        mode = mode == null ? MovementMode.WALK : mode;
        arrivalRadius = Double.isFinite(arrivalRadius) ? Math.max(0.5D, arrivalRadius) : 1.0D;
        timeoutTicks = Math.max(20, timeoutTicks);
        behavior = behavior == null ? "unknown" : behavior;
        goal = goal == null ? "unknown" : goal;
    }
    public static NavigationOptions walk(String behavior) {
        return new NavigationOptions(MovementMode.WALK, 1.0D, NavigationSettings.current().defaultTimeoutTicks(), true, behavior, behavior);
    }
}
