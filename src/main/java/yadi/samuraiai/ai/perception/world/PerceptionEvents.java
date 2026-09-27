package yadi.samuraiai.ai.perception.world;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yadi.samuraiai.Samuraiai;
import yadi.samuraiai.ai.perception.hearing.SoundCategory;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.runtime.ServerScheduler;

/**
 * Forge hooks that turn what happens in the world into perception inputs: the per-tick drive, sounds (block breaking and
 * placing, explosions, damage, deaths, projectiles, doors) and damage reports for NPCs that were hurt. They only record
 * facts; each NPC decides for itself whether it heard or felt them.
 */
@Mod.EventBusSubscriber(modid = Samuraiai.MODID)
public final class PerceptionEvents {
    private PerceptionEvents() { }

    private static PerceptionService service() { return PerceptionService.getInstance(); }
    private static boolean active(LevelAccessor level) { return level instanceof ServerLevel && ServerScheduler.getInstance().isServerThread() && service().enabled(); }

    @SubscribeEvent public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) service().tick();
    }

    @SubscribeEvent public static void onBreak(BlockEvent.BreakEvent event) {
        if (!active(event.getLevel())) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        UUID who = event.getPlayer() == null ? null : event.getPlayer().getUUID();
        service().sound(level, SoundCategory.BLOCK_BREAK, who, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0.8D);
        service().blockChanged(level, pos);
    }

    @SubscribeEvent public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!active(event.getLevel())) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        UUID who = event.getEntity() == null ? null : event.getEntity().getUUID();
        service().sound(level, SoundCategory.BLOCK_PLACE, who, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0.5D);
        service().blockChanged(level, pos);
    }

    @SubscribeEvent public static void onFluid(BlockEvent.FluidPlaceBlockEvent event) {
        if (active(event.getLevel())) service().blockChanged((ServerLevel) event.getLevel(), event.getPos());
    }

    @SubscribeEvent public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!active(event.getLevel())) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        Vec3 at = event.getExplosion().getPosition();
        UUID who = event.getExplosion().getExploder() == null ? null : event.getExplosion().getExploder().getUUID();
        service().sound(level, SoundCategory.EXPLOSION, who, at.x, at.y, at.z, 1.0D);
        int marked = 0;
        for (BlockPos pos : event.getAffectedBlocks()) { if (marked++ >= 64) break; service().blockChanged(level, pos); }
    }

    @SubscribeEvent public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level.isClientSide || !ServerScheduler.getInstance().isServerThread() || !service().enabled()) return;
        ServerLevel level = (ServerLevel) victim.level;
        service().sound(level, SoundCategory.DAMAGE, victim.getUUID(), victim.getX(), victim.getY(), victim.getZ(), 0.6D);
        var attacker = event.getSource().getEntity();
        for (var npc : NPCManager.getInstance().getActive()) {
            if (!npc.getController().ownsEntity(npc.getInstance(), victim.getUUID())) continue;
            service().damage(npc.getId(), attacker == null ? null : attacker.getUUID(), attacker == null ? event.getSource().getMsgId() : attacker.getName().getString(),
                    attacker == null ? Double.NaN : attacker.getX(), attacker == null ? Double.NaN : attacker.getY(), attacker == null ? Double.NaN : attacker.getZ(), event.getAmount());
        }
    }

    @SubscribeEvent public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level.isClientSide || !ServerScheduler.getInstance().isServerThread() || !service().enabled()) return;
        service().sound((ServerLevel) victim.level, SoundCategory.IMPACT, victim.getUUID(), victim.getX(), victim.getY(), victim.getZ(), 0.7D);
    }

    @SubscribeEvent public static void onProjectile(ProjectileImpactEvent event) {
        var projectile = event.getProjectile();
        if (projectile.level.isClientSide || !ServerScheduler.getInstance().isServerThread() || !service().enabled()) return;
        Vec3 at = event.getRayTraceResult().getLocation();
        UUID owner = projectile.getOwner() == null ? null : projectile.getOwner().getUUID();
        service().sound((ServerLevel) projectile.level, SoundCategory.PROJECTILE, owner, at.x, at.y, at.z, 0.5D);
    }

    @SubscribeEvent public static void onInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!active(event.getLevel())) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        var block = level.getBlockState(event.getPos()).getBlock();
        if (!(block instanceof DoorBlock || block instanceof FenceGateBlock || block instanceof TrapDoorBlock)) return;
        BlockPos pos = event.getPos();
        service().sound(level, SoundCategory.DOOR, event.getEntity().getUUID(), pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0.55D);
        service().blockChanged(level, pos);
    }
}
