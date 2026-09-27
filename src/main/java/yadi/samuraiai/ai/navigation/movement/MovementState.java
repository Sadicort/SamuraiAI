package yadi.samuraiai.ai.navigation.movement;

import yadi.samuraiai.ai.navigation.pathfinding.NavigationPath;

/** Per-walker movement memory: where it is in the path, door progress and the last known position. */
public final class MovementState {
    final PathFollower follower = new PathFollower();
    final DoorInteraction door = new DoorInteraction();
    private NavigationPath bound;
    double lastX, lastY, lastZ;
    boolean hasLast;
    int jumpCooldown;
    float smoothedYaw;
    boolean yawKnown;
    /** Last steering order, kept so it can be repeated on ticks where the controller does not recompute. */
    double aimX, aimY, aimZ, aimFactor;
    boolean hasAim;

    public int index() { return follower.index(); }
    public DoorInteraction door() { return door; }
    public PathFollower follower() { return follower; }

    /** Binds a (new) path: progress restarts at the node nearest the body so a recalculated path is joined mid-way. */
    void bind(NavigationPath path, double x, double y, double z) {
        if (bound == path) return;
        bound = path;
        follower.restart(path, x, y, z);
    }

    public void forceRebind() { bound = null; }
    public void resetTracking() { hasLast = false; }
}
