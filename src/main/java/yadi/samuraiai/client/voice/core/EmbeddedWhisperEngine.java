package yadi.samuraiai.client.voice.core;

import io.github.ggerganov.whispercpp.params.WhisperFullParams;
import io.github.ggerganov.whispercpp.params.WhisperSamplingStrategy;
import yadi.samuraiai.client.voice.SpeechRecognitionService;
import yadi.samuraiai.client.voice.VoiceLanguageManager;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import yadi.samuraiai.client.voice.util.VoiceThreadDispatcher;

/** Local Whisper inference through the Java/JNA binding bundled by Forge. */
public final class EmbeddedWhisperEngine implements SpeechRecognitionService, AutoCloseable {
    private static final AudioFormat TARGET = new AudioFormat(16_000f, 16, 1, true, false);
    private final VoiceEngineManager manager;
    private final VoiceThreadDispatcher worker = new VoiceThreadDispatcher("SamuraiAI-Whisper");
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean recognizing = new AtomicBoolean();
    private volatile CompletableFuture<Result> active = CompletableFuture.completedFuture(Result.failure("Sin sesión", 0));
    private final WhisperNativeContext whisper;
    private final String loadError;

    public EmbeddedWhisperEngine(VoiceEngineManager manager) {
        this.manager = manager;
        WhisperNativeContext loaded = null;
        String error = null;
        try {
            if (manager.model() == null || !Files.isRegularFile(manager.model())) {
                error = "Modelo Whisper no instalado";
            } else {
                loaded = new WhisperNativeContext(manager.model().toAbsolutePath().toString());
            }
        } catch (Exception | LinkageError failure) {
            error = "No se pudo cargar Whisper: " + (failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage());
            if (loaded != null) try { loaded.close(); } catch (Throwable ignored) { }
            loaded = null;
        }
        whisper = loaded;
        loadError = error;
    }

    @Override public boolean available() { return !closed.get() && whisper != null && loadError == null; }
    public String loadError() { return loadError == null ? "Whisper no disponible" : loadError; }

    @Override public synchronized CompletableFuture<Result> recognize(byte[] wav, VoiceLanguageManager.Language language) {
        if (!available()) {
            Arrays.fill(wav, (byte) 0);
            String message = manager.state() == VoiceEngineState.ERROR
                    ? (loadError == null ? "Whisper no disponible" : loadError)
                    : "Voice Core aún está preparando el modelo Whisper";
            return CompletableFuture.completedFuture(Result.failure(message, 0));
        }
        if (!recognizing.compareAndSet(false, true)) {
            Arrays.fill(wav, (byte) 0);
            return CompletableFuture.completedFuture(Result.failure("Whisper está procesando otra grabación", 0));
        }
        long started = System.nanoTime();
        CompletableFuture<Result> submitted = worker.submit(() -> {
            float[] samples = null;
            try {
                if (closed.get()) return Result.failure("Whisper cerrado", elapsed(started));
                samples = pcmSamples(wav);
                if (samples.length == 0) return Result.failure("No se capturó audio", elapsed(started));
                WhisperFullParams params = whisper.getFullDefaultParams(WhisperSamplingStrategy.WHISPER_SAMPLING_GREEDY);
                params.n_threads = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() / 2));
                params.language = language == null || language == VoiceLanguageManager.Language.AUTO ? null : VoiceLanguageManager.tag(language);
                params.detectLanguage(language == null || language == VoiceLanguageManager.Language.AUTO);
                params.transcribeMode();
                params.enableContext(false);
                params.singleSegment(true);
                params.printProgress(false);
                params.printRealtime(false);
                params.printTimestamps(false);
                params.printSpecial(false);
                params.write();
                String text;
                synchronized (whisper) { text = whisper.fullTranscribe(params, samples); }
                return Result.ok(text, elapsed(started));
            } catch (Exception | LinkageError failure) {
                return Result.failure("Error Whisper: " + (failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage()), elapsed(started));
            } finally { if (samples != null) Arrays.fill(samples, 0f); }
        }).whenComplete((result, error) -> { Arrays.fill(wav, (byte) 0); recognizing.set(false); });
        active = submitted;
        return submitted;
    }

    private static float[] pcmSamples(byte[] wav) throws Exception {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav));
             AudioInputStream converted = source.getFormat().matches(TARGET) ? source : AudioSystem.getAudioInputStream(TARGET, source)) {
            byte[] pcm = converted.readAllBytes();
            ByteBuffer bytes = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN);
            float[] samples = new float[pcm.length / 2];
            for (int i = 0; i < samples.length; i++) samples[i] = bytes.getShort() / 32768f;
            Arrays.fill(pcm, (byte) 0);
            return samples;
        }
    }

    private static long elapsed(long started) { return (System.nanoTime() - started) / 1_000_000L; }
    @Override public String name() { return "embedded-whispercpp"; }
    @Override public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        // Native inference is not safely preemptible. Release context only after its active call finishes.
        active.handle((result, error) -> null).thenCompose(ignored ->
                worker.submit(() -> { if (whisper != null) whisper.close(); return null; }))
                .exceptionally(error -> {
                    yadi.samuraiai.client.voice.VoiceDebugLogger.LOG.error("[Voice/ENGINE] Error cerrando contexto nativo", error);
                    return null;
                }).whenComplete((ignored, error) -> worker.close());
    }
}
