package yadi.samuraiai.client.voice;

import java.util.concurrent.CompletableFuture;

public interface SpeechRecognitionService extends AutoCloseable {
    record Result(boolean success, String text, String error, long latencyMillis) {
        public static Result ok(String text, long latency) { return new Result(text != null && !text.isBlank(), text == null ? "" : text.trim(), "", latency); }
        public static Result failure(String error, long latency) { return new Result(false, "", error == null ? "Error de reconocimiento" : error, latency); }
    }
    CompletableFuture<Result> recognize(byte[] wav, VoiceLanguageManager.Language language);
    String name();
    default boolean available() { return true; }
    @Override default void close() { }
}
