package yadi.samuraiai.ollama.model;

import java.util.Locale;
import java.util.Optional;

public enum ChatRole {

    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant");

    private final String value;

    ChatRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Parses the wire value. Empty rather than throwing, because this is fed
     * by whatever the model sent back and an unexpected role must not take
     * down the reply pipeline.
     */
    public static Optional<ChatRole> fromValue(String value) {

        if (value == null) {
            return Optional.empty();
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        for (ChatRole role : values()) {
            if (role.value.equals(normalized)) {
                return Optional.of(role);
            }
        }

        return Optional.empty();
    }
}
