package yadi.samuraiai.client.voice;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import java.util.concurrent.atomic.AtomicReference;
import yadi.samuraiai.client.voice.core.VoiceBootstrapService;
import yadi.samuraiai.client.voice.event.*;

/** Client-only coordinator. It never calls DialogueRouter or server code. */
public final class VoiceManager implements AutoCloseable {
    private static final VoiceManager INSTANCE = new VoiceManager();
    public static VoiceManager getInstance() { return INSTANCE; }
    private final VoiceRecorder recorder = new VoiceRecorder();
    private final AtomicReference<VoiceSession> session = new AtomicReference<>();
    private volatile ChatScreen screen;
    public VoiceSession session() { return session.get(); }
    public void toggle(ChatScreen target) {
        if (!VoiceConfig.get().enabled()) return;
        if (target == null) return;
        VoiceSession current = session.get();
        if (current != null && current.state() == VoiceSession.State.LISTENING) { recorder.stop(current); return; }
        start(target);
    }
    public void start(ChatScreen target) {
        if (target == null || !VoiceConfig.get().enabled()) return;
        cancel(); screen = target; VoiceSession next = new VoiceSession(); session.set(next); VoiceEventBus.post(new VoiceStartedEvent(next.id()));
        if (!VoiceBootstrapService.manager().usable()) {
            fail(next, new IllegalStateException(VoiceBootstrapService.manager().lastError()));
            return;
        }
        if (!recorder.start(next, wav -> recognize(next, wav), error -> fail(next, error))) fail(next, new IllegalStateException("No se pudo abrir el micrófono"));
    }
    private void recognize(VoiceSession current, byte[] wav) {
        if (current != session.get()) { java.util.Arrays.fill(wav, (byte) 0); return; } current.force(VoiceSession.State.PROCESSING);
        var engine = VoiceBootstrapService.manager();
        engine.engine().recognize(wav, VoiceConfig.get().language()).whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
            if (current != session.get()) return;
            if (error != null || result == null || !result.success()) { fail(current, error == null ? new IllegalStateException(result == null ? "Resultado nulo" : result.error()) : error); return; }
            if (Minecraft.getInstance().screen != screen) { cancel(); return; }
            current.text(result.text()); current.force(VoiceSession.State.SUCCESS);
            if (VoiceConfig.get().insertAutomatically()) VoiceInputController.insert(screen, result.text());
            VoiceEventBus.post(new VoiceRecognizedEvent(current.id(), result.text(), result.latencyMillis()));
            VoiceDebugLogger.info("[Voice] Reconocimiento completado: {} ms", result.latencyMillis());
            if (VoiceConfig.get().insertAutomatically() && VoiceConfig.get().sendAutomatically()) VoiceInputController.send(screen);
        }));
    }
    private void fail(VoiceSession current, Throwable error) {
        Minecraft.getInstance().execute(() -> {
            if (current != session.get()) return;
            current.error(error == null ? "Error de voz" : error.getMessage());
            current.force(VoiceSession.State.ERROR);
            VoiceEventBus.post(new VoiceErrorEvent(current.id(), current.error()));
            VoiceDebugLogger.warn("[Voice] Error de reconocimiento: {}", current.error());
        });
    }
    public void cancel() { VoiceSession current = session.getAndSet(null); if (current != null) { current.force(VoiceSession.State.CANCELLED); recorder.stop(current); VoiceEventBus.post(new VoiceStoppedEvent(current.id(), true)); } screen = null; }
    public void onScreenClosed(ChatScreen closed) { if (screen == closed) cancel(); }
    @Override public void close() { cancel(); recorder.close(); VoiceBootstrapService.manager().close(); }
}
