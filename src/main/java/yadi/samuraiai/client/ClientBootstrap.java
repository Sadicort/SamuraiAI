package yadi.samuraiai.client;

import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import yadi.samuraiai.client.voice.VoiceForgeConfig;

/** Loaded only on the physical client; integrated-server logic stays in common/server services. */
public final class ClientBootstrap {
    public static void register(FMLJavaModLoadingContext context) {
        yadi.samuraiai.foundation.thread.ThreadGuard.bindClient(() -> net.minecraft.client.Minecraft.getInstance().isSameThread());
        context.registerConfig(ModConfig.Type.CLIENT, VoiceForgeConfig.SPEC);
        context.getModEventBus().addListener(VoiceForgeConfig::onLoad);
        context.getModEventBus().addListener(VoiceForgeConfig::onReload);
    }
    private ClientBootstrap() { }
}
