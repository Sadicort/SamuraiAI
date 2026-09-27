package yadi.samuraiai.ai.navigation.testkit;

import java.util.HashSet;
import java.util.Set;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.movement.MovementBody;
import yadi.samuraiai.ai.navigation.movement.SmoothRotation;
import yadi.samuraiai.ai.navigation.terrain.BlockProfile;
import yadi.samuraiai.ai.navigation.terrain.Material;

/**
 * A crude physical body over a {@link GridWorldView}: walks toward the wanted point at a fixed speed, is
 * stopped by solid blocks, steps up one block when it jumps (or over stairs/slabs), falls when unsupported,
 * climbs ladders and toggles doors. Enough physics to exercise the navigation loop faithfully.
 */
public final class SimulatedBody implements MovementBody {
    private final GridWorldView world;
    private double x, y, z;
    private float yaw;
    private boolean alive = true, sprint, sneak, frozen;
    private double wantedX, wantedY, wantedZ, factor;
    private boolean hasWanted;
    private int jumpWindow;
    private int climbDirection;
    private final Set<NavPos> openDoors = new HashSet<>();
    public int jumps, teleports;
    private final java.util.UUID entityId = java.util.UUID.randomUUID();
    @Override public java.util.UUID entityId() { return entityId; }

    public SimulatedBody(GridWorldView world, double x, double y, double z) { this.world = world; this.x = x; this.y = y; this.z = z; }

    /** A body that never moves however hard it is steered: for stuck-recovery tests. */
    public SimulatedBody frozen(boolean value) { frozen = value; return this; }
    public void kill() { alive = false; }
    public double speedPerTick() { return 0.215D; }

    public void physicsTick() {
        if (!alive) return;
        if (climbDirection != 0 && world.profile(NavPos.ofBlock(x, y + 0.01, z)).climbable()) y += 0.2D * climbDirection;
        else gravity();
        if (jumpWindow > 0) jumpWindow--;
        if (frozen || !hasWanted) return;
        double dx = wantedX - x, dz = wantedZ - z;
        double distance = Math.hypot(dx, dz);
        if (distance > 1.0E-4D) {
            yaw = SmoothRotation.approach(yaw, SmoothRotation.yawTo(x, z, wantedX, wantedZ), 40.0F);
            double step = Math.min(distance, speedPerTick() * factor);
            tryMove(x + dx / distance * step, z + dz / distance * step);
        }
        // Like a vanilla MoveControl, a steering order is honoured for one tick only: it must be renewed every tick.
        hasWanted = false;
    }

    private void tryMove(double nx, double nz) {
        int feetY = NavPos.ofBlock(x, y + 0.01, z).y();
        int bx = (int) Math.floor(nx), bz = (int) Math.floor(nz);
        if (free(bx, feetY, bz)) { x = nx; z = nz; return; }
        BlockProfile ledge = world.profile(bx, feetY, bz);
        boolean easy = ledge.material() == Material.STAIRS || ledge.material() == Material.SLAB || ledge.isLowStep();
        boolean canStep = ledge.blocksMovement() && ledge.topHeight() <= 1.01D && (jumpWindow > 0 || easy) && onGround();
        if (canStep && free(bx, feetY + 1, bz) && world.profile((int) Math.floor(x), feetY + 2, (int) Math.floor(z)).isOccupiable()) {
            y = feetY + 1; x = nx; z = nz;
            if (jumpWindow > 0) jumps++;
        }
    }

    private boolean free(int bx, int by, int bz) {
        BlockProfile feet = world.profile(bx, by, bz), head = world.profile(bx, by + 1, bz);
        return feet.isOccupiable() && head.isOccupiable() && !(feet.door() && !openDoors.contains(new NavPos(bx, by, bz)));
    }

    private void gravity() {
        NavPos block = NavPos.ofBlock(x, y + 0.01D, z);
        BlockProfile feet = world.profile(block);
        if (feet.climbable()) return;
        if (world.profile(block.offset(0, -1, 0)).canSupport()) { y = Math.max(y, block.y()); return; }
        if (feet.isLowStep()) return;
        y -= 0.5D;
    }

    @Override public double x() { return x; }
    @Override public double y() { return y; }
    @Override public double z() { return z; }
    @Override public float yaw() { return yaw; }
    @Override public boolean onGround() {
        NavPos block = NavPos.ofBlock(x, y + 0.01D, z);
        return world.profile(block.offset(0, -1, 0)).canSupport() || world.profile(block).isLowStep() || world.profile(block).climbable();
    }
    @Override public boolean inWater() { return world.profile(NavPos.ofBlock(x, y + 0.01, z)).isWater(); }
    @Override public boolean alive() { return alive; }
    @Override public void steer(double tx, double ty, double tz, double speedFactor) {
        wantedX = tx; wantedY = ty; wantedZ = tz; factor = speedFactor; hasWanted = true;
    }
    @Override public void look(double tx, double ty, double tz) { }
    public String jumpTrace = "";
    @Override public void jump() { jumpWindow = 12; jumpTrace += String.format("[x=%.2f y=%.2f %s] ", x, y, new Throwable().getStackTrace()[1]); }
    @Override public void stop() { hasWanted = false; }
    @Override public void sprint(boolean on) { sprint = on; }
    @Override public void sneak(boolean on) { sneak = on; }
    @Override public void climb(int direction) { climbDirection = direction; }
    @Override public boolean setDoor(NavPos door, boolean open) {
        BlockProfile profile = world.profile(door);
        if (!profile.door() || profile.isIronDoor()) return false;
        if (open) openDoors.add(door); else openDoors.remove(door);
        return true;
    }
    @Override public boolean isDoorOpen(NavPos door) { return openDoors.contains(door); }
    @Override public boolean teleportSafe(NavPos target) {
        x = target.centerX(); y = target.y(); z = target.centerZ(); teleports++;
        return true;
    }
    public boolean sprinting() { return sprint; }
    public boolean sneaking() { return sneak; }
    public boolean isOpen(NavPos door) { return openDoors.contains(door); }
}
