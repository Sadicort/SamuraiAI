package yadi.samuraiai.ai.knowledge.storage;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.storage.Json;
import yadi.samuraiai.ai.knowledge.culture.TraditionState;
import yadi.samuraiai.ai.knowledge.history.HistoricalEvent;
import yadi.samuraiai.ai.knowledge.history.HistoryType;
import yadi.samuraiai.ai.knowledge.model.AccessLevel;
import yadi.samuraiai.ai.knowledge.model.Predicate;
import yadi.samuraiai.ai.knowledge.rumors.RumorClaim;
import yadi.samuraiai.ai.knowledge.rumors.RumorHop;
import yadi.samuraiai.ai.knowledge.rumors.RumorRecord;
import yadi.samuraiai.ai.knowledge.rumors.RumorState;
import yadi.samuraiai.ai.knowledge.rumors.Transformation;
import yadi.samuraiai.ai.knowledge.society.Community;
import yadi.samuraiai.ai.knowledge.society.CommunityKind;
import yadi.samuraiai.ai.knowledge.society.SocietyEngine;
import yadi.samuraiai.ai.knowledge.worldmemory.Legend;

/** Persistent JSON form of the society: communities (with their collective knowledge, history, traditions, standing), rumours and legends. */
public final class SocietyCodec {
    private SocietyCodec() { }

    public static JsonObject toJson(SocietyEngine society) {
        JsonObject o = new JsonObject();
        o.add("communities", Json.array(society.communities(), SocietyCodec::community));
        o.add("rumors", Json.array(society.rumors(), SocietyCodec::rumor));
        o.add("legends", Json.array(society.worldMemory().all(), l -> { JsonObject j = new JsonObject(); j.add("subject", Json.entity(l.subject())); j.addProperty("hero", l.heroism()); j.addProperty("traitor", l.treachery()); j.addProperty("acts", l.acts()); return j; }));
        return o;
    }

    /** Replaces the society's state with the saved one. */
    public static void load(SocietyEngine society, JsonObject o, int cellSize) {
        society.reset();
        for (Community c : Json.list(Json.arr(o, "communities"), j -> readCommunity(j, cellSize))) society.put(c);
        for (RumorRecord r : Json.list(Json.arr(o, "rumors"), SocietyCodec::readRumor)) society.putRumor(r);
        for (Legend l : Json.list(Json.arr(o, "legends"), j -> { EntityRef s = Json.entity(Json.obj(j, "subject")); return s == null ? null : new Legend(s, Json.num(j, "hero", 0), Json.num(j, "traitor", 0), Json.integer(j, "acts", 0)); })) society.worldMemory().put(l);
        List<HistoricalEvent> timeline = new java.util.ArrayList<>();
        for (Community c : society.communities()) timeline.addAll(c.history());
        timeline.sort((a, b) -> Long.compare(a.at(), b.at()));
        society.loadTimeline(timeline);
    }

    private static JsonObject community(Community c) {
        JsonObject o = new JsonObject();
        o.addProperty("id", c.id()); o.addProperty("name", c.name()); o.addProperty("kind", c.kind().name()); o.addProperty("culture", c.cultureId());
        o.add("center", Json.place(c.center())); o.addProperty("radius", c.radius());
        if (c.leader() != null) o.addProperty("leader", c.leader().toString());
        JsonObject members = new JsonObject();
        c.members().forEach((k, v) -> members.addProperty(k.toString(), v.name()));
        o.add("members", members);
        o.add("collective", KnowledgeCodec.toJson(c.collective()));
        o.add("history", Json.array(c.history(), SocietyCodec::history));
        JsonObject traditions = new JsonObject();
        c.traditions().forEach((k, t) -> { JsonObject j = new JsonObject(); j.addProperty("s", t.strength()); j.addProperty("n", t.observed()); j.addProperty("last", t.lastObserved()); j.addProperty("decay", t.lastDecay()); traditions.add(k, j); });
        o.add("traditions", traditions);
        JsonObject support = new JsonObject();
        c.support().forEach((k, v) -> support.add(k, Json.uuids(v)));
        o.add("support", support);
        com.google.gson.JsonArray standing = new com.google.gson.JsonArray();
        c.standing().forEach((subject, labels) -> {
            EntityRef ref = c.standingSubjects().get(subject);
            if (ref == null) return;
            JsonObject j = new JsonObject(); j.add("subject", Json.entity(ref));
            JsonObject l = new JsonObject(); labels.forEach(l::addProperty); j.add("labels", l);
            standing.add(j);
        });
        o.add("standing", standing);
        return o;
    }

