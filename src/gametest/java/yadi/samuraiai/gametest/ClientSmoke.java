package yadi.samuraiai.gametest;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import yadi.samuraiai.client.voice.core.VoiceBootstrapService;
import yadi.samuraiai.client.voice.core.VoiceEngineState;
import yadi.samuraiai.client.voice.VoiceLanguageManager;
import java.nio.file.*;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** Opt-in development client smoke test, absent from release jar. */
@Mod.EventBusSubscriber(modid="samuraiai",value=Dist.CLIENT)
public final class ClientSmoke {
    private static int ticks;
    private static long started;
    private static CompletableFuture<yadi.samuraiai.client.voice.SpeechRecognitionService.Result> recognition;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("samuraiai.smokeClient") || event.phase != TickEvent.Phase.END) return;
        Minecraft client=Minecraft.getInstance();
        String fixture = System.getProperty("samuraiai.voiceSmokeFixture", "");
        if (!fixture.isBlank()) {
            if (started == 0) started = System.nanoTime();
            var manager = VoiceBootstrapService.manager();
            var audit = yadi.samuraiai.foundation.FoundationBootstrap.summary();
            if (audit != null && !yadi.samuraiai.foundation.FoundationBootstrap.allowed("voice"))
                throw new AssertionError("VOICE_SMOKE_FOUNDATION_BLOCKED: " + audit.foundation());
            if (manager.state() == VoiceEngineState.ERROR) throw new AssertionError("VOICE_SMOKE_FAILED: " + manager.lastError());
            if (System.nanoTime() - started > 180_000_000_000L) throw new AssertionError("VOICE_SMOKE_TIMEOUT: " + manager.state());
            if (client.screen instanceof TitleScreen && manager.usable() && recognition == null) {
                recognition = CompletableFuture.supplyAsync(() -> {
                    try { return Files.readAllBytes(Path.of(fixture)); }
                    catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
                }).thenCompose(wav -> manager.engine().recognize(wav, VoiceLanguageManager.Language.EN));
            }
            if (recognition != null && recognition.isDone()) {
                var result = recognition.join(); // Already completed: never blocks render.
                if (!result.success() || !result.text().toLowerCase(Locale.ROOT).contains("country"))
                    throw new AssertionError("VOICE_SMOKE_TRANSCRIPTION_FAILED: " + result.error());
                System.out.println("PHASE17_VOICE_TRANSCRIPTION_OK latencyMs=" + result.latencyMillis());
                client.stop();
            }
            return;
        }
        if (client.screen instanceof TitleScreen && ++ticks >= 40) {
            System.out.println("PHASE1_CLIENT_READY");
            client.stop();
        }
    }
}
