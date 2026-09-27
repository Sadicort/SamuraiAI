package yadi.samuraiai.ai.navigation.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yadi.samuraiai.Samuraiai;
import yadi.samuraiai.runtime.ServerScheduler;

/**
 * Forge hooks that feed the navigation service: the per-tick drive (END phase, after NPC brains have queued
 * their requests) and the world events that invalidate cached nodes and paths.
 */
@Mod.EventBusSubscriber(modid = Samuraiai.MODID)
public final class NavigationEvents {
    private NavigationEvents() { }

    @SubscribeEvent public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) NavigationService.getInstance().tick();
    }

    @SubscribeEvent public static void onBreak(BlockEvent.BreakEvent event) { changed(event.getLevel(), event.getPos()); }
    @SubscribeEvent public static void onPlace(BlockEvent.EntityPlaceEvent event) { changed(event.getLevel(), event.getPos()); }
    @SubscribeEvent public static void onFluid(BlockEvent.FluidPlaceBlockEvent event) { changed(event.getLevel(), event.getPos()); }

    @SubscribeEvent public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !ServerScheduler.getInstance().isRunning()) return;
        var at = event.getExplosion().getPosition();
        NavigationService.getInstance().explosion(level, at.x, at.y, at.z, event.getAffectedBlocks());
    }

    @SubscribeEvent public static void onChunkUnload(ChunkEvent.Unload event) { chunk(event.getLevel(), event.getChunk().getPos().x, event.getChunk().getPos().z); }
    @SubscribeEvent public static void onChunkLoad(ChunkEvent.Load event) { chunk(event.getLevel(), event.getChunk().getPos().x, event.getChunk().getPos().z); }

    private static void changed(net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos) {
        if (level instanceof ServerLevel server && ServerScheduler.getInstance().isServerThread()) NavigationService.getInstance().worldChanged(server, pos);
    }

    private static void chunk(net.minecraft.world.level.LevelAccessor level, int x, int z) {
        if (level instanceof ServerLevel server && ServerScheduler.getInstance().isServerThread()) NavigationService.getInstance().chunkChanged(server, x, z);
    }
}
