package yadi.samuraiai.config;

import java.net.URI;
import java.util.Locale;
import yadi.samuraiai.logging.SamuraiLogger;

/** One immutable snapshot is published atomically on config load/reload. */
public final class SamuraiSettings {
    private SamuraiSettings() {}
    public record Values(String ollamaHost,
            String ollamaModel,
            int connectTimeoutSeconds,
            int requestTimeoutSeconds,
            int maxConcurrentRequests,
            int aiTimeoutSeconds,
            int queueSize,
            int queueWaitMillis,
            int dialogueQueueSize,
            int maxMessageLength,
            int memoryMessages,
            int responseLength,
            int promptLimit,
            int playerCooldownMillis,
            int npcCooldownMillis,
            int conversationCooldownMillis,
            int maxActiveNpcs,
            int brainTickInterval,
            double perceptionRadius,
            double chatRadius,
            boolean answerPublicChat,
            boolean emotionDecay) {
        public Values {
            ollamaHost = normalizeHost(ollamaHost);
            ollamaModel = ollamaModel == null || ollamaModel.isBlank() ? "llama3.2:latest" : ollamaModel.trim();
            connectTimeoutSeconds = Math.max(1, Math.min(120, connectTimeoutSeconds));
            requestTimeoutSeconds = Math.max(1, Math.min(600, requestTimeoutSeconds));
            maxConcurrentRequests = Math.max(1, Math.min(64, maxConcurrentRequests));
            aiTimeoutSeconds = Math.max(1, Math.min(600, aiTimeoutSeconds));
            queueSize = Math.max(1, Math.min(10000, queueSize));
            queueWaitMillis = Math.max(1, Math.min(600000, queueWaitMillis));
            dialogueQueueSize = Math.max(1, Math.min(256, dialogueQueueSize));
            maxMessageLength = Math.max(16, Math.min(8000, maxMessageLength));
            memoryMessages = Math.max(2, Math.min(200, memoryMessages));
            responseLength = Math.max(32, Math.min(2000, responseLength));
            promptLimit = Math.max(1000, Math.min(100000, promptLimit));
            playerCooldownMillis = Math.max(0, Math.min(60000, playerCooldownMillis));
            npcCooldownMillis = Math.max(0, Math.min(60000, npcCooldownMillis));
            conversationCooldownMillis = Math.max(0, Math.min(60000, conversationCooldownMillis));
            maxActiveNpcs = Math.max(1, Math.min(1000, maxActiveNpcs));
            brainTickInterval = Math.max(1, Math.min(1200, brainTickInterval));
            perceptionRadius = Double.isFinite(perceptionRadius) ? Math.max(1, Math.min(256, perceptionRadius)) : 24D;
            chatRadius = Double.isFinite(chatRadius) ? Math.max(1, Math.min(256, chatRadius)) : 12D;
        }
    }
    private static final Values DEFAULTS = new Values("http://127.0.0.1:11434", "llama3.2:latest", 10, 60, 4, 30, 128, 60000, 16, 500, 20, 300, 16000, 3000, 0, 0, 50, 20, 24, 12, true, true);
    private static volatile Values values = DEFAULTS;
    public static Values defaults() { return DEFAULTS; }
    public static Values snapshot() { return values; }
    public static void apply(Values next) { values = java.util.Objects.requireNonNull(next); }
    public static String ollamaHost() { return values.ollamaHost(); }
    public static String ollamaModel() { return values.ollamaModel(); }
    public static int connectTimeoutSeconds() { return values.connectTimeoutSeconds(); }
    public static int requestTimeoutSeconds() { return values.requestTimeoutSeconds(); }
    public static int maxConcurrentRequests() { return values.maxConcurrentRequests(); }
    public static int aiTimeoutSeconds() { return values.aiTimeoutSeconds(); }
    public static int queueSize() { return values.queueSize(); }
    public static int queueWaitMillis() { return values.queueWaitMillis(); }
    public static int dialogueQueueSize() { return values.dialogueQueueSize(); }
    public static int maxMessageLength() { return values.maxMessageLength(); }
    public static int memoryMessages() { return values.memoryMessages(); }
    public static int responseLength() { return values.responseLength(); }
    public static int promptLimit() { return values.promptLimit(); }
    public static int playerCooldownMillis() { return values.playerCooldownMillis(); }
    public static int npcCooldownMillis() { return values.npcCooldownMillis(); }
    public static int conversationCooldownMillis() { return values.conversationCooldownMillis(); }
    public static int maxActiveNpcs() { return values.maxActiveNpcs(); }
    public static int brainTickInterval() { return values.brainTickInterval(); }
    public static double perceptionRadius() { return values.perceptionRadius(); }
    public static double chatRadius() { return values.chatRadius(); }
    public static boolean answerPublicChat() { return values.answerPublicChat(); }
    public static boolean emotionDecay() { return values.emotionDecay(); }
    public static String normalizeHost(String raw) {
        String host = raw == null || raw.isBlank() ? "http://127.0.0.1:11434" : raw.trim();
        if (!host.contains("://")) host = "http://" + host;
        try {
            URI uri = URI.create(host);
            if (!java.util.Set.of("http", "https").contains(uri.getScheme().toLowerCase(Locale.ROOT))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getFragment() != null || uri.getPort() == 0 || uri.getPort() > 65535
                    || !(uri.getPath().isEmpty() || uri.getPath().equals("/")))
                throw new IllegalArgumentException("Expected HTTP(S) origin without credentials/path");
            return host.endsWith("/") ? host.substring(0, host.length()-1) : host;
        } catch (IllegalArgumentException error) {
            SamuraiLogger.CONFIG.warn("Invalid Ollama origin; using loopback");
            return "http://127.0.0.1:11434";
        }
    }
}
