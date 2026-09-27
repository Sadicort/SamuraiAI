package yadi.samuraiai.ai.memory.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.storage.Json;
import yadi.samuraiai.ai.memory.engine.MemoryRuntime;
import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.memory.model.Category;
import yadi.samuraiai.ai.memory.model.Chapter;
import yadi.samuraiai.ai.memory.model.Consequence;
import yadi.samuraiai.ai.memory.model.EmotionalSignature;
import yadi.samuraiai.ai.memory.model.EpisodeKind;
import yadi.samuraiai.ai.memory.model.Impression;
import yadi.samuraiai.ai.memory.model.Importance;
import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.memory.model.MemoryState;
import yadi.samuraiai.ai.memory.model.MemoryType;
import yadi.samuraiai.ai.memory.model.Origin;
import yadi.samuraiai.ai.memory.procedural.Skill;
import yadi.samuraiai.ai.memory.procedural.SkillKind;
import yadi.samuraiai.ai.memory.semantic.Aspect;
import yadi.samuraiai.ai.memory.semantic.SemanticBelief;
import yadi.samuraiai.ai.memory.spatial.LandmarkKind;
import yadi.samuraiai.ai.memory.spatial.SpatialNode;

/** Converts a memory runtime to and from its persistent JSON form. Reading is tolerant: one unreadable record is skipped, never the whole file. */
public final class MemoryCodec {
    private MemoryCodec() { }

    public static JsonObject toJson(MemoryRuntime rt) {
        JsonObject o = new JsonObject();
        o.addProperty("npc", rt.npcId().toString());
        o.add("records", Json.array(rt.all(), MemoryCodec::record));
        JsonObject spatial = new JsonObject();
        spatial.add("nodes", Json.array(rt.spatial().nodes(), MemoryCodec::node));
        if (rt.spatial().lastVisited() != null) spatial.addProperty("last", rt.spatial().lastVisited().toString());
        o.add("spatial", spatial);
        o.add("skills", Json.array(rt.procedural().all(), MemoryCodec::skill));
        o.add("beliefs", Json.array(rt.semantic().all(), MemoryCodec::belief));
        return o;
    }

    public static MemoryRuntime fromJson(UUID npc, JsonObject o, MemorySettings settings) {
        MemoryRuntime rt = new MemoryRuntime(npc, settings);
        for (MemoryRecord r : Json.list(Json.arr(o, "records"), j -> readRecord(npc, j))) rt.add(r);
        JsonObject spatial = Json.obj(o, "spatial");
        for (SpatialNode n : Json.list(Json.arr(spatial, "nodes"), MemoryCodec::readNode)) rt.spatial().put(n);
        UUID last = Json.uuid(spatial, "last");
        if (last != null) rt.spatial().lastVisited(last);
        for (Skill s : Json.list(Json.arr(o, "skills"), MemoryCodec::readSkill)) rt.procedural().put(s);
        for (SemanticBelief b : Json.list(Json.arr(o, "beliefs"), MemoryCodec::readBelief)) rt.semantic().put(b);
        return rt;
    }

    // ---- records

    private static JsonObject record(MemoryRecord r) {
        JsonObject o = new JsonObject();
        o.addProperty("id", r.id().toString()); o.addProperty("type", r.type().name()); o.addProperty("category", r.category().name());
        o.addProperty("episode", r.episode().name()); o.addProperty("kind", r.kind().name());
        o.add("stamp", Json.stamp(r.stamp())); o.addProperty("end", r.endTime());
        o.add("place", Json.place(r.place()));
        if (r.actor() != null) o.add("actor", Json.entity(r.actor()));
        if (r.target() != null) o.add("target", Json.entity(r.target()));
        o.add("entities", Json.array(r.entities(), Json::entity));
        o.add("emotion", emotion(r.emotion()));
        o.addProperty("importance", r.importance().name()); o.addProperty("confidence", r.confidence()); o.addProperty("duration", r.duration());
        o.addProperty("state", r.state().name());
        o.add("tags", Json.strings(r.tags()));
        JsonObject ctx = new JsonObject();
        r.context().forEach(ctx::addProperty);
        o.add("context", ctx);
        JsonObject origin = new JsonObject();
        origin.addProperty("source", r.origin().source()); origin.addProperty("event", r.origin().event());
        if (r.origin().traceId() != null) origin.addProperty("trace", r.origin().traceId().toString());
        o.add("origin", origin);
        o.add("events", Json.strings(r.events()));
        o.add("consequences", Json.array(r.consequences(), c -> { JsonObject j = new JsonObject(); j.addProperty("kind", c.kind()); j.addProperty("target", c.target()); j.addProperty("m", c.magnitude()); return j; }));
        o.add("chapters", Json.array(r.chapters(), c -> { JsonObject j = new JsonObject(); j.addProperty("s", c.start()); j.addProperty("e", c.end()); j.addProperty("l", c.label()); return j; }));
        o.add("impressions", Json.array(r.impressions(), i -> { JsonObject j = new JsonObject(); j.addProperty("a", i.aspect().name()); j.addProperty("d", i.delta()); return j; }));
        o.add("social", Json.uuids(r.socialLinks())); o.add("knowledge", Json.uuids(r.knowledgeLinks()));
        o.addProperty("version", r.version()); o.addProperty("strength", r.strength()); o.addProperty("weight", r.emotionalWeight());
        o.addProperty("access", r.accessCount()); o.addProperty("repeat", r.repeatCount());
        o.addProperty("lastAccess", r.lastAccess()); o.addProperty("lastDecay", r.lastDecay()); o.addProperty("lastReinforced", r.lastReinforced()); o.addProperty("lastEcho", r.lastEcho());
        o.addProperty("protected", r.isProtected()); o.addProperty("detailed", r.detailed()); o.addProperty("witnessed", r.witnessed()); o.addProperty("semantic", r.semanticApplied());
        return o;
    }

