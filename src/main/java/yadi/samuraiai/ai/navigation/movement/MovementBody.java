package yadi.samuraiai.ai.navigation.movement;

import yadi.samuraiai.ai.navigation.graph.NavPos;

/**
 * The physical actuator behind an NPC: what the movement controller is allowed to ask of a body. The
 * navigation core never sees a Minecraft entity; the adapter implements this over a {@code Mob}.
 * All calls happen on the server thread.
 */
public interface MovementBody {
    double x();
    double y();
    double z();
    float yaw();
    boolean onGround();
    boolean inWater();
    boolean alive();

    /** Asks the body to head toward a point; {@code speedFactor} scales its natural walking speed. */
    void steer(double x, double y, double z, double speedFactor);
    /** Turns the head toward a point without moving. */
    void look(double x, double y, double z);
    void jump();
    void stop();
    void sprint(boolean on);
    void sneak(boolean on);
    /** Pushes the body up (+1) or down (-1) a climbable block; 0 releases it. */
    void climb(int direction);
    /** Opens or closes a door at the block position; false when the door refuses (locked, missing, not a door). */
    boolean setDoor(NavPos door, boolean open);
    boolean isDoorOpen(NavPos door);
    /**
     * Where to aim to get through an open door: the centre of the free gap beside the open leaf, which is not the
     * block centre. Null when the body cannot tell (then the block centre is used).
     */
    default double[] doorPassPoint(NavPos door) { return null; }

    /** Last-resort relocation to a validated standing node. */
    boolean teleportSafe(NavPos target);

    /**
     * UUID of the physical entity, so entity scans can ignore the walker itself. Not the NPC's identity UUID:
     * a backend's entity has its own. Null when the body is not an entity.
     */
    default java.util.UUID entityId() { return null; }

    /** Physical width in blocks, read from the entity: routing keeps half of it clear of walls. */
    default double width() { return 0.6D; }

    /** Free-form description of the body's physical state for debug traces (speed, collisions, AI flags). */
    default String diagnostics() { return ""; }

    /** Block holding the body's feet; slightly raised so a body standing exactly on a block top reads as that block. */
    default NavPos block() { return NavPos.ofBlock(x(), y() + 0.01D, z()); }
}
