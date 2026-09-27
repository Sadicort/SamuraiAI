package yadi.samuraiai.client.voice;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;

public final class VoiceOverlayRenderer {
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!VoiceConfig.get().showOverlay()) return;
        VoiceSession session = VoiceManager.getInstance().session(); if (session == null || session.state() == VoiceSession.State.IDLE) return;
        PoseStack pose = event.getPoseStack(); Minecraft mc = Minecraft.getInstance(); String label = switch (session.state()) {
            case LISTENING -> "Escuchando..."; case PROCESSING -> "Procesando..."; case SUCCESS -> "Voz lista"; case ERROR -> "Voz: " + session.error(); default -> "";
        };
        if (!label.isBlank()) { int x = 8, y = 8; GuiComponent.fill(pose, x - 4, y - 4, x + mc.font.width(label) + 12, y + 16, 0xAA111111); mc.font.draw(pose, label, x, y, 0xFFFFFF); if (session.state() == VoiceSession.State.LISTENING) GuiComponent.fill(pose, x, y + 11, x + (int)(mc.font.width(label) * session.level()), y + 13, 0xFFB8860B); }
    }
    private VoiceOverlayRenderer() {}
}
