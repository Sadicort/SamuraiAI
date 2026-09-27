package yadi.samuraiai.ai.relationship.storage;

import com.google.gson.JsonObject;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.Cause;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.storage.Json;
import yadi.samuraiai.ai.relationship.engine.RelationshipRuntime;
import yadi.samuraiai.ai.relationship.model.Dimension;
import yadi.samuraiai.ai.relationship.model.FriendshipStage;
import yadi.samuraiai.ai.relationship.model.LoyaltyKind;
import yadi.samuraiai.ai.relationship.model.PromiseRecord;
import yadi.samuraiai.ai.relationship.model.PromiseStatus;
import yadi.samuraiai.ai.relationship.model.RelationState;
import yadi.samuraiai.ai.relationship.model.RelationType;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;
import yadi.samuraiai.ai.relationship.model.ReputationLabel;
import yadi.samuraiai.ai.relationship.model.ReputationRecord;
import yadi.samuraiai.ai.relationship.model.ReputationScope;
import yadi.samuraiai.ai.relationship.model.SocialEvent;

/** Persistent JSON form of one NPC's relationship runtime (relationships, promises, reputation book). Tolerant on read. */
public final class RelationshipCodec {
    private RelationshipCodec() { }

    public static JsonObject toJson(RelationshipRuntime rt) {
        JsonObject o = new JsonObject();
        o.addProperty("npc", rt.npcId().toString());
        o.add("relationships", Json.array(rt.all(), RelationshipCodec::relationship));
        o.add("promises", Json.array(rt.promises().values(), RelationshipCodec::promise));
        o.add("reputation", Json.array(rt.reputation().all(), RelationshipCodec::reputation));
        return o;
    }

    public static RelationshipRuntime fromJson(UUID npc, JsonObject o, int causeCapacity) {
        RelationshipRuntime rt = new RelationshipRuntime(npc);
        for (RelationshipRecord r : Json.list(Json.arr(o, "relationships"), j -> readRelationship(npc, j, causeCapacity))) rt.put(r);
        for (PromiseRecord p : Json.list(Json.arr(o, "promises"), RelationshipCodec::readPromise)) rt.promises().put(p.id(), p);
        for (ReputationRecord r : Json.list(Json.arr(o, "reputation"), RelationshipCodec::readReputation)) rt.reputation().put(r);
        return rt;
    }

    private static JsonObject relationship(RelationshipRecord r) {
        JsonObject o = new JsonObject();
        o.addProperty("id", r.id().toString()); o.add("target", Json.entity(r.target())); o.addProperty("type", r.type().name()); o.addProperty("state", r.state().name());
        JsonObject values = new JsonObject();
        for (Dimension d : Dimension.values()) values.addProperty(d.name(), r.get(d));
        o.add("values", values);
        JsonObject causes = new JsonObject();
        for (Dimension d : Dimension.values()) if (r.causes(d).size() > 0) causes.add(d.name(), Json.array(r.causes(d).list(), c -> {
            JsonObject j = new JsonObject(); j.addProperty("k", c.kind()); j.addProperty("r", c.ref()); j.addProperty("n", c.note()); j.addProperty("d", c.delta()); j.addProperty("t", c.at()); return j; }));
        o.add("causes", causes);
        o.add("history", Json.array(r.history(), h -> { JsonObject j = new JsonObject(); j.addProperty("t", h.at()); j.addProperty("k", h.kind());
            if (h.memoryId() != null) j.addProperty("m", h.memoryId().toString()); j.addProperty("tr", h.trust()); j.addProperty("re", h.respect()); j.addProperty("ho", h.honor()); j.addProperty("n", h.note()); return j; }));
        o.add("memories", Json.uuids(r.memories())); o.add("events", Json.uuids(r.events()));
        o.addProperty("stage", r.stage().name()); o.addProperty("loyaltyKind", r.loyaltyKind().name()); o.addProperty("interactions", r.interactions());
        o.addProperty("positive", r.positive()); o.addProperty("negative", r.negative()); o.addProperty("oathsKept", r.oathsKept()); o.addProperty("oathsBroken", r.oathsBroken());
        o.addProperty("sharedDanger", r.sharedDanger()); o.addProperty("version", r.version()); o.addProperty("loyaltyBroken", r.loyaltyBroken());
        o.addProperty("created", r.created()); o.addProperty("lastInteraction", r.lastInteraction()); o.addProperty("lastDecay", r.lastDecay());
        return o;
    }

