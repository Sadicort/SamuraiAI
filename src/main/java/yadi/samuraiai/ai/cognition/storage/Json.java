package yadi.samuraiai.ai.cognition.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.EntityKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.cognition.model.Stamp;

/** Tolerant JSON readers and writers for the persistent formats: a missing or mistyped field yields the default, never an exception. */
public final class Json {
    private Json() { }

    public static String str(JsonObject o, String key, String def) {
        JsonElement e = o == null ? null : o.get(key);
        return e == null || e.isJsonNull() || !e.isJsonPrimitive() ? def : e.getAsString();
    }
    public static double num(JsonObject o, String key, double def) {
        JsonElement e = o == null ? null : o.get(key);
        try { return e == null || e.isJsonNull() ? def : e.getAsDouble(); } catch (RuntimeException x) { return def; }
    }
    public static long lng(JsonObject o, String key, long def) {
        JsonElement e = o == null ? null : o.get(key);
        try { return e == null || e.isJsonNull() ? def : e.getAsLong(); } catch (RuntimeException x) { return def; }
    }
    public static int integer(JsonObject o, String key, int def) { return (int) lng(o, key, def); }
    public static boolean bool(JsonObject o, String key, boolean def) {
        JsonElement e = o == null ? null : o.get(key);
        try { return e == null || e.isJsonNull() ? def : e.getAsBoolean(); } catch (RuntimeException x) { return def; }
    }
    public static UUID uuid(JsonObject o, String key) {
        String s = str(o, key, null);
        if (s == null) return null;
        try { return UUID.fromString(s); } catch (IllegalArgumentException x) { return null; }
    }
    public static JsonObject obj(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
    }
    public static JsonArray arr(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : new JsonArray();
    }
    public static <E extends Enum<E>> E enumOf(JsonObject o, String key, Class<E> type, E def) {
        String s = str(o, key, null);
        if (s == null) return def;
        try { return Enum.valueOf(type, s.toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException x) { return def; }
    }

    public static <T> JsonArray array(Iterable<T> items, Function<T, JsonElement> mapper) {
        JsonArray array = new JsonArray();
        for (T item : items) array.add(mapper.apply(item));
        return array;
    }
    public static <T> List<T> list(JsonArray array, Function<JsonObject, T> mapper) {
        List<T> result = new ArrayList<>();
        for (JsonElement e : array) {
            if (!e.isJsonObject()) continue;
            try { T item = mapper.apply(e.getAsJsonObject()); if (item != null) result.add(item); } catch (RuntimeException ignored) { /* one bad entry never loses the rest */ }
        }
        return result;
    }
    public static JsonArray strings(Iterable<String> values) {
        JsonArray array = new JsonArray();
        for (String v : values) array.add(v);
        return array;
    }
    public static List<String> stringList(JsonArray array) {
        List<String> result = new ArrayList<>();
        for (JsonElement e : array) if (e.isJsonPrimitive()) result.add(e.getAsString());
        return result;
    }
    public static JsonArray uuids(Iterable<UUID> values) {
        JsonArray array = new JsonArray();
        for (UUID v : values) array.add(v.toString());
        return array;
    }
    public static List<UUID> uuidList(JsonArray array) {
        List<UUID> result = new ArrayList<>();
        for (JsonElement e : array) {
            try { result.add(UUID.fromString(e.getAsString())); } catch (RuntimeException ignored) { /* skip */ }
        }
        return result;
    }
    public static List<EmotionKind> emotionList(JsonArray array) {
        List<EmotionKind> result = new ArrayList<>();
        for (String s : stringList(array)) EmotionKind.parse(s).ifPresent(result::add);
        return result;
    }

    // ---- shared value types

    public static JsonObject entity(EntityRef e) {
        JsonObject o = new JsonObject();
        o.addProperty("id", e.id().toString()); o.addProperty("kind", e.kind().name()); o.addProperty("name", e.name());
        return o;
    }
    public static EntityRef entity(JsonObject o) {
        UUID id = uuid(o, "id");
        return id == null ? null : new EntityRef(id, enumOf(o, "kind", EntityKind.class, EntityKind.UNKNOWN), str(o, "name", ""));
    }
    public static JsonObject place(PlaceRef p) {
        JsonObject o = new JsonObject();
        o.addProperty("dim", p.dimension()); o.addProperty("x", p.x()); o.addProperty("y", p.y()); o.addProperty("z", p.z()); o.addProperty("zone", p.zone());
        return o;
    }
    public static PlaceRef place(JsonObject o) { return new PlaceRef(str(o, "dim", ""), num(o, "x", 0), num(o, "y", 0), num(o, "z", 0), str(o, "zone", "")); }
    public static JsonObject stamp(Stamp s) {
        JsonObject o = new JsonObject();
        o.addProperty("t", s.gameTime()); o.addProperty("real", s.realMillis()); o.addProperty("weather", s.weather());
        return o;
    }
    public static Stamp stamp(JsonObject o) { return new Stamp(lng(o, "t", 0), lng(o, "real", 0), str(o, "weather", "")); }
}
