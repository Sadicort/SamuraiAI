package yadi.samuraiai.ai.scheduler.interrupt;

import yadi.samuraiai.ai.scheduler.routine.RoutineInstance;

/** A routine that was interrupted, with the policy that governs how (and for how long) it may be resumed. */
public record InterruptFrame(RoutineInstance instance, InterruptPolicy policy, String by, long at, long expiresAt) {
    public boolean expired(long now) { return now >= expiresAt; }
}
