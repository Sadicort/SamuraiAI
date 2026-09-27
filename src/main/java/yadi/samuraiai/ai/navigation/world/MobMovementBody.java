package yadi.samuraiai.ai.navigation.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.movement.MovementBody;

/**
 * {@link MovementBody} over a vanilla {@link Mob}: steering through its MoveControl and JumpControl, head
 * turning through LookControl, doors through the door block API. It imports nothing from CustomNPCs, so any
 * mob-based backend can reuse it. Server-thread only.
 */
public final class MobMovementBody implements MovementBody {
    private final Mob mob;

    public MobMovementBody(Mob mob) { this.mob = mob; }

    public Mob mob() { return mob; }
    @Override public java.util.UUID entityId() { return mob.getUUID(); }
    public String dimension() { return mob.level.dimension().location().toString(); }

    @Override public double x() { return mob.getX(); }
    @Override public double y() { return mob.getY(); }
    @Override public double z() { return mob.getZ(); }
    @Override public float yaw() { return mob.getYRot(); }
    @Override public boolean onGround() { return mob.isOnGround(); }
    @Override public boolean inWater() { return mob.isInWater(); }
    @Override public boolean alive() { return mob.isAlive() && !mob.isRemoved(); }

    @Override public void steer(double x, double y, double z, double speedFactor) {
        mob.getMoveControl().setWantedPosition(x, y, z, speedFactor);
    }

    @Override public void look(double x, double y, double z) { mob.getLookControl().setLookAt(x, y, z, 30.0F, 30.0F); }
    @Override public void jump() { mob.getJumpControl().jump(); }

    @Override public void stop() {
        mob.getNavigation().stop();
        mob.setZza(0.0F);
        mob.setXxa(0.0F);
        mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0.0D);
    }

    @Override public void sprint(boolean on) { if (mob.isSprinting() != on) mob.setSprinting(on); }
    @Override public void sneak(boolean on) { if (mob.isShiftKeyDown() != on) mob.setShiftKeyDown(on); }

    @Override public void climb(int direction) {
        if (direction == 0 || !mob.onClimbable()) return;
        Vec3 motion = mob.getDeltaMovement();
        mob.setDeltaMovement(motion.x, 0.15D * direction, motion.z);
    }

    @Override public boolean setDoor(NavPos door, boolean open) {
        if (!(mob.level instanceof ServerLevel level)) return false;
        BlockPos pos = lowerHalf(level, new BlockPos(door.x(), door.y(), door.z()));
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock doorBlock) {
            if (state.is(Blocks.IRON_DOOR)) return false;
            doorBlock.setOpen(mob, level, state, pos, open);
            return level.getBlockState(pos).getValue(DoorBlock.OPEN) == open;
        }
        if (state.getBlock() instanceof FenceGateBlock) {
            if (state.getValue(FenceGateBlock.OPEN) != open) {
                level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, open), 10);
                level.levelEvent(null, open ? 1008 : 1014, pos, 0);
            }
            return true;
        }
        return false;
    }

    @Override public boolean isDoorOpen(NavPos door) {
        if (!(mob.level instanceof ServerLevel level)) return false;
        BlockPos pos = lowerHalf(level, new BlockPos(door.x(), door.y(), door.z()));
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock) return state.getValue(DoorBlock.OPEN);
        if (state.getBlock() instanceof FenceGateBlock) return state.getValue(FenceGateBlock.OPEN);
        return false;
    }

    /** Centre of the free gap of an open door: a 0.6-wide body barely fits beside the leaf if it aims at the block centre. */
    @Override public double[] doorPassPoint(NavPos door) {
        if (!(mob.level instanceof ServerLevel level)) return null;
        BlockPos pos = lowerHalf(level, new BlockPos(door.x(), door.y(), door.z()));
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock) || !state.getValue(DoorBlock.OPEN)) return null;
        var shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) return null;
        double minX = shape.min(net.minecraft.core.Direction.Axis.X), maxX = shape.max(net.minecraft.core.Direction.Axis.X);
        double minZ = shape.min(net.minecraft.core.Direction.Axis.Z), maxZ = shape.max(net.minecraft.core.Direction.Axis.Z);
        if (maxZ - minZ < maxX - minX) {
            double centre = minZ >= 1.0D - maxZ ? minZ / 2.0D : maxZ + (1.0D - maxZ) / 2.0D;
            return new double[]{pos.getX() + 0.5D, pos.getZ() + centre};
        }
        double centre = minX >= 1.0D - maxX ? minX / 2.0D : maxX + (1.0D - maxX) / 2.0D;
        return new double[]{pos.getX() + centre, pos.getZ() + 0.5D};
    }

    @Override public double width() { return mob.getBbWidth(); }

    @Override public String diagnostics() {
        var box = mob.getBoundingBox();
        Vec3 motion = mob.getDeltaMovement();
        return String.format("bb=%.2fx%.2f speed=%.3f zza=%.2f motion=(%.3f,%.3f,%.3f) hColl=%b noAi=%b effAi=%b free[E=%b W=%b S=%b N=%b] navDone=%b",
                mob.getBbWidth(), mob.getBbHeight(), mob.getSpeed(), mob.zza, motion.x, motion.y, motion.z, mob.horizontalCollision, mob.isNoAi(),
                mob.isEffectiveAi(), mob.level.noCollision(mob, box.move(0.15D, 0, 0)), mob.level.noCollision(mob, box.move(-0.15D, 0, 0)),
                mob.level.noCollision(mob, box.move(0, 0, 0.15D)), mob.level.noCollision(mob, box.move(0, 0, -0.15D)), mob.getNavigation().isDone());
    }

    @Override public boolean teleportSafe(NavPos target) {
        mob.getNavigation().stop();
        mob.teleportTo(target.centerX(), target.y(), target.centerZ());
        return true;
    }

    private static BlockPos lowerHalf(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) return pos.below();
        return pos;
    }
}