    private static Community readCommunity(JsonObject o, int cellSize) {
        String id = Json.str(o, "id", null);
        if (id == null) return null;
        Community c = new Community(id, Json.str(o, "name", id), Json.enumOf(o, "kind", CommunityKind.class, CommunityKind.VILLAGE), Json.str(o, "culture", "village"),
                Json.place(Json.obj(o, "center")), Json.num(o, "radius", 32), cellSize);
        c.leader(Json.uuid(o, "leader"));
        JsonObject members = Json.obj(o, "members");
        for (String key : members.keySet()) try { c.members().put(UUID.fromString(key), Json.enumOf(members, key, AccessLevel.class, AccessLevel.MEMBERS)); } catch (IllegalArgumentException ignored) { /* skip */ }
        var collective = KnowledgeCodec.fromJson(c.uuid(), Json.obj(o, "collective"), cellSize);
        for (var r : collective.all()) c.collective().add(r);
        for (HistoricalEvent e : Json.list(Json.arr(o, "history"), SocietyCodec::readHistory)) c.history().add(e);
        JsonObject traditions = Json.obj(o, "traditions");
        for (String key : traditions.keySet()) {
            JsonObject j = Json.obj(traditions, key);
            TraditionState t = new TraditionState(Json.num(j, "s", 0.5), Json.lng(j, "decay", 0));
            t.observed(Json.integer(j, "n", 0)); t.lastObserved(Json.lng(j, "last", 0));
            c.traditions().put(key, t);
        }
        JsonObject support = Json.obj(o, "support");
        for (String key : support.keySet()) c.support().put(key, new java.util.LinkedHashSet<>(Json.uuidList(Json.arr(support, key))));
        for (var element : Json.arr(o, "standing")) {
            if (!element.isJsonObject()) continue;
            JsonObject j = element.getAsJsonObject();
            EntityRef subject = Json.entity(Json.obj(j, "subject"));
            if (subject == null) continue;
            JsonObject labels = Json.obj(j, "labels");
            java.util.Map<String, Double> map = new java.util.LinkedHashMap<>();
            for (String key : labels.keySet()) map.put(key, Json.num(labels, key, 0));
            c.standing().put(subject.id(), map); c.standingSubjects().put(subject.id(), subject);
        }
        return c;
    }

    private static JsonObject history(HistoricalEvent e) {
        JsonObject j = new JsonObject();
        j.addProperty("id", e.id().toString()); j.addProperty("type", e.type().name()); j.addProperty("at", e.at()); j.add("place", Json.place(e.place()));
        j.add("participants", Json.array(e.participants(), Json::entity)); j.add("tags", Json.strings(e.tags())); j.addProperty("significance", e.significance());
        j.addProperty("kind", e.kind()); if (e.traceId() != null) j.addProperty("trace", e.traceId().toString()); j.addProperty("community", e.community());
        if (e.witness() != null) j.addProperty("witness", e.witness().toString());
        return j;
    }

    private static HistoricalEvent readHistory(JsonObject j) {
        UUID id = Json.uuid(j, "id");
        if (id == null) return null;
        return new HistoricalEvent(id, Json.enumOf(j, "type", HistoryType.class, HistoryType.OTHER), Json.lng(j, "at", 0), Json.place(Json.obj(j, "place")), Json.list(Json.arr(j, "participants"), Json::entity),
                new java.util.LinkedHashSet<>(Json.stringList(Json.arr(j, "tags"))), Json.num(j, "significance", 0), Json.str(j, "kind", ""), Json.uuid(j, "trace"), Json.str(j, "community", ""), Json.uuid(j, "witness"));
    }