    private static MemoryRecord readRecord(UUID npc, JsonObject o) {
        UUID id = Json.uuid(o, "id");
        if (id == null) return null;
        UUID trace = Json.uuid(Json.obj(o, "origin"), "trace");
        Origin origin = new Origin(Json.str(Json.obj(o, "origin"), "source", ""), Json.str(Json.obj(o, "origin"), "event", ""), trace);
        EntityRef actor = o.has("actor") ? Json.entity(Json.obj(o, "actor")) : null;
        EntityRef target = o.has("target") ? Json.entity(Json.obj(o, "target")) : null;
        MemoryRecord r = new MemoryRecord(id, npc, Json.enumOf(o, "type", MemoryType.class, MemoryType.EPISODIC), Json.enumOf(o, "category", Category.class, Category.PERSONAL),
                Json.enumOf(o, "episode", EpisodeKind.class, EpisodeKind.OBSERVATION), Json.enumOf(o, "kind", ExperienceKind.class, ExperienceKind.MET_PERSON),
                Json.stamp(Json.obj(o, "stamp")), Json.place(Json.obj(o, "place")), actor, target, readEmotion(Json.obj(o, "emotion")),
                Json.enumOf(o, "importance", Importance.class, Importance.NORMAL), origin);
        r.endTime(Json.lng(o, "end", r.stamp().gameTime()));
        Json.list(Json.arr(o, "entities"), Json::entity).forEach(r.entities()::add);
        r.confidence(Json.num(o, "confidence", 1)); r.duration(Json.lng(o, "duration", 0));
        r.state(Json.enumOf(o, "state", MemoryState.class, MemoryState.CONSOLIDATED));
        r.tags().addAll(Json.stringList(Json.arr(o, "tags")));
        JsonObject ctx = Json.obj(o, "context");
        for (String key : ctx.keySet()) r.context().put(key, Json.str(ctx, key, ""));
        r.events().addAll(Json.stringList(Json.arr(o, "events")));
        Json.list(Json.arr(o, "consequences"), j -> new Consequence(Json.str(j, "kind", ""), Json.str(j, "target", ""), Json.num(j, "m", 0))).forEach(r.consequences()::add);
        Json.list(Json.arr(o, "chapters"), j -> new Chapter(Json.lng(j, "s", 0), Json.lng(j, "e", 0), Json.str(j, "l", ""))).forEach(r.chapters()::add);
        Json.list(Json.arr(o, "impressions"), j -> new Impression(Json.enumOf(j, "a", Aspect.class, Aspect.HELPFUL), Json.num(j, "d", 0))).forEach(r.impressions()::add);
        r.socialLinks().addAll(Json.uuidList(Json.arr(o, "social"))); r.knowledgeLinks().addAll(Json.uuidList(Json.arr(o, "knowledge")));
        r.version(Json.integer(o, "version", 1)); r.strength(Json.num(o, "strength", 1)); r.emotionalWeight(Json.num(o, "weight", 0));
        r.accessCount(Json.integer(o, "access", 0)); r.repeatCount(Json.integer(o, "repeat", 1));
        r.lastAccess(Json.lng(o, "lastAccess", 0)); r.lastDecay(Json.lng(o, "lastDecay", r.stamp().gameTime())); r.lastReinforced(Json.lng(o, "lastReinforced", 0));
        r.lastEcho(Json.lng(o, "lastEcho", Long.MIN_VALUE / 2));
        r.protect(Json.bool(o, "protected", false)); r.detailed(Json.bool(o, "detailed", true)); r.witnessed(Json.bool(o, "witnessed", false));
        r.semanticApplied(Json.bool(o, "semantic", false));
        return r;
    }

