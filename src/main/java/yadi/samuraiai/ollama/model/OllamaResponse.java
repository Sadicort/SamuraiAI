package yadi.samuraiai.ollama.model;

/**
 * Body of a non-streaming {@code /api/chat} reply. Only the fields the mod
 * actually reads are declared; Gson ignores the rest.
 */
public class OllamaResponse {

    private String model;

    private ChatMessage message;

    private boolean done;

    /**
     * Ollama reports model-level problems here with a 200 status, so this
     * must be checked even on an otherwise successful response.
     */
    private String error;

    public OllamaResponse() {
    }

    public String getModel() {
        return model;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public void setMessage(ChatMessage message) {
        this.message = message;
    }

    public boolean isDone() {
        return done;
    }

    public String getError() {
        return error;
    }

    /** Never null, so callers can trim/inspect without a null check. */
    public String getContent() {
        return message != null ? message.getContent() : "";
    }
}