    private static JsonObject rumor(RumorRecord r) {
        JsonObject j = new JsonObject();
        j.addProperty("id", r.id().toString()); j.add("origin", Json.entity(r.origin()));
        if (r.originMemory() != null) j.addProperty("memory", r.originMemory().toString());
        if (r.traceId() != null) j.addProperty("trace", r.traceId().toString());
        RumorClaim c = r.claim();
        JsonObject claim = new JsonObject();
        claim.add("subject", Json.entity(c.subject())); claim.addProperty("predicate", c.predicate().name());
        if (c.object() != null) claim.add("object", Json.entity(c.object()));
        claim.addProperty("label", c.label()); claim.addProperty("magnitude", c.magnitude()); claim.add("place", Json.place(c.place())); claim.addProperty("kind", c.kind());
        j.add("claim", claim);
        j.addProperty("initial", r.initialMagnitude()); j.addProperty("community", r.community()); j.addProperty("created", r.created());
        j.addProperty("state", r.state().name()); j.addProperty("strength", r.strength()); j.addProperty("lastSpread", r.lastSpread());
        j.addProperty("resolvedAt", r.resolvedAt()); j.addProperty("resolvedBy", r.resolvedBy());
        j.add("hops", Json.array(r.hops(), h -> { JsonObject x = new JsonObject(); x.addProperty("f", h.from().toString()); x.addProperty("t", h.to().toString()); x.addProperty("at", h.at()); x.addProperty("c", h.credibility()); x.addProperty("x", h.transformed()); return x; }));
        j.add("transformations", Json.array(r.transformations(), t -> { JsonObject x = new JsonObject(); x.addProperty("at", t.at()); x.addProperty("hop", t.hop()); x.addProperty("n", t.note()); x.addProperty("b", t.before()); x.addProperty("a", t.after()); return x; }));
        j.add("holders", Json.uuids(r.holders()));
        return j;
    }

    private static RumorRecord readRumor(JsonObject j) {
        UUID id = Json.uuid(j, "id");
        EntityRef origin = Json.entity(Json.obj(j, "origin"));
        JsonObject c = Json.obj(j, "claim");
        EntityRef subject = Json.entity(Json.obj(c, "subject"));
        if (id == null || origin == null || subject == null) return null;
        RumorClaim claim = new RumorClaim(subject, Json.enumOf(c, "predicate", Predicate.class, Predicate.HEARD_ABOUT), c.has("object") ? Json.entity(Json.obj(c, "object")) : null,
                Json.str(c, "label", ""), Json.num(c, "magnitude", 0), Json.place(Json.obj(c, "place")), Json.str(c, "kind", ""));
        RumorRecord r = new RumorRecord(id, origin, Json.uuid(j, "memory"), Json.uuid(j, "trace"), claim.withMagnitude(Json.num(j, "initial", claim.magnitude())), Json.str(j, "community", ""), Json.lng(j, "created", 0), Json.num(j, "strength", 1));
        r.claim(claim);
        r.state(Json.enumOf(j, "state", RumorState.class, RumorState.ACTIVE)); r.lastSpread(Json.lng(j, "lastSpread", r.created())); r.resolved(Json.lng(j, "resolvedAt", 0), Json.str(j, "resolvedBy", ""));
        for (RumorHop h : Json.list(Json.arr(j, "hops"), x -> { UUID f = Json.uuid(x, "f"), t = Json.uuid(x, "t"); return f == null || t == null ? null : new RumorHop(f, t, Json.lng(x, "at", 0), Json.num(x, "c", 0), Json.bool(x, "x", false)); })) r.hops().add(h);
        for (Transformation t : Json.list(Json.arr(j, "transformations"), x -> new Transformation(Json.lng(x, "at", 0), Json.integer(x, "hop", 0), Json.str(x, "n", ""), Json.num(x, "b", 0), Json.num(x, "a", 0)))) r.transformations().add(t);
        r.holders().addAll(Json.uuidList(Json.arr(j, "holders")));
        return r;
    }
}
