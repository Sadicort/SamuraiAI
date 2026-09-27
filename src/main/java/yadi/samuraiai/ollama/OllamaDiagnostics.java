package yadi.samuraiai.ollama;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import yadi.samuraiai.ai.CancellableFutures;

public final class OllamaDiagnostics {
    public enum Status { AVAILABLE, UNAVAILABLE, MODEL_NOT_FOUND, HTTP_ERROR, JSON_ERROR }
    public record Result(Status status, String model, String detail) {}
    public CompletableFuture<Result> check() {
        return check(URI.create(OllamaConfig.tagsUrl()), OllamaConfig.model(), Duration.ofSeconds(OllamaConfig.connectTimeoutSeconds()));
    }
    public CompletableFuture<Result> check(URI tags, String model, Duration timeout) {
        var request = HttpRequest.newBuilder(tags).timeout(timeout).GET().build();
        return CancellableFutures.map(HttpClient.newBuilder().connectTimeout(timeout).build()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString()), (response, error) -> {
            if (error != null) return new Result(Status.UNAVAILABLE, model, error.getClass().getSimpleName());
            if (response.statusCode() != 200) return new Result(Status.HTTP_ERROR, model, "HTTP " + response.statusCode());
            try {
                JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray models = body.getAsJsonArray("models");
                if (models == null) return new Result(Status.JSON_ERROR, model, "Missing models array");
                String expected = model.contains(":") ? model : model + ":latest";
                for (JsonElement element : models) {
                    JsonObject item = element.getAsJsonObject();
                    String name = item.has("name") ? item.get("name").getAsString() : item.get("model").getAsString();
                    if (expected.equals(name)) return new Result(Status.AVAILABLE, model, "API and model ready");
                }
                return new Result(Status.MODEL_NOT_FOUND, model, "Configured model is not installed");
            } catch (RuntimeException invalid) {
                return new Result(Status.JSON_ERROR, model, "Invalid model-list JSON");
            }
        });
    }
}
