package yadi.samuraiai.client.voice.gui;

import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import yadi.samuraiai.client.voice.VoiceOverlayRenderer;

public final class VoiceOverlay {
    private VoiceOverlay() {}
    public static void render(RenderGuiOverlayEvent.Post event) { VoiceOverlayRenderer.render(event); }
}
