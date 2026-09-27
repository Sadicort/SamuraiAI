package yadi.samuraiai.ai.cognition.storage;

import com.google.gson.JsonObject;

/** Upgrades one persistent payload from schema version {@link #from()} to {@code from() + 1}. Must be pure and never throw for well-formed data. */
public interface Migration {
    int from();
    JsonObject apply(JsonObject payload);
}
