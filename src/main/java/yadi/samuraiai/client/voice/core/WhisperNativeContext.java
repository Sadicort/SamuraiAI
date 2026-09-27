package yadi.samuraiai.client.voice.core;

import com.sun.jna.Pointer;
import io.github.ggerganov.whispercpp.WhisperCppJnaLibrary;
import io.github.ggerganov.whispercpp.params.WhisperFullParams;
import io.github.ggerganov.whispercpp.params.WhisperSamplingStrategy;
import java.io.IOException;

/** Owned native handles. Unlike the upstream convenience wrapper, never prints recognized text. */
final class WhisperNativeContext implements AutoCloseable {
    private final WhisperCppJnaLibrary library = WhisperCppJnaLibrary.instance;
    private Pointer context;
    private Pointer parameters;

    WhisperNativeContext(String model) throws IOException {
        context = library.whisper_init_from_file(model);
        if (context == null) throw new IOException("Whisper rechazó el modelo verificado: " + model);
    }
    WhisperFullParams getFullDefaultParams(WhisperSamplingStrategy strategy) throws IOException {
        if (parameters != null) { library.whisper_free_params(parameters); parameters = null; }
        parameters = library.whisper_full_default_params_by_ref(strategy.ordinal());
        if (parameters == null) throw new IOException("Whisper no pudo reservar parámetros");
        WhisperFullParams result = new WhisperFullParams(parameters);
        result.read();
        return result;
    }
    String fullTranscribe(WhisperFullParams params, float[] samples) throws IOException {
        if (context == null) throw new IOException("Contexto Whisper cerrado");
        if (library.whisper_full(context, params, samples, samples.length) != 0)
            throw new IOException("Whisper no pudo procesar el audio");
        StringBuilder text = new StringBuilder();
        int count = library.whisper_full_n_segments(context);
        for (int index = 0; index < count; index++) text.append(library.whisper_full_get_segment_text(context, index));
        return text.toString().trim();
    }
    @Override public void close() {
        if (parameters != null) { library.whisper_free_params(parameters); parameters = null; }
        if (context != null) { library.whisper_free(context); context = null; }
    }
}
