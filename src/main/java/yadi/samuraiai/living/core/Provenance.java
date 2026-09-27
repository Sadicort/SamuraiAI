package yadi.samuraiai.living.core;

import com.google.gson.JsonObject;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.storage.Json;

/**
 * Where something came from and why: the answer to "every resource must have provenance, every quest a cause, every
 * inheritance an ownership history". {@code kind} names the source's domain ({@code region-deposit}, {@code producer},
 * {@code quest}, {@code world-event}, {@code player}, {@code founding}...), {@code id} identifies it inside that domain,
 * {@code cause} is a short human reason, {@code minute} is the Deiliora minute and {@code trace} links related records.
 */
public record Provenance(String kind, String id, String cause, long minute, UUID trace) {
    public Provenance {
        kind = kind == null || kind.isBlank() ? "unknown" : kind;
        id = id == null ? "" : id;
        cause = cause == null ? "" : cause;
    }

    public static Provenance of(String kind, String id, String cause, long minute) { return new Provenance(kind, id, cause, minute, null); }

    public Provenance traced(UUID newTrace) { return new Provenance(kind, id, cause, minute, newTrace); }

    public String label() { return kind + ":" + id + (cause.isEmpty() ? "" : " (" + cause + ")"); }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("kind", kind); o.addProperty("id", id); o.addProperty("cause", cause); o.addProperty("minute", minute);
        if (trace != null) o.addProperty("trace", trace.toString());
        return o;
    }

    public static Provenance fromJson(JsonObject o) {
        return new Provenance(Json.str(o, "kind", "unknown"), Json.str(o, "id", ""), Json.str(o, "cause", ""), Json.lng(o, "minute", 0), Json.uuid(o, "trace"));
    }
}
