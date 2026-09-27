package yadi.samuraiai.ai;

public enum AIError {
    TIMEOUT("Necesito un momento más para pensar. Inténtalo de nuevo."),
    MODEL_NOT_FOUND("Hoy no encuentro las palabras. Mi voz necesita ser revisada."),
    UNAVAILABLE("Mi voz no está disponible ahora. Inténtalo más tarde."),
    QUEUE_FULL("Hay demasiadas conversaciones esperando. Vuelve en un momento."),
    HTTP("No pude terminar mi respuesta. Inténtalo de nuevo."),
    JSON("Mis pensamientos se confundieron. ¿Puedes repetirlo?"),
    CANCELLED(""),
    INTERNAL("Perdí el hilo de la conversación. ¿Puedes repetirlo?");
    private final String fallback;
    AIError(String fallback) { this.fallback = fallback; }
    public String fallback() { return fallback; }
    public static AIError classify(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause != cause.getCause()) cause = cause.getCause();
        if (cause instanceof java.util.concurrent.CancellationException) return CANCELLED;
        if (cause instanceof java.util.concurrent.TimeoutException || cause instanceof java.net.http.HttpTimeoutException) return TIMEOUT;
        if (cause instanceof com.google.gson.JsonParseException) return JSON;
        if (cause instanceof java.net.ConnectException || cause instanceof java.net.UnknownHostException) return UNAVAILABLE;
        String message = String.valueOf(error.getMessage()).toLowerCase(java.util.Locale.ROOT);
        if (message.contains("model") && (message.contains("not found") || message.contains("404"))) return MODEL_NOT_FOUND;
        if (message.contains("ilegible") || message.contains("json") || message.contains("contenido")) return JSON;
        if (message.contains("codigo") || message.contains("http")) return HTTP;
        if (message.contains("contactar")) return UNAVAILABLE;
        return INTERNAL;
    }
}
