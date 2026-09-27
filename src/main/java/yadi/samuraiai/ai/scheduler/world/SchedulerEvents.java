package yadi.samuraiai.ai.scheduler.world;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yadi.samuraiai.Samuraiai;

/** Forge hook that drives the scheduler once per server tick (end phase, so it sees this tick's perception). */
@Mod.EventBusSubscriber(modid = Samuraiai.MODID)
public final class SchedulerEvents {
    private SchedulerEvents() { }

    @SubscribeEvent public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) SchedulerService.getInstance().tick();
    }
}
