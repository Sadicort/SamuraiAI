package yadi.samuraiai.ai;

/** Technical cause is separate from player-visible text. */
public record AIResponse(boolean success, String text, String error) {
    public AIResponse { text = text == null ? "" : text; }
    public static AIResponse ok(String text) { return new AIResponse(true, text, null); }
    public static AIResponse failure(String error) { return failure(error, AIError.INTERNAL.fallback()); }
    public static AIResponse failure(String error, String fallback) { return new AIResponse(false, fallback, error); }
    public static AIResponse failure(AIError error) { return failure(error.name(), error.fallback()); }
    public boolean hasSpeakableText() { return !text.isBlank(); }
}
