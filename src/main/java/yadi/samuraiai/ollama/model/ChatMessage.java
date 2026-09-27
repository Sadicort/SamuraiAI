package yadi.samuraiai.ollama.model;

import java.util.Objects;

/**
 * One turn of an Ollama chat exchange.
 *
 * <p>Kept as a mutable JavaBean with exactly the field names Ollama expects
 * ({@code role}, {@code content}) because Gson serialises it straight onto
 * the wire; the no-arg constructor exists for deserialisation only.
 */
public class ChatMessage {

    private String role;
    private String content;

    /** Required by Gson. */
    public ChatMessage() {
    }

    public ChatMessage(ChatRole role, String content) {
        this.role = Objects.requireNonNull(role, "role").getValue();
        this.content = content == null ? "" : content;
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(ChatRole.SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(ChatRole.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(ChatRole.ASSISTANT, content);
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content == null ? "" : content;
    }

    /**
     * The role as an enum. Returns {@link ChatRole#ASSISTANT} for anything
     * unrecognised, since an unknown role only ever arrives from the model's
     * own reply and treating it as spoken-by-the-NPC is the safe reading.
     */
    public ChatRole getRoleAsEnum() {
        return ChatRole.fromValue(role).orElse(ChatRole.ASSISTANT);
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (!(other instanceof ChatMessage message)) {
            return false;
        }

        return Objects.equals(role, message.role) && Objects.equals(content, message.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, content);
    }

    @Override
    public String toString() {
        return role + ": " + getContent();
    }
}
