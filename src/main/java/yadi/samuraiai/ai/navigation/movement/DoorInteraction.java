package yadi.samuraiai.ai.navigation.movement;

import yadi.samuraiai.ai.navigation.graph.NavPos;

/**
 * Door pipeline: approach, open, wait, cross, close. One instance per walker. The controller feeds it the
 * door node being approached and how far the walker is; it answers with what to do this tick.
 */
public final class DoorInteraction {
    public enum Phase { IDLE, WAITING, CROSSING }
    public enum Outcome { NONE, OPENED, WAITING, FAILED, CLOSED }

    private static final double OPEN_DISTANCE = 2.2D, CLOSE_DISTANCE = 1.4D;
    private Phase phase = Phase.IDLE;
    private NavPos door;
    private int doorIndex = -1;
    private int waitLeft;
    private boolean openedByUs;
    private NavPos lastClosed;

    public Phase phase() { return phase; }
    public NavPos door() { return door; }
    public boolean busy() { return phase != Phase.IDLE; }
    /** The door most recently closed by {@link #passed}, still readable after the interaction has reset. */
    public NavPos lastClosed() { return lastClosed; }

    /** Called while the walker's target node is a door node. */
    public Outcome approach(MovementBody body, NavPos doorPos, int index, double distance, int waitTicks) {
        if (phase == Phase.WAITING) {
            if (--waitLeft > 0) return Outcome.WAITING;
            phase = Phase.CROSSING;
            return Outcome.NONE;
        }
        if (phase == Phase.CROSSING) return Outcome.NONE;
        if (distance > OPEN_DISTANCE) return Outcome.NONE;
        door = doorPos;
        doorIndex = index;
        if (body.isDoorOpen(doorPos)) { phase = Phase.CROSSING; return Outcome.NONE; }
        if (!body.setDoor(doorPos, true)) { reset(); return Outcome.FAILED; }
        openedByUs = true;
        waitLeft = Math.max(1, waitTicks);
        phase = waitTicks > 0 ? Phase.WAITING : Phase.CROSSING;
        return Outcome.OPENED;
    }

    /** Called every tick after the door: closes it once the walker is past and clear of it. */
    public Outcome passed(MovementBody body, int currentIndex, boolean closeBehind) {
        if (phase == Phase.IDLE || door == null || currentIndex <= doorIndex) return Outcome.NONE;
        double distance = Math.hypot(body.x() - door.centerX(), body.z() - door.centerZ());
        if (distance < CLOSE_DISTANCE) return Outcome.NONE;
        Outcome result = Outcome.NONE;
        if (closeBehind && openedByUs && body.isDoorOpen(door) && body.setDoor(door, false)) { result = Outcome.CLOSED; lastClosed = door; }
        reset();
        return result;
    }

    /** Leaves a door we opened closed if the session ends while it is still open and the walker is clear of it. */
    public NavPos abandon(MovementBody body, boolean closeBehind) {
        NavPos toClose = null;
        if (door != null && openedByUs && closeBehind && body != null && body.isDoorOpen(door)) {
            double distance = Math.hypot(body.x() - door.centerX(), body.z() - door.centerZ());
            if (distance >= CLOSE_DISTANCE && body.setDoor(door, false)) toClose = door;
        }
        reset();
        return toClose;
    }

    private void reset() { phase = Phase.IDLE; door = null; doorIndex = -1; waitLeft = 0; openedByUs = false; }
}
