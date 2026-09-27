package yadi.samuraiai.ai.emotion.storage;

import com.google.gson.JsonObject;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.Cause;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.storage.Json;
import yadi.samuraiai.ai.emotion.engine.EmotionRuntime;
import yadi.samuraiai.ai.emotion.model.DecayCurve;
import yadi.samuraiai.ai.emotion.model.EmotionOrigin;
import yadi.samuraiai.ai.emotion.model.EmotionPhase;
import yadi.samuraiai.ai.emotion.model.EmotionRecord;
import yadi.samuraiai.ai.emotion.model.HistoryEntry;
import yadi.samuraiai.ai.emotion.model.MoodKind;
import yadi.samuraiai.ai.emotion.model.TraumaRecord;
import yadi.samuraiai.ai.emotion.model.TriggerSource;

/** Persistent JSON form of one NPC's emotional state. Emotions that have faded are not saved; traumas, the mood and the history are. */
public final class EmotionCodec {
    private EmotionCodec() { }

    public static JsonObject toJson(EmotionRuntime rt) {
        JsonObject o = new JsonObject();
        o.addProperty("npc", rt.npcId().toString());
        o.add("active", Json.array(rt.activeRecords(), EmotionCodec::record));
        o.add("traumas", Json.array(rt.traumas(), EmotionCodec::trauma));
        o.add("history", Json.array(rt.history(), h -> { JsonObject j = new JsonObject(); j.addProperty("k", h.kind().name()); j.addProperty("p", h.peak()); j.addProperty("s", h.start());
            j.addProperty("e", h.end()); j.addProperty("src", h.source()); if (h.memoryId() != null) j.addProperty("m", h.memoryId().toString()); j.addProperty("o", h.outcome()); return j; }));
        JsonObject mood = new JsonObject();
        mood.addProperty("mood", rt.mood().name()); mood.addProperty("since", rt.moodSince());
        com.google.gson.JsonArray scores = new com.google.gson.JsonArray();
        for (double v : rt.moodScores()) scores.add(v);
        mood.add("scores", scores);
        JsonObject ticks = new JsonObject();
        rt.moodTicks().forEach((k, v) -> ticks.addProperty(k.name(), v));
        mood.add("ticks", ticks);
        o.add("mood", mood);
        o.addProperty("lastUpdate", rt.lastUpdate());
        return o;
    }

    public static EmotionRuntime fromJson(UUID npc, JsonObject o, int causeCapacity) {
        EmotionRuntime rt = new EmotionRuntime(npc);
        for (EmotionRecord r : Json.list(Json.arr(o, "active"), j -> readRecord(npc, j, causeCapacity))) rt.active().put(r.key(), r);
        for (TraumaRecord t : Json.list(Json.arr(o, "traumas"), j -> readTrauma(npc, j))) rt.traumas().add(t);
        for (HistoryEntry h : Json.list(Json.arr(o, "history"), j -> new HistoryEntry(Json.enumOf(j, "k", EmotionKind.class, EmotionKind.CALM), Json.num(j, "p", 0), Json.lng(j, "s", 0), Json.lng(j, "e", 0),
                Json.str(j, "src", ""), Json.uuid(j, "m"), Json.str(j, "o", "")))) rt.history().addLast(h);
        JsonObject mood = Json.obj(o, "mood");
        rt.mood(Json.enumOf(mood, "mood", MoodKind.class, MoodKind.NEUTRAL), Json.lng(mood, "since", 0));
        var scores = Json.arr(mood, "scores");
        for (int i = 0; i < Math.min(scores.size(), rt.moodScores().length); i++) try { rt.moodScores()[i] = scores.get(i).getAsDouble(); } catch (RuntimeException ignored) { /* keep default */ }
        JsonObject ticks = Json.obj(mood, "ticks");
        for (MoodKind k : MoodKind.values()) if (ticks.has(k.name())) rt.moodTicks().put(k, Json.lng(ticks, k.name(), 0));
        long last = Json.lng(o, "lastUpdate", Long.MIN_VALUE / 2);
        if (last > 0) rt.lastUpdate(last);
        return rt;
    }

