package yadi.samuraiai.ollama;

import yadi.samuraiai.config.SamuraiSettings;

/**
 * Endpoint layout of the Ollama HTTP API. The tunable values (host, model,
 * timeouts) moved to {@link SamuraiSettings} so they can be changed from
 * the config file; only the parts fixed by Ollama's own API remain constant
 * here.
 */
public final class OllamaConfig {

    /** Chat completions endpoint. */
    public static final String CHAT_ENDPOINT = "/api/chat";

    /** Model listing endpoint, used by the availability check. */
    public static final String TAGS_ENDPOINT = "/api/tags";

    private OllamaConfig() {
    }

    public static String host() {
        return SamuraiSettings.ollamaHost();
    }

    public static String model() {
        return SamuraiSettings.ollamaModel();
    }

    public static String chatUrl() {
        return host() + CHAT_ENDPOINT;
    }

    public static String tagsUrl() {
        return host() + TAGS_ENDPOINT;
    }

    public static int connectTimeoutSeconds() {
        return SamuraiSettings.connectTimeoutSeconds();
    }

    public static int requestTimeoutSeconds() {
        return SamuraiSettings.requestTimeoutSeconds();
    }
}
