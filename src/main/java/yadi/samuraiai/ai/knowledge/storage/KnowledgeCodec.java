package yadi.samuraiai.ai.knowledge.storage;

import com.google.gson.JsonObject;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.storage.Json;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeRuntime;
import yadi.samuraiai.ai.knowledge.model.AccessLevel;
import yadi.samuraiai.ai.knowledge.model.KnowledgeCategory;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;
import yadi.samuraiai.ai.knowledge.model.LearnMethod;
import yadi.samuraiai.ai.knowledge.model.Predicate;
import yadi.samuraiai.ai.knowledge.model.Revision;
import yadi.samuraiai.ai.knowledge.model.ValidationState;

/** Persistent JSON form of a knowledge runtime (an NPC's beliefs or a community's collective knowledge). Tolerant on read: an unreadable record is skipped. */
public final class KnowledgeCodec {
    private KnowledgeCodec() { }

    public static JsonObject toJson(KnowledgeRuntime rt) {
        JsonObject o = new JsonObject();
        o.addProperty("owner", rt.ownerId().toString());
        o.add("records", Json.array(rt.all(), KnowledgeCodec::record));
        o.add("rumors", Json.uuids(rt.rumors()));
        JsonObject ranks = new JsonObject();
        rt.ranks().forEach((k, v) -> ranks.addProperty(k, v.name()));
        o.add("ranks", ranks);
        return o;
    }

    public static KnowledgeRuntime fromJson(UUID owner, JsonObject o, int cellSize) {
        KnowledgeRuntime rt = new KnowledgeRuntime(owner, cellSize);
        for (KnowledgeRecord r : Json.list(Json.arr(o, "records"), j -> readRecord(owner, j))) rt.add(r);
        rt.rumors().addAll(Json.uuidList(Json.arr(o, "rumors")));
        JsonObject ranks = Json.obj(o, "ranks");
        for (String key : ranks.keySet()) rt.ranks().put(key, Json.enumOf(ranks, key, AccessLevel.class, AccessLevel.MEMBERS));
        return rt;
    }

    private static JsonObject record(KnowledgeRecord r) {
        JsonObject o = new JsonObject();
        o.addProperty("id", r.id().toString()); o.addProperty("type", r.type().name()); o.addProperty("category", r.category().name());
        o.add("subject", Json.entity(r.subject())); o.addProperty("predicate", r.predicate().name());
        if (r.object() != null) o.add("object", Json.entity(r.object()));
        JsonObject attrs = new JsonObject();
        r.attributes().forEach(attrs::addProperty);
        o.add("attributes", attrs);
        o.addProperty("origin", r.origin().name());
        if (r.source() != null) o.add("source", Json.entity(r.source()));
        o.addProperty("confidence", r.confidence()); o.addProperty("importance", r.importance());
        o.addProperty("learned", r.learned()); o.addProperty("updated", r.updated()); o.addProperty("lastDecay", r.lastDecay()); o.addProperty("lastUsed", r.lastUsed());
        o.add("place", Json.place(r.place()));
        o.add("tags", Json.strings(r.tags()));
        o.add("links", Json.uuids(r.links())); o.add("supporters", Json.uuids(r.supporters())); o.add("contradictors", Json.uuids(r.contradictors())); o.add("memories", Json.uuids(r.memoryLinks()));
        o.addProperty("state", r.state().name()); o.addProperty("access", r.access().name());
        o.add("revisions", Json.array(r.revisions(), v -> { JsonObject j = new JsonObject(); j.addProperty("t", v.at()); j.addProperty("oc", v.oldConfidence()); j.addProperty("nc", v.newConfidence());
            j.addProperty("os", v.oldState().name()); j.addProperty("ns", v.newState().name()); j.addProperty("r", v.reason()); return j; }));
        o.addProperty("version", r.version()); o.addProperty("uses", r.uses());
        o.addProperty("direct", r.directEvidence()); o.addProperty("public", r.publicEvidence()); o.addProperty("trusted", r.trustedEvidence());
        if (r.rumorId() != null) o.addProperty("rumor", r.rumorId().toString());
        if (r.traceId() != null) o.addProperty("trace", r.traceId().toString());
        return o;
    }

    private static KnowledgeRecord readRecord(UUID owner, JsonObject o) {
        UUID id = Json.uuid(o, "id");
        EntityRef subject = Json.entity(Json.obj(o, "subject"));
        if (id == null || subject == null) return null;
        EntityRef object = o.has("object") ? Json.entity(Json.obj(o, "object")) : null;
        KnowledgeRecord r = new KnowledgeRecord(id, owner, Json.enumOf(o, "type", KnowledgeType.class, KnowledgeType.FACT), Json.enumOf(o, "category", KnowledgeCategory.class, KnowledgeCategory.GENERAL),
                subject, Json.enumOf(o, "predicate", Predicate.class, Predicate.KNOWS), object, Json.enumOf(o, "origin", LearnMethod.class, LearnMethod.EXPERIENCE), Json.num(o, "confidence", 0.5), Json.lng(o, "learned", 0));
        JsonObject attrs = Json.obj(o, "attributes");
        for (String key : attrs.keySet()) r.attributes().put(key, Json.str(attrs, key, ""));
        if (o.has("source")) r.source(Json.entity(Json.obj(o, "source")));
        r.importance(Json.num(o, "importance", 0.3)); r.updated(Json.lng(o, "updated", r.learned())); r.lastDecay(Json.lng(o, "lastDecay", r.learned()));
        r.place(Json.place(Json.obj(o, "place")));
        r.tags().addAll(Json.stringList(Json.arr(o, "tags")));
        r.links().addAll(Json.uuidList(Json.arr(o, "links"))); r.supporters().addAll(Json.uuidList(Json.arr(o, "supporters")));
        r.contradictors().addAll(Json.uuidList(Json.arr(o, "contradictors"))); r.memoryLinks().addAll(Json.uuidList(Json.arr(o, "memories")));
        r.state(Json.enumOf(o, "state", ValidationState.class, ValidationState.UNKNOWN)); r.access(Json.enumOf(o, "access", AccessLevel.class, AccessLevel.PUBLIC));
        for (Revision v : Json.list(Json.arr(o, "revisions"), j -> new Revision(Json.lng(j, "t", 0), Json.num(j, "oc", 0), Json.num(j, "nc", 0), Json.enumOf(j, "os", ValidationState.class, ValidationState.UNKNOWN),
                Json.enumOf(j, "ns", ValidationState.class, ValidationState.UNKNOWN), Json.str(j, "r", "")))) r.revisions().add(v);
        r.version(Json.integer(o, "version", 1)); r.uses(Json.integer(o, "uses", 0));
        r.directEvidence(Json.bool(o, "direct", false)); r.publicEvidence(Json.bool(o, "public", false)); r.trustedEvidence(Json.bool(o, "trusted", false));
        r.rumorId(Json.uuid(o, "rumor")); r.traceId(Json.uuid(o, "trace"));
        return r;
    }
}
