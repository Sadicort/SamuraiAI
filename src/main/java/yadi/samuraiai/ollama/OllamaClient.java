package yadi.samuraiai.ollama;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import yadi.samuraiai.ollama.model.OllamaRequest;
import yadi.samuraiai.ollama.model.OllamaResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Thin async HTTP client for Ollama's chat API.
 *
 * <p>The {@link HttpClient} is shared process-wide rather than per instance:
 * each one owns a connection pool and a selector thread, and the mod creates
 * an {@code OllamaAIService} per provider, so a per-instance client leaked
 * threads for no benefit. It is rebuilt only if the configured connect
 * timeout actually changes on a config reload.
 */
public class OllamaClient {

    /**
     * Server error bodies get echoed into logs and, indirectly, into the
     * failure message a player may see. Ollama can return a multi-kilobyte
     * stack trace, so cap what we propagate.
     */
    private static final int MAX_ERROR_BODY_CHARS = 400;

    private static final Gson GSON = new Gson();

    private static final Object CLIENT_LOCK = new Object();

    private static HttpClient sharedClient;

    private static int sharedClientConnectTimeout = -1;

    /**
     * Sends a chat request. The returned future never completes with a raw
     * transport exception: everything is normalised to {@link
     * OllamaException} so callers have a single failure type to handle.
     */
    public CompletableFuture<OllamaResponse> chatAsync(OllamaRequest request) {

        final HttpRequest httpRequest;

        try {
            httpRequest = buildChatRequest(request);
        } catch (RuntimeException e) {
            // A malformed host from the config would otherwise throw
            // synchronously and bypass the caller's future-based handling.
            return CompletableFuture.failedFuture(
                    new OllamaException("No se pudo construir la peticion a Ollama: " + e.getMessage(), e));
        }

        return yadi.samuraiai.ai.CancellableFutures.map(client()
                .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString()), (response, error) -> {

                    if (error != null) {
                        throw new OllamaException(
                                "No se pudo contactar con Ollama en " + OllamaConfig.host()
                                        + " (" + rootMessage(error) + ")", error);
                    }

                    return parse(response);
                });
    }

    private HttpRequest buildChatRequest(OllamaRequest request) {

        String json = GSON.toJson(request);

        return HttpRequest.newBuilder()
                .uri(URI.create(OllamaConfig.chatUrl()))
                .timeout(Duration.ofSeconds(OllamaConfig.requestTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    private static OllamaResponse parse(HttpResponse<String> response) {

        String body = response.body() == null ? "" : response.body();

        if (response.statusCode() != 200) {
            throw new OllamaException(
                    "Ollama respondio con codigo " + response.statusCode() + ": " + truncate(body));
        }

        OllamaResponse parsed;

        try {
            parsed = GSON.fromJson(body, OllamaResponse.class);
        } catch (JsonSyntaxException e) {
            throw new OllamaException("Respuesta de Ollama ilegible: " + truncate(body), e);
        }

        if (parsed == null) {
            throw new OllamaException("Ollama devolvio un cuerpo vacio.");
        }

        // Ollama reports model-level problems (unknown model, out of memory)
        // as a 200 with an "error" field, so status alone is not enough.
        if (parsed.getError() != null && !parsed.getError().isBlank()) {
            throw new OllamaException("Ollama devolvio un error: " + truncate(parsed.getError()));
        }

        if (parsed.getContent().isBlank()) {
            throw new OllamaException("Ollama devolvio una respuesta sin contenido.");
        }

        return parsed;
    }

    /**
     * Best-effort reachability probe used by {@code /samuraiai status}, so an
     * admin can tell "Ollama is down" apart from "the NPC had nothing to say".
     */
    public CompletableFuture<Boolean> isReachable() {

        HttpRequest probe;

        try {
            probe = HttpRequest.newBuilder()
                    .uri(URI.create(OllamaConfig.tagsUrl()))
                    .timeout(Duration.ofSeconds(OllamaConfig.connectTimeoutSeconds()))
                    .GET()
                    .build();
        } catch (RuntimeException e) {
            return CompletableFuture.completedFuture(false);
        }

        return client()
                .sendAsync(probe, HttpResponse.BodyHandlers.discarding())
                .handle((response, error) -> error == null && response.statusCode() == 200);
    }

    private static HttpClient client() {

        int connectTimeout = OllamaConfig.connectTimeoutSeconds();

        synchronized (CLIENT_LOCK) {

            if (sharedClient == null || sharedClientConnectTimeout != connectTimeout) {
                sharedClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(connectTimeout))
                        .version(HttpClient.Version.HTTP_1_1)
                        .build();
                sharedClientConnectTimeout = connectTimeout;
            }

            return sharedClient;
        }
    }

    /**
     * Async transport failures arrive wrapped in a CompletionException whose
     * own message is just the cause's class name, which is useless in a log.
     */
    private static String rootMessage(Throwable error) {

        Throwable current = error;

        while (current.getCause() != null && current.getMessage() == null) {
            current = current.getCause();
        }

        return current.getMessage() != null ? current.getMessage() : current.getClass().getSimpleName();
    }

    private static String truncate(String text) {
        return text.length() <= MAX_ERROR_BODY_CHARS
                ? text
                : text.substring(0, MAX_ERROR_BODY_CHARS) + "...";
    }
}
