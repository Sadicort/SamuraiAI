package yadi.samuraiai.ollama.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Payload for {@code /api/chat}.
 *
 * <p>{@code stream} stays false: {@link yadi.samuraiai.ollama.OllamaClient}
 * parses the body as a single JSON object, and a streaming reply would
 * arrive as newline-delimited JSON that Gson cannot read in one pass.
 */
public class OllamaRequest {

    private String model;

    private List<ChatMessage> messages = new ArrayList<>();

    private boolean stream = false;

    private OllamaOptions options;

    public OllamaRequest(String model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    /** Read-only view; use {@link #addMessage} to extend the conversation. */
    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void addMessage(ChatMessage message) {
        messages.add(Objects.requireNonNull(message, "message"));
    }

    public void addMessages(Collection<ChatMessage> toAdd) {
        for (ChatMessage message : toAdd) {
            addMessage(message);
        }
    }

    public void addSystemMessage(String message) {
        addMessage(ChatMessage.system(message));
    }

    public void addUserMessage(String message) {
        addMessage(ChatMessage.user(message));
    }

    public void addAssistantMessage(String message) {
        addMessage(ChatMessage.assistant(message));
    }

    public boolean isStream() {
        return stream;
    }

    public OllamaOptions getOptions() {
        return options;
    }

    public void setOptions(OllamaOptions options) {
        this.options = options;
    }
}
