package yadi.samuraiai.client.voice;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import yadi.samuraiai.client.voice.core.VoiceBootstrapService;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class VoiceGuiEvents {
    private static volatile ChatScreen chat;
    public static ChatScreen currentChat() { return chat; }
    @Mod.EventBusSubscriber(modid = yadi.samuraiai.Samuraiai.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        public static final KeyMapping VOICE_KEY = new KeyMapping("key.samuraiai.voice", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.samuraiai");
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(VOICE_KEY); }
        @SubscribeEvent public static void clientSetup(FMLClientSetupEvent event) { event.enqueueWork(VoiceBootstrapService::start); }
    }
    @Mod.EventBusSubscriber(modid = yadi.samuraiai.Samuraiai.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        private static final VoiceKeyModeController KEY_MODE = new VoiceKeyModeController();
        @SubscribeEvent public static void shutdown(net.minecraftforge.event.GameShuttingDownEvent event) {
            VoiceManager.getInstance().close();
        }
        @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
            Screen screen = event.getScreen(); if (!(screen instanceof ChatScreen target) || !VoiceConfig.get().enabled()) return;
            chat = target; var input = VoiceInputController.input(target); if (input != null) input.setWidth(Math.max(30, target.width - 34));
            event.addListener(new VoiceMicrophoneButton(Math.max(0, target.width - 30), target.height - 22, 26, 20));
        }
        @SubscribeEvent public static void closing(ScreenEvent.Closing event) { if (event.getScreen() == chat) { VoiceManager.getInstance().onScreenClosed(chat); chat = null; } }
        @SubscribeEvent public static void overlay(RenderGuiOverlayEvent.Post event) { VoiceOverlayRenderer.render(event); }
        @SubscribeEvent public static void key(InputEvent.Key event) {
            if (event.getAction() != GLFW.GLFW_PRESS || !(net.minecraft.client.Minecraft.getInstance().screen instanceof ChatScreen target)) return;
            if (!VoiceConfig.get().holdToTalk() && event.getKey() == ModEvents.VOICE_KEY.getKey().getValue()) VoiceManager.getInstance().toggle(target);
        }
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            boolean hold = VoiceConfig.get().holdToTalk();
            if (!(net.minecraft.client.Minecraft.getInstance().screen instanceof ChatScreen target)) { KEY_MODE.reset(); return; }
            VoiceKeyModeController.Action action = KEY_MODE.update(hold, ModEvents.VOICE_KEY.isDown());
            if (action == VoiceKeyModeController.Action.START) VoiceManager.getInstance().start(target);
            else if (action == VoiceKeyModeController.Action.STOP) {
                VoiceSession session = VoiceManager.getInstance().session();
                if (session != null && session.state() == VoiceSession.State.LISTENING) VoiceManager.getInstance().toggle(target);
            }
        }
    }
    private VoiceGuiEvents() {}
}
