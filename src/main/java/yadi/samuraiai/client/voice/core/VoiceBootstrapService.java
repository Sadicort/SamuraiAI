package yadi.samuraiai.client.voice.core;

import yadi.samuraiai.client.voice.*;

public final class VoiceBootstrapService {
    private static final VoiceEngineManager MANAGER = new VoiceEngineManager(
            task -> net.minecraft.client.Minecraft.getInstance().execute(task));
    public static VoiceEngineManager manager() { return MANAGER; }
    public static void start() {
        if (!yadi.samuraiai.foundation.FoundationBootstrap.allowed("voice")) {
            VoiceDebugLogger.warn("[Voice/BOOTSTRAP] Foundation deshabilitó Voice; revisar el reporte de inicio");
            return;
        }
        MANAGER.bootstrap();
    }
    private VoiceBootstrapService() {}
}
