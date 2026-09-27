package yadi.samuraiai.living.calendar.timeline;

import com.google.gson.JsonObject;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.storage.Json;
import yadi.samuraiai.living.core.Provenance;

/**
 * One dated fact of world history. {@code scopes} say what it belongs to ({@code region:<id>}, {@code settlement:<id>},
 * {@code village:<id>}, {@code family:<id>}, {@code npc:<uuid>}, {@code player:<uuid>}, {@code lineage:<id>}...), so a village's
 * or a family's history is a query on this one chronology rather than a second copy of it. {@code significance} (0..1)
 * decides what is kept forever when the timeline is trimmed.
 */
public record TimelineEntry(UUID id, long minute, TimelineCategory category, String title, String detail, Set<String> scopes, double significance, Provenance source) {
    public TimelineEntry {
        title = title == null ? "" : title;
        detail = detail == null ? "" : detail;
        scopes = scopes == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(scopes));
        significance = Double.isFinite(significance) ? Math.max(0.0D, Math.min(1.0D, significance)) : 0.0D;
        category = category == null ? TimelineCategory.OTHER : category;
    }

    public boolean in(String scope) { return scopes.contains(scope); }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id.toString()); o.addProperty("minute", minute); o.addProperty("category", category.name());
        o.addProperty("title", title); o.addProperty("detail", detail); o.add("scopes", Json.strings(scopes)); o.addProperty("significance", significance);
        if (source != null) o.add("source", source.toJson());
        return o;
    }

    public static TimelineEntry fromJson(JsonObject o) {
        UUID id = Json.uuid(o, "id");
        if (id == null) return null;
        return new TimelineEntry(id, Json.lng(o, "minute", 0), Json.enumOf(o, "category", TimelineCategory.class, TimelineCategory.OTHER), Json.str(o, "title", ""),
                Json.str(o, "detail", ""), new LinkedHashSet<>(Json.stringList(Json.arr(o, "scopes"))), Json.num(o, "significance", 0),
                o.has("source") ? Provenance.fromJson(Json.obj(o, "source")) : null);
    }
}
