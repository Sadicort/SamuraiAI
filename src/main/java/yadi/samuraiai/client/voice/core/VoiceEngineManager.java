package yadi.samuraiai.client.voice.core;

import yadi.samuraiai.client.voice.*;
import yadi.samuraiai.client.voice.event.*;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import yadi.samuraiai.client.voice.util.VoiceThreadDispatcher;

public final class VoiceEngineManager implements AutoCloseable {
    private final AtomicReference<VoiceEngineState> state = new AtomicReference<>(VoiceEngineState.UNINITIALIZED);
    private final VoiceModelManager models;
    private final Installer installation;
    private final Function<VoiceEngineManager, SpeechRecognitionService> loader;
    private final Consumer<Runnable> eventDispatcher;
    private final VoiceThreadDispatcher worker = new VoiceThreadDispatcher("SamuraiAI-VoiceBootstrap");
    private volatile String lastError = "Voice Core está preparando el modelo Whisper";
    private final SpeechRecognitionService unavailable = new SpeechRecognitionService() {
        public CompletableFuture<Result> recognize(byte[] wav, VoiceLanguageManager.Language language) {
            java.util.Arrays.fill(wav, (byte) 0);
            return CompletableFuture.completedFuture(Result.failure(lastError, 0));
        }
        public String name() { return "whisper-unavailable"; }
        public boolean available() { return false; }
    };
    private volatile SpeechRecognitionService engine = unavailable;
    private volatile Path model;
    private CompletableFuture<Void> pending;
    private boolean closed;
    private long generation;
    @FunctionalInterface interface Installer { Path ensure(VoiceLanguageManager.Language language) throws Exception; }

    public VoiceEngineManager() { this(Runnable::run); }
    public VoiceEngineManager(Consumer<Runnable> eventDispatcher) {
        this.models = new VoiceModelManager();
        this.installation = new VoiceInstallationService(models)::ensure;
        this.loader = new VoiceEngineLoader()::load;
        this.eventDispatcher = eventDispatcher;
    }
    VoiceEngineManager(VoiceModelManager models, Installer installation,
                       Function<VoiceEngineManager, SpeechRecognitionService> loader, Consumer<Runnable> dispatcher) {
        this.models = models;
        this.installation = installation;
        this.loader = loader;
        this.eventDispatcher = dispatcher;
    }
    public VoiceEngineState state() { return state.get(); }
    public VoiceModelManager models() { return models; }
    public Path model() { return model; }
    public SpeechRecognitionService engine() { return engine; }
    public String lastError() { return lastError; }
    public synchronized CompletableFuture<Void> bootstrap() {
        if (closed) return CompletableFuture.failedFuture(new IllegalStateException("Voice Core cerrado"));
        if (pending != null && !pending.isDone()) return pending;
        if (!VoiceConfig.get().enabled()) { state.set(VoiceEngineState.DISABLED); return CompletableFuture.completedFuture(null); }
        if (usable()) return CompletableFuture.completedFuture(null);
        var language = VoiceConfig.get().language();
        long attempt = ++generation;
        state.set(VoiceEngineState.BOOTSTRAPPING);
        pending = worker.submit(() -> {
            SpeechRecognitionService candidate = null;
            try {
                synchronized (this) { if (closed || generation != attempt) return null; state.set(VoiceEngineState.INSTALLING); }
                Path path = installation.ensure(language);
                synchronized (this) {
                    if (closed || generation != attempt || Thread.currentThread().isInterrupted()) return null;
                    model = path;
                    state.set(VoiceEngineState.LOADING_ENGINE);
                }
                candidate = loader.apply(this);
                if (!candidate.available()) {
                    String detail = candidate instanceof EmbeddedWhisperEngine embedded ? embedded.loadError() : "Motor no disponible";
                    throw new IllegalStateException(detail);
                }
                synchronized (this) {
                    if (closed || generation != attempt || Thread.currentThread().isInterrupted()) return null;
                    engine = candidate;
                    candidate = null;
                    lastError = "";
                    state.set(VoiceEngineState.READY);
                }
                publish(new VoiceReadyEvent(VoiceEngineState.READY, path.toString()), attempt);
                VoiceDebugLogger.info("[Voice/ENGINE] Voice Core READY");
                return null;
            } catch (Exception | LinkageError failure) {
                synchronized (this) {
                    if (closed || generation != attempt) return null;
                    lastError = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
                    state.set(VoiceEngineState.ERROR);
                }
                VoiceDebugLogger.LOG.error("[Voice/BOOTSTRAP] Falló la preparación de Whisper", failure);
                publish(new VoiceErrorEvent(null, lastError), attempt);
                throw failure;
            } finally { if (candidate != null) candidate.close(); }
        });
        pending.whenComplete((ignored, failure) -> {
            synchronized (this) {
                if (failure instanceof CancellationException && !closed && generation == attempt) {
                    generation++;
                    lastError = "Preparación de Whisper cancelada";
                    state.set(VoiceEngineState.UNINITIALIZED);
                }
            }
        });
        return pending;
    }
    public synchronized CompletableFuture<Void> reload() {
        if (closed) return CompletableFuture.failedFuture(new IllegalStateException("Voice Core cerrado"));
        generation++;
        if (pending != null && !pending.isDone()) pending.cancel(true);
        SpeechRecognitionService previous = engine;
        engine = unavailable; model = null; pending = null;
        lastError = "Recargando Voice Core"; state.set(VoiceEngineState.UNINITIALIZED);
        if (previous != unavailable) previous.close();
        return bootstrap();
    }
    private void publish(Object event, long attempt) {
        eventDispatcher.accept(() -> {
            synchronized (this) { if (closed || generation != attempt) return; }
            VoiceEventBus.post(event);
        });
    }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        if (pending != null) pending.cancel(true);
        engine.close();
        engine = unavailable;
        lastError = "Voice Core cerrado";
        state.set(VoiceEngineState.DISABLED);
        worker.close();
    }
    public boolean usable() { return state() == VoiceEngineState.READY && engine.available(); }
}
