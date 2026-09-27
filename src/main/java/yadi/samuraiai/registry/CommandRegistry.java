package yadi.samuraiai.registry;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yadi.samuraiai.Samuraiai;
import yadi.samuraiai.command.SamuraiCommand;

@Mod.EventBusSubscriber(modid = Samuraiai.MODID)
public final class CommandRegistry {

    private CommandRegistry() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SamuraiCommand.register(event.getDispatcher());
    }
}
