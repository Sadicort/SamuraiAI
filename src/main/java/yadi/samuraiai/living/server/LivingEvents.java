package yadi.samuraiai.living.server;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yadi.samuraiai.Samuraiai;
import yadi.samuraiai.runtime.ServerScheduler;

/** Forge events the living world needs directly: the server tick and deaths (of NPCs, and of what players kill). */
@Mod.EventBusSubscriber(modid = Samuraiai.MODID)
public final class LivingEvents {
    private LivingEvents() { }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) LivingService.getInstance().tick();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (event.isCanceled() || event.getEntity().level.isClientSide || !ServerScheduler.getInstance().isRunning()) return;
        LivingService.getInstance().onDeath(event.getEntity(), event.getSource().getEntity());
    }
}
