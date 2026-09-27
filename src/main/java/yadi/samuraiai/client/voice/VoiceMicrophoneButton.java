package yadi.samuraiai.client.voice;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class VoiceMicrophoneButton extends Button {
    public VoiceMicrophoneButton(int x, int y, int width, int height) { super(x, y, width, height, Component.literal("Mic"), button -> {}); }
    @Override public void onPress() {
        var chat = findChat();
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown())
            net.minecraft.client.Minecraft.getInstance().setScreen(new yadi.samuraiai.client.voice.gui.VoiceSettingsScreen(chat));
        else VoiceManager.getInstance().toggle(chat);
    }
    private net.minecraft.client.gui.screens.ChatScreen findChat() { return VoiceGuiEvents.currentChat(); }
    @Override public void renderButton(com.mojang.blaze3d.vertex.PoseStack pose, int mouseX, int mouseY, float partialTick) {
        VoiceSession session = VoiceManager.getInstance().session();
        String icon = session == null ? "Mic" : switch (session.state()) {
            case LISTENING -> "■"; case PROCESSING -> "…"; case SUCCESS -> "✓"; case ERROR -> "!"; case CANCELLED, IDLE -> "Mic";
        };
        setMessage(Component.literal(icon));
        super.renderButton(pose, mouseX, mouseY, partialTick);
    }
}