    private static RelationshipRecord readRelationship(UUID npc, JsonObject o, int causeCapacity) {
        UUID id = Json.uuid(o, "id");
        EntityRef target = Json.entity(Json.obj(o, "target"));
        if (id == null || target == null) return null;
        JsonObject values = Json.obj(o, "values");
        double[] initial = new double[Dimension.values().length];
        for (Dimension d : Dimension.values()) initial[d.ordinal()] = Json.num(values, d.name(), 0);
        RelationshipRecord r = new RelationshipRecord(id, npc, target, Json.lng(o, "created", 0), initial, causeCapacity);
        r.type(Json.enumOf(o, "type", RelationType.class, RelationType.STRANGER)); r.state(Json.enumOf(o, "state", RelationState.class, RelationState.ACTIVE));
        JsonObject causes = Json.obj(o, "causes");
        for (Dimension d : Dimension.values())
            for (Cause c : Json.list(Json.arr(causes, d.name()), j -> new Cause(Json.str(j, "k", ""), Json.str(j, "r", ""), Json.str(j, "n", ""), Json.num(j, "d", 0), Json.lng(j, "t", 0)))) r.causes(d).add(c);
        for (SocialEvent h : Json.list(Json.arr(o, "history"), j -> new SocialEvent(Json.lng(j, "t", 0), Json.str(j, "k", ""), Json.uuid(j, "m"), Json.num(j, "tr", 0), Json.num(j, "re", 0), Json.num(j, "ho", 0), Json.str(j, "n", "")))) r.history().add(h);
        r.memories().addAll(Json.uuidList(Json.arr(o, "memories"))); r.events().addAll(Json.uuidList(Json.arr(o, "events")));
        r.stage(Json.enumOf(o, "stage", FriendshipStage.class, FriendshipStage.STRANGER)); r.loyaltyKind(Json.enumOf(o, "loyaltyKind", LoyaltyKind.class, LoyaltyKind.PERSONAL));
        r.interactions(Json.integer(o, "interactions", 0)); r.positive(Json.integer(o, "positive", 0)); r.negative(Json.integer(o, "negative", 0));
        r.oathsKept(Json.integer(o, "oathsKept", 0)); r.oathsBroken(Json.integer(o, "oathsBroken", 0)); r.sharedDanger(Json.integer(o, "sharedDanger", 0));
        r.version(Json.integer(o, "version", 1)); r.loyaltyBroken(Json.bool(o, "loyaltyBroken", false));
        r.lastInteraction(Json.lng(o, "lastInteraction", 0)); r.lastDecay(Json.lng(o, "lastDecay", r.lastInteraction()));
        return r;
    }

    private static JsonObject promise(PromiseRecord p) {
        JsonObject j = new JsonObject();
        j.addProperty("id", p.id().toString()); j.add("promiser", Json.entity(p.promiser())); j.add("promisee", Json.entity(p.promisee())); j.addProperty("kind", p.kind().name());
        j.addProperty("subject", p.subject()); j.addProperty("made", p.madeAt()); j.addProperty("due", p.dueAt()); j.addProperty("public", p.publicPromise());
        j.addProperty("weight", p.weight()); j.addProperty("status", p.status().name()); j.addProperty("resolved", p.resolvedAt());
        return j;
    }

    private static PromiseRecord readPromise(JsonObject j) {
        UUID id = Json.uuid(j, "id");
        EntityRef a = Json.entity(Json.obj(j, "promiser")), b = Json.entity(Json.obj(j, "promisee"));
        if (id == null || a == null || b == null) return null;
        PromiseRecord p = new PromiseRecord(id, a, b, Json.enumOf(j, "kind", PromiseRecord.Kind.class, PromiseRecord.Kind.GENERIC), Json.str(j, "subject", ""), Json.lng(j, "made", 0),
                Json.lng(j, "due", 0), Json.bool(j, "public", false), Json.num(j, "weight", 0.5));
        PromiseStatus status = Json.enumOf(j, "status", PromiseStatus.class, PromiseStatus.ACTIVE);
        if (status != PromiseStatus.ACTIVE) p.resolve(status, Json.lng(j, "resolved", 0));
        return p;
    }

    private static JsonObject reputation(ReputationRecord r) {
        JsonObject j = new JsonObject();
        j.add("subject", Json.entity(r.subject())); j.addProperty("scope", r.scope().name()); j.addProperty("scopeId", r.scopeId());
        JsonObject scores = new JsonObject();
        r.scores().forEach((k, v) -> scores.addProperty(k.name(), v));
        j.add("scores", scores); j.add("sources", Json.uuids(r.sources())); j.addProperty("confidence", r.confidence()); j.addProperty("updated", r.updated());
        j.addProperty("direct", r.direct()); j.addProperty("hearsay", r.hearsay());
        return j;
    }

    private static ReputationRecord readReputation(JsonObject j) {
        EntityRef subject = Json.entity(Json.obj(j, "subject"));
        if (subject == null) return null;
        ReputationRecord r = new ReputationRecord(subject, Json.enumOf(j, "scope", ReputationScope.class, ReputationScope.LOCAL), Json.str(j, "scopeId", ""));
        JsonObject scores = Json.obj(j, "scores");
        for (String key : scores.keySet()) try { r.scores().put(ReputationLabel.valueOf(key), Json.num(scores, key, 0)); } catch (IllegalArgumentException ignored) { /* unknown label */ }
        r.sources().addAll(Json.uuidList(Json.arr(j, "sources"))); r.confidence(Json.num(j, "confidence", 0)); r.updated(Json.lng(j, "updated", 0));
        r.direct(Json.integer(j, "direct", 0)); r.hearsay(Json.integer(j, "hearsay", 0));
        return r;
    }

}