    private static JsonObject emotion(EmotionalSignature e) {
        JsonObject j = new JsonObject();
        j.addProperty("p", e.primary().name());
        j.add("s", Json.strings(e.secondary().stream().map(Enum::name).toList()));
        j.addProperty("i", e.intensity()); j.addProperty("v", e.valence()); j.addProperty("t", e.traumatic());
        return j;
    }

    private static EmotionalSignature readEmotion(JsonObject j) {
        return new EmotionalSignature(Json.enumOf(j, "p", EmotionKind.class, EmotionKind.CALM), Json.emotionList(Json.arr(j, "s")), Json.num(j, "i", 0), Json.num(j, "v", 0), Json.bool(j, "t", false));
    }

    // ---- spatial, procedural, semantic

    private static JsonObject node(SpatialNode n) {
        JsonObject j = new JsonObject();
        j.addProperty("id", n.id().toString()); j.addProperty("name", n.name()); j.addProperty("kind", n.kind().name()); j.add("place", Json.place(n.place()));
        j.addProperty("visits", n.visits()); j.addProperty("fam", n.familiarity()); j.addProperty("danger", n.danger()); j.addProperty("safety", n.safety());
        j.addProperty("last", n.lastVisit()); j.addProperty("first", n.firstVisit()); j.add("links", Json.uuids(n.connections()));
        return j;
    }

    private static SpatialNode readNode(JsonObject j) {
        UUID id = Json.uuid(j, "id");
        if (id == null) return null;
        SpatialNode n = new SpatialNode(id, Json.str(j, "name", ""), Json.enumOf(j, "kind", LandmarkKind.class, LandmarkKind.OTHER), Json.place(Json.obj(j, "place")), Json.lng(j, "first", 0));
        n.visits(Json.integer(j, "visits", 0)); n.familiarity(Json.num(j, "fam", 0)); n.danger(Json.num(j, "danger", 0)); n.safety(Json.num(j, "safety", 0)); n.lastVisit(Json.lng(j, "last", 0));
        n.connections().addAll(Json.uuidList(Json.arr(j, "links")));
        return n;
    }

    private static JsonObject skill(Skill s) {
        JsonObject j = new JsonObject();
        j.addProperty("key", s.key()); j.addProperty("kind", s.kind().name()); j.addProperty("prof", s.proficiency()); j.addProperty("uses", s.uses()); j.addProperty("last", s.lastUsed());
        j.addProperty("protected", s.isProtected()); j.add("way", Json.array(s.waypoints(), Json::place));
        return j;
    }

    private static Skill readSkill(JsonObject j) {
        String key = Json.str(j, "key", null);
        if (key == null) return null;
        Skill s = new Skill(key, Json.enumOf(j, "kind", SkillKind.class, SkillKind.ROUTE));
        s.proficiency(Json.num(j, "prof", 0)); s.uses(Json.integer(j, "uses", 0)); s.lastUsed(Json.lng(j, "last", 0)); s.protect(Json.bool(j, "protected", false));
        Json.list(Json.arr(j, "way"), Json::place).forEach(s.waypoints()::add);
        return s;
    }

    private static JsonObject belief(SemanticBelief b) {
        JsonObject j = new JsonObject();
        j.add("subject", Json.entity(b.subject())); j.addProperty("aspect", b.aspect().name()); j.addProperty("value", b.value());
        j.addProperty("confidence", b.confidence()); j.addProperty("support", b.support()); j.addProperty("updated", b.updated());
        return j;
    }

    private static SemanticBelief readBelief(JsonObject j) {
        EntityRef subject = Json.entity(Json.obj(j, "subject"));
        if (subject == null) return null;
        return new SemanticBelief(subject, Json.enumOf(j, "aspect", Aspect.class, Aspect.HELPFUL), Json.num(j, "value", 0), Json.num(j, "confidence", 0), Json.integer(j, "support", 0), Json.lng(j, "updated", 0));
    }
}
