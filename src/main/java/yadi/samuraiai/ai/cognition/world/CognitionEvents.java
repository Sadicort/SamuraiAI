package yadi.samuraiai.ai.cognition.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yadi.samuraiai.Samuraiai;
import yadi.samuraiai.runtime.ServerScheduler;

/** Forge events that become experiences: a blow, a death, an explosion. Everything else the layer learns from arrives on the NPC event bus. */
@Mod.EventBusSubscriber(modid = Samuraiai.MODID)
public final class CognitionEvents {
    private CognitionEvents() { }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) CognitionService.getInstance().tick();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.getEntity().level.isClientSide || !ServerScheduler.getInstance().isRunning()) return;
        CognitionService.getInstance().onHurt(event.getEntity(), event.getSource().getEntity(), event.getAmount());
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.isCanceled() || event.getEntity().level.isClientSide || !ServerScheduler.getInstance().isRunning()) return;
        CognitionService.getInstance().onDeath(event.getEntity(), event.getSource().getEntity());
    }

    @SubscribeEvent
    public static void explosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide || !(event.getLevel() instanceof ServerLevel level) || !ServerScheduler.getInstance().isRunning()) return;
        var pos = event.getExplosion().getPosition();
        CognitionService.getInstance().onExplosion(level, pos.x, pos.y, pos.z);
    }
}
