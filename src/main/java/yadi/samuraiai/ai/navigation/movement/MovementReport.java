package yadi.samuraiai.ai.navigation.movement;

import yadi.samuraiai.ai.navigation.graph.NavPos;

/** What one movement tick did, reported back to the navigation runtime, which owns every decision that follows. */
public record MovementReport(Status status, int index, double moved, boolean jumped, NavPos doorOpened, NavPos doorClosed,
                             double distanceToTarget) {
    public enum Status { MOVING, ARRIVED, WAITING_DOOR, DOOR_FAILED, OFF_PATH, BODY_LOST }

    public static MovementReport of(Status status, int index) { return new MovementReport(status, index, 0, false, null, null, 0); }
}