    private static JsonObject record(EmotionRecord r) {
        JsonObject o = new JsonObject();
        o.addProperty("id", r.id().toString()); o.addProperty("kind", r.kind().name()); o.addProperty("key", r.key());
        o.addProperty("source", r.origin().source().name()); if (r.origin().memoryId() != null) o.addProperty("memory", r.origin().memoryId().toString());
        o.addProperty("ref", r.origin().ref()); if (r.origin().traceId() != null) o.addProperty("trace", r.origin().traceId().toString());
        o.addProperty("intensity", r.intensity()); o.addProperty("initial", r.initial()); o.addProperty("peak", r.peak());
        o.addProperty("phase", r.phase().name()); o.addProperty("curve", r.curve().name()); o.addProperty("half", r.customHalfLife());
        o.addProperty("created", r.created()); o.addProperty("updated", r.updated());
        o.addProperty("stability", r.stability()); o.addProperty("control", r.control()); o.addProperty("valence", r.valence()); o.addProperty("arousal", r.arousal()); o.addProperty("persistence", r.persistence());
        o.add("influences", Json.array(r.influences().list(), c -> { JsonObject j = new JsonObject(); j.addProperty("k", c.kind()); j.addProperty("r", c.ref()); j.addProperty("n", c.note()); j.addProperty("d", c.delta()); j.addProperty("t", c.at()); return j; }));
        o.add("related", Json.uuids(r.relatedEvents())); o.addProperty("version", r.version()); o.addProperty("reinforcements", r.reinforcements());
        if (r.traumaId() != null) o.addProperty("trauma", r.traumaId().toString());
        return o;
    }

    private static EmotionRecord readRecord(UUID npc, JsonObject o, int causeCapacity) {
        UUID id = Json.uuid(o, "id");
        if (id == null) return null;
        EmotionKind kind = Json.enumOf(o, "kind", EmotionKind.class, EmotionKind.CALM);
        EmotionOrigin origin = new EmotionOrigin(Json.enumOf(o, "source", TriggerSource.class, TriggerSource.ADMIN), Json.uuid(o, "memory"), Json.str(o, "ref", ""), Json.uuid(o, "trace"));
        EmotionRecord r = new EmotionRecord(id, npc, kind, origin, Json.str(o, "key", kind + "|" + id), Json.num(o, "intensity", 0), Json.lng(o, "created", 0), causeCapacity);
        r.initial(Json.num(o, "initial", r.intensity())); r.peak(Json.num(o, "peak", r.intensity()));
        r.phase(Json.enumOf(o, "phase", EmotionPhase.class, EmotionPhase.ACTIVE)); r.curve(Json.enumOf(o, "curve", DecayCurve.class, DecayCurve.EXPONENTIAL)); r.customHalfLife(Json.num(o, "half", 0));
        r.updated(Json.lng(o, "updated", r.created())); r.stability(Json.num(o, "stability", 0.5)); r.control(Json.num(o, "control", 0.5));
        r.valence(Json.num(o, "valence", kind.valence())); r.arousal(Json.num(o, "arousal", kind.arousal())); r.persistence(Json.num(o, "persistence", 0.4));
        for (Cause c : Json.list(Json.arr(o, "influences"), j -> new Cause(Json.str(j, "k", ""), Json.str(j, "r", ""), Json.str(j, "n", ""), Json.num(j, "d", 0), Json.lng(j, "t", 0)))) r.influences().add(c);
        r.relatedEvents().addAll(Json.uuidList(Json.arr(o, "related"))); r.version(Json.integer(o, "version", 1)); r.reinforcements(Json.integer(o, "reinforcements", 0));
        r.traumaId(Json.uuid(o, "trauma"));
        return r;
    }

    private static JsonObject trauma(TraumaRecord t) {
        JsonObject o = new JsonObject();
        o.addProperty("id", t.id().toString()); o.addProperty("origin", t.originKind()); o.addProperty("emotion", t.emotion().name()); o.addProperty("intensity", t.intensity());
        o.addProperty("progress", t.progress()); o.addProperty("at", t.at()); o.addProperty("last", t.lastProgress()); o.addProperty("phase", t.phase().name());
        o.add("memories", Json.uuids(t.memories())); o.add("triggers", Json.strings(t.triggers())); o.addProperty("flashbacks", t.flashbacks());
        o.addProperty("avoidance", t.avoidance()); o.addProperty("version", t.version());
        return o;
    }

    private static TraumaRecord readTrauma(UUID npc, JsonObject o) {
        UUID id = Json.uuid(o, "id");
        if (id == null) return null;
        TraumaRecord t = new TraumaRecord(id, npc, Json.str(o, "origin", ""), Json.enumOf(o, "emotion", EmotionKind.class, EmotionKind.FEAR), Json.num(o, "intensity", 0), Json.lng(o, "at", 0));
        t.progress(Json.num(o, "progress", 0)); t.lastProgress(Json.lng(o, "last", t.at())); t.phase(Json.enumOf(o, "phase", TraumaRecord.Phase.class, TraumaRecord.Phase.ACTIVE));
        t.memories().addAll(Json.uuidList(Json.arr(o, "memories"))); t.triggers().addAll(Json.stringList(Json.arr(o, "triggers")));
        t.flashbacks(Json.integer(o, "flashbacks", 0)); t.avoidance(Json.num(o, "avoidance", 0)); t.version(Json.integer(o, "version", 1));
        return t;
    }
}
