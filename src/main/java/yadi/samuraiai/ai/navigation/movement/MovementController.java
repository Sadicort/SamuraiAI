package yadi.samuraiai.ai.navigation.movement;

import yadi.samuraiai.ai.navigation.engine.NavigationSettings;
import yadi.samuraiai.ai.navigation.graph.EdgeType;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.pathfinding.NavigationPath;

/**
 * Executes a path physically, one tick at a time: chooses gait, steers toward the aim point, turns
 * progressively, jumps, climbs and runs the door pipeline. It calculates nothing strategic: routes come from
 * the pathfinding engine and every decision about success or failure belongs to the runtime.
 */
public final class MovementController {
    private static final int JUMP_COOLDOWN_TICKS = 10;

    public MovementReport tick(MovementState state, MovementBody body, NavigationPath path, MovementMode mode,
                               double finalRadius, NavigationSettings settings) {
        if (!body.alive()) return MovementReport.of(MovementReport.Status.BODY_LOST, state.index());
        state.bind(path, body.x(), body.y(), body.z());
        double moved = 0;
        if (state.hasLast) moved = Math.hypot(body.x() - state.lastX, body.z() - state.lastZ);
        state.lastX = body.x(); state.lastY = body.y(); state.lastZ = body.z(); state.hasLast = true;
        if (state.jumpCooldown > 0) state.jumpCooldown--;

        PathFollower.Step step = state.follower.next(path, body.x(), body.y(), body.z(), body.onGround(), settings.reachRadius(), finalRadius);
        NavPos doorOpened = null, doorClosed = null;

        DoorInteraction door = state.door;
        var target = path.get(step.targetIndex());
        if (target.via() == EdgeType.DOOR && !step.arrived()) {
            var outcome = door.approach(body, target.pos(), step.targetIndex(), step.distanceToTarget(), settings.doorWaitTicks());
            if (outcome == DoorInteraction.Outcome.FAILED) {
                body.stop();
                return new MovementReport(MovementReport.Status.DOOR_FAILED, step.targetIndex(), moved, false, null, null, step.distanceToTarget());
            }
            if (outcome == DoorInteraction.Outcome.OPENED) doorOpened = target.pos();
            if (door.phase() == DoorInteraction.Phase.WAITING) {
                body.stop();
                body.look(target.pos().centerX(), target.pos().y() + 1.0D, target.pos().centerZ());
                return new MovementReport(MovementReport.Status.WAITING_DOOR, step.targetIndex(), moved, false, doorOpened, null, step.distanceToTarget());
            }
        }
        if (door.busy()) {
            var closed = door.passed(body, step.targetIndex(), settings.closeDoorsBehind());
            if (closed == DoorInteraction.Outcome.CLOSED) doorClosed = door.lastClosed();
        }

        if (step.arrived()) {
            body.stop();
            body.climb(0);
            return new MovementReport(MovementReport.Status.ARRIVED, step.targetIndex(), moved, false, doorOpened, doorClosed, step.distanceToTarget());
        }
        if (step.offPath()) return new MovementReport(MovementReport.Status.OFF_PATH, step.targetIndex(), moved, false, doorOpened, doorClosed, step.distanceToTarget());

        double gait = switch (mode) {
            case WALK -> settings.walkSpeed();
            case RUN -> settings.runSpeed();
            case SPRINT -> settings.sprintSpeed();
            case SNEAK -> settings.sneakSpeed();
        };
        double aimX = step.aimX(), aimZ = step.aimZ();
        if (target.via() == EdgeType.DOOR && door.phase() == DoorInteraction.Phase.CROSSING) {
            double[] gap = body.doorPassPoint(target.pos());
            if (gap != null) { aimX = gap[0]; aimZ = gap[1]; }
        }
        float desiredYaw = SmoothRotation.yawTo(body.x(), body.z(), aimX, aimZ);
        float error = SmoothRotation.difference(body.yaw(), desiredYaw);
        double factor = gait * SmoothRotation.turnSpeedFactor(error, settings.turnSlowdown());
        if (step.precise() && step.distanceToTarget() < 1.2D) factor *= 0.8D;
        body.sprint(mode == MovementMode.SPRINT);
        body.sneak(mode == MovementMode.SNEAK);

        boolean jumped = false;
        if (step.climb() != 0) body.climb(step.climb());
        else body.climb(0);
        body.look(aimX, step.aimY() + 1.0D, aimZ);
        body.steer(aimX, step.aimY(), aimZ, factor);
        state.aimX = aimX; state.aimY = step.aimY(); state.aimZ = aimZ; state.aimFactor = factor; state.hasAim = true;
        if (step.jump() && state.jumpCooldown == 0 && body.onGround()) {
            body.jump();
            state.jumpCooldown = JUMP_COOLDOWN_TICKS;
            jumped = true;
        }
        return new MovementReport(MovementReport.Status.MOVING, step.targetIndex(), moved, jumped, doorOpened, doorClosed, step.distanceToTarget());
    }

    /**
     * Repeats the last steering order without recomputing anything. A vanilla MoveControl honours a destination for a single
     * tick, so a body that is only re-steered every few ticks (far from any player) would walk a fraction of the time; lowering
     * how often decisions are made must not lower how often the body is driven.
     */
    public void reapply(MovementState state, MovementBody body) {
        if (state.hasAim && body.alive()) body.steer(state.aimX, state.aimY, state.aimZ, state.aimFactor);
    }

    /** Halts the body without ending the session (waiting for a temporary obstacle, a chunk, recovery). */
    public void hold(MovementBody body) {
        body.stop();
        body.climb(0);
        body.sprint(false);
    }
}
