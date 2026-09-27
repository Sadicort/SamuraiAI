package yadi.samuraiai.client.voice;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import java.lang.reflect.Field;

/** Bridges a recognized string into ChatScreen's existing input field. */
public final class VoiceInputController {
    private static final Field INPUT;
    static {
        Field field;
        try { field = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(ChatScreen.class, "f_95573_"); }
        catch (RuntimeException error) {
            VoiceDebugLogger.LOG.error("[Voice/GUI] No se pudo resolver el campo de chat", error);
            field = null;
        }
        INPUT = field;
    }
    public static void insert(ChatScreen screen, String text) { if (screen == null || text == null || text.isBlank()) return; EditBox input = input(screen); if (input == null) return; input.setValue(text.trim()); input.setCursorPosition(text.trim().length()); }
    public static void send(ChatScreen screen) { if (screen == null) return; EditBox input = input(screen); if (input != null && !input.getValue().isBlank()) screen.handleChatInput(input.getValue(), false); }
    public static EditBox input(ChatScreen screen) { try { return INPUT == null ? null : (EditBox) INPUT.get(screen); } catch (IllegalAccessException e) { return null; } }
    private VoiceInputController() {}
}
