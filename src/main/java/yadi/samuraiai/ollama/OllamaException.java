package yadi.samuraiai.ollama;

/**
 * Raised when the Ollama server is unreachable, answers with a non-200
 * status, or returns a body that is not a usable chat response.
 *
 * <p>Unchecked on purpose: every call path goes through
 * {@link yadi.samuraiai.ai.AIRequestQueue}, which already converts any
 * failure into a fallback {@code AIResponse}. Making it checked would only
 * force {@code try/catch} noise inside {@code CompletableFuture} lambdas,
 * which cannot throw checked exceptions anyway.
 */
public class OllamaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OllamaException(String message) {
        super(message);
    }

    public OllamaException(String message, Throwable cause) {
        super(message, cause);
    }
}
