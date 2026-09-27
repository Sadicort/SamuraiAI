package yadi.samuraiai.living.quest.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.storage.Json;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.core.persistence.StoreSection;
import yadi.samuraiai.living.quest.branching.BranchSpec;
import yadi.samuraiai.living.quest.branching.Path;
import yadi.samuraiai.living.quest.campaigns.Campaign;
import yadi.samuraiai.living.quest.conditions.ConditionKind;
import yadi.samuraiai.living.quest.consequences.ConsequenceSpec;
import yadi.samuraiai.living.quest.engine.QuestEngine;
import yadi.samuraiai.living.quest.memory.QuestHistory;
import yadi.samuraiai.living.quest.objectives.ObjectiveType;
import yadi.samuraiai.living.quest.objectives.QuestObjective;
import yadi.samuraiai.living.quest.rewards.RewardSpec;
import yadi.samuraiai.living.quest.runtime.Quest;
import yadi.samuraiai.living.quest.templates.QuestTemplate;

/** The quests, campaigns, cooldowns and history, persisted as one versioned file. */
public final class QuestStorage {
    public static final int SCHEMA = 1;

    private QuestStorage() { }

    public static List<StoreSection> sections(QuestEngine e) { return List.of(new Quests(e)); }

    static final class Quests implements StoreSection {
        private final QuestEngine e;
        Quests(QuestEngine e) { this.e = e; }
        @Override public String domain() { return "quests"; }
        @Override public String file() { return "quest/quests.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        @Override public boolean dirty() { return e.dirty(); }
        @Override public void clean() { e.clean(); }

        private static String uid(UUID u) { return u == null ? null : u.toString(); }

        private static JsonObject quest(Quest q) {
            JsonObject o = new JsonObject();
            o.addProperty("id", q.id().toString()); o.addProperty("template", q.template()); o.addProperty("category", q.category().name()); o.addProperty("title", q.title());
            JsonObject story = new JsonObject(); q.story().forEach((k, v) -> story.addProperty(k.name(), v)); o.add("story", story);
            o.addProperty("stage", q.stage().name()); o.addProperty("state", q.state().name()); o.addProperty("originKind", q.originKind().name()); o.addProperty("originKey", q.originKey());
            o.addProperty("severity", q.severity()); if (q.cause() != null) o.add("cause", q.cause().toJson());
            o.addProperty("giver", q.giver().toString()); o.addProperty("giverName", q.giverName()); o.addProperty("giverProfession", q.giverProfession());
            if (q.settlement() != null) o.addProperty("settlement", q.settlement().toString()); if (q.region() != null) o.addProperty("region", q.region().toString());
            JsonArray objectives = new JsonArray();
            for (QuestObjective x : q.objectives()) {
                JsonObject j = new JsonObject();
                j.addProperty("id", x.id().toString()); j.addProperty("type", x.type().name()); j.addProperty("text", x.description()); j.addProperty("target", x.target());
                j.addProperty("dim", x.dimension()); j.addProperty("x", Double.isNaN(x.x()) ? 0 : x.x()); j.addProperty("y", Double.isNaN(x.y()) ? 0 : x.y()); j.addProperty("z", Double.isNaN(x.z()) ? 0 : x.z());
                j.addProperty("placed", x.placed()); j.addProperty("radius", x.radius()); j.addProperty("required", x.required()); j.addProperty("progress", x.progress());
                j.addProperty("state", x.state().name()); j.addProperty("optional", x.optional()); if (x.branch() != null) j.addProperty("branch", x.branch().name());
                objectives.add(j);
            }
            o.add("objectives", objectives);
            JsonArray branches = new JsonArray();
            for (BranchSpec b : q.branches()) { JsonObject j = new JsonObject(); j.addProperty("path", b.path().name()); j.addProperty("label", b.label()); j.addProperty("trust", b.minTrust()); j.addProperty("standing", b.minStanding()); j.addProperty("factor", b.rewardFactor()); branches.add(j); }
            o.add("branches", branches);
            if (q.path() != null) o.addProperty("path", q.path().name());
            JsonArray rewards = new JsonArray();
            for (RewardSpec r : q.rewards()) { JsonObject j = new JsonObject(); j.addProperty("kind", r.kind().name()); j.addProperty("amount", r.amount()); j.addProperty("target", r.target()); j.addProperty("text", r.text()); rewards.add(j); }
            o.add("rewards", rewards);
            JsonArray cons = new JsonArray();
            for (ConsequenceSpec c : q.consequences()) {
                JsonObject j = new JsonObject();
                j.addProperty("kind", c.kind().name()); j.addProperty("target", c.target()); j.addProperty("amount", c.amount()); j.addProperty("text", c.text());
                j.addProperty("success", c.onSuccess()); j.addProperty("failure", c.onFailure()); if (c.branch() != null) j.addProperty("branch", c.branch().name());
                cons.add(j);
            }
            o.add("consequences", cons);
            JsonObject vars = new JsonObject(); q.variables().forEach(vars::addProperty); o.add("vars", vars);
            o.addProperty("created", q.created()); o.addProperty("offeredUntil", q.offeredUntil()); o.addProperty("accepted", q.acceptedAt()); o.addProperty("deadline", q.deadline());
            o.addProperty("ended", q.endedAt()); o.addProperty("days", q.durationDays());
            JsonObject players = new JsonObject(); q.players().forEach((k, v) -> players.addProperty(k.toString(), v)); o.add("players", players);
            JsonArray decisions = new JsonArray();
            for (Quest.Decision d : q.decisions()) { JsonObject j = new JsonObject(); j.addProperty("m", d.minute()); j.addProperty("p", uid(d.player())); j.addProperty("c", d.choice()); decisions.add(j); }
            o.add("decisions", decisions);
            o.add("applied", Json.strings(q.applied())); o.add("given", Json.strings(q.given())); o.add("log", Json.strings(q.log())); o.add("tags", Json.strings(q.tags()));
            if (q.campaign() != null) { o.addProperty("campaign", q.campaign().toString()); o.addProperty("campaignStage", q.campaignStage()); }
            if (q.parent() != null) o.addProperty("parent", q.parent().toString());
            if (q.reservedFor() != null) o.addProperty("reservedFor", q.reservedFor().toString());
            o.addProperty("merged", q.merged());
            return o;
        }

        private static Quest quest(JsonObject o) {
            UUID id = Json.uuid(o, "id"), giver = Json.uuid(o, "giver");
            if (id == null || giver == null) return null;
            Quest q = new Quest(id, Json.str(o, "template", ""), Json.enumOf(o, "category", QuestTemplate.Category.class, QuestTemplate.Category.STORY), Json.str(o, "title", ""),
                    Json.enumOf(o, "originKind", ConditionKind.class, ConditionKind.CUSTOM), Json.str(o, "originKey", ""), Json.num(o, "severity", 0.5),
                    o.has("cause") ? Provenance.fromJson(Json.obj(o, "cause")) : null, giver, Json.str(o, "giverName", ""), Json.str(o, "giverProfession", ""),
                    Json.uuid(o, "settlement"), Json.uuid(o, "region"), Json.lng(o, "created", 0), Json.lng(o, "offeredUntil", 0), Json.integer(o, "days", 5));
            JsonObject story = Json.obj(o, "story");
            for (String k : story.keySet()) try { q.story().put(QuestTemplate.StoryStage.valueOf(k), Json.str(story, k, "")); } catch (IllegalArgumentException ignored) { }
            for (JsonElement el : Json.arr(o, "objectives")) if (el.isJsonObject()) {
                JsonObject j = el.getAsJsonObject();
                boolean placed = Json.bool(j, "placed", false);
                QuestObjective x = new QuestObjective(Json.uuid(j, "id"), Json.enumOf(j, "type", ObjectiveType.class, ObjectiveType.TALK), Json.str(j, "text", ""), Json.str(j, "target", ""),
                        Json.str(j, "dim", ""), placed ? Json.num(j, "x", 0) : Double.NaN, placed ? Json.num(j, "y", 0) : Double.NaN, placed ? Json.num(j, "z", 0) : Double.NaN,
                        Json.num(j, "radius", 24), Json.num(j, "required", 1), Json.bool(j, "optional", false), Path.parse(Json.str(j, "branch", null)).orElse(null));
                x.restore(Json.num(j, "progress", 0), Json.enumOf(j, "state", QuestObjective.State.class, QuestObjective.State.PENDING));
                q.objectives().add(x);
            }
            for (JsonElement el : Json.arr(o, "branches")) if (el.isJsonObject()) {
                JsonObject j = el.getAsJsonObject();
                Path.parse(Json.str(j, "path", "")).ifPresent(p -> q.branches().add(new BranchSpec(p, Json.str(j, "label", ""), Json.num(j, "trust", 0), Json.num(j, "standing", -1), Json.num(j, "factor", 1))));
            }
            Path.parse(Json.str(o, "path", null)).ifPresent(q::path);
            for (JsonElement el : Json.arr(o, "rewards")) if (el.isJsonObject()) {
                JsonObject j = el.getAsJsonObject();
                q.rewards().add(new RewardSpec(Json.enumOf(j, "kind", RewardSpec.Kind.class, RewardSpec.Kind.COINS), Json.str(j, "amount", "0"), Json.str(j, "target", ""), Json.str(j, "text", "")));
            }
            for (JsonElement el : Json.arr(o, "consequences")) if (el.isJsonObject()) {
                JsonObject j = el.getAsJsonObject();
                q.consequences().add(new ConsequenceSpec(Json.enumOf(j, "kind", ConsequenceSpec.Kind.class, ConsequenceSpec.Kind.HISTORY), Json.str(j, "target", ""), Json.str(j, "amount", "0"),
                        Json.str(j, "text", ""), Json.bool(j, "success", true), Json.bool(j, "failure", false), Path.parse(Json.str(j, "branch", null)).orElse(null)));
            }
            JsonObject vars = Json.obj(o, "vars");
            for (String k : vars.keySet()) q.variables().put(k, Json.str(vars, k, ""));
            JsonObject players = Json.obj(o, "players");
            for (String k : players.keySet()) try { q.players().put(UUID.fromString(k), Json.str(players, k, "")); } catch (IllegalArgumentException ignored) { }
            for (JsonElement el : Json.arr(o, "decisions")) if (el.isJsonObject()) { JsonObject j = el.getAsJsonObject(); q.decisions().add(new Quest.Decision(Json.lng(j, "m", 0), Json.uuid(j, "p"), Json.str(j, "c", ""))); }
            q.applied().addAll(Json.stringList(Json.arr(o, "applied"))); q.given().addAll(Json.stringList(Json.arr(o, "given")));
            for (String l : Json.stringList(Json.arr(o, "log"))) q.log(l);
            q.tags().addAll(Json.stringList(Json.arr(o, "tags")));
            if (o.has("campaign")) q.campaign(Json.uuid(o, "campaign"), Json.integer(o, "campaignStage", 0));
            q.parent(Json.uuid(o, "parent"));
            q.reservedFor(Json.uuid(o, "reservedFor"));
            q.restoreMerged(Json.integer(o, "merged", 0));
            q.restoreTimes(Json.lng(o, "offeredUntil", 0), Json.lng(o, "accepted", 0), Json.lng(o, "deadline", 0), Json.lng(o, "ended", 0));
            // the state and stage last: setters log, and restoring must not rewrite history
            Quest.State state = Json.enumOf(o, "state", Quest.State.class, Quest.State.OFFERED);
            QuestTemplate.StoryStage stage = Json.enumOf(o, "stage", QuestTemplate.StoryStage.class, QuestTemplate.StoryStage.PROLOGUE);
            q.state(state); q.stage(stage);
            return q;
        }

        @Override public JsonObject write() {
            JsonObject p = new JsonObject();
            p.add("quests", Json.array(e.quests(), Quests::quest));
            JsonArray campaigns = new JsonArray();
            for (Campaign c : e.campaigns()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", c.id().toString()); o.addProperty("type", c.type().name()); o.addProperty("title", c.title()); o.add("stages", Json.strings(c.stages()));
                o.addProperty("origin", c.originKey()); if (c.settlement() != null) o.addProperty("settlement", c.settlement().toString()); o.addProperty("started", c.started());
                o.addProperty("stage", c.stage()); o.addProperty("state", c.state().name()); o.addProperty("ended", c.ended()); if (c.player() != null) o.addProperty("player", c.player().toString());
                o.add("quests", Json.uuids(c.quests()));
                campaigns.add(o);
            }
            p.add("campaigns", campaigns);
            JsonObject cd = new JsonObject(); e.cooldowns().forEach(cd::addProperty); p.add("cooldowns", cd);
            JsonArray history = new JsonArray();
            for (var entry : e.history().players().entrySet())
                for (QuestHistory.Entry h : entry.getValue()) history.add(entry(entry.getKey(), h));
            for (QuestHistory.Entry h : e.history().world(Integer.MAX_VALUE)) history.add(entry(null, h));
            p.add("history", history);
            JsonObject totals = new JsonObject(); e.history().totals().forEach((k, v) -> totals.addProperty(k.name(), v)); p.add("totals", totals);
            p.addProperty("seed", e.seed());
            return p;
        }

        private static JsonObject entry(UUID player, QuestHistory.Entry h) {
            JsonObject o = new JsonObject();
            if (player != null) o.addProperty("player", player.toString());
            o.addProperty("quest", h.quest().toString()); o.addProperty("template", h.template()); o.addProperty("title", h.title()); o.addProperty("outcome", h.outcome().name());
            o.addProperty("path", h.path()); o.add("decisions", Json.strings(h.decisions())); o.addProperty("ending", h.ending()); o.addProperty("minute", h.minute());
            if (h.settlement() != null) o.addProperty("settlement", h.settlement().toString());
            return o;
        }

        @Override public void read(JsonObject p) {
            e.useSeed(Json.lng(p, "seed", e.seed()));
            for (Quest q : Json.list(Json.arr(p, "quests"), Quests::quest)) e.restoreQuest(q);
            for (JsonElement el : Json.arr(p, "campaigns")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID id = Json.uuid(o, "id");
                if (id == null) continue;
                Campaign c = new Campaign(id, Json.enumOf(o, "type", Campaign.Type.class, Campaign.Type.CLAN), Json.str(o, "title", ""), Json.stringList(Json.arr(o, "stages")),
                        Json.str(o, "origin", ""), Json.uuid(o, "settlement"), Json.lng(o, "started", 0));
                c.restore(Json.integer(o, "stage", 0), Json.enumOf(o, "state", Campaign.State.class, Campaign.State.ACTIVE), Json.lng(o, "ended", 0), Json.uuid(o, "player"), Json.uuidList(Json.arr(o, "quests")));
                e.restoreCampaign(c);
            }
            JsonObject cd = Json.obj(p, "cooldowns");
            for (String k : cd.keySet()) e.restoreCooldown(k, Json.lng(cd, k, 0));
            for (JsonElement el : Json.arr(p, "history")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID quest = Json.uuid(o, "quest");
                if (quest == null) continue;
                e.history().restore(Json.uuid(o, "player"), new QuestHistory.Entry(quest, Json.str(o, "template", ""), Json.str(o, "title", ""), Json.enumOf(o, "outcome", Quest.State.class, Quest.State.COMPLETED),
                        Json.str(o, "path", ""), Json.stringList(Json.arr(o, "decisions")), Json.str(o, "ending", ""), Json.lng(o, "minute", 0), Json.uuid(o, "settlement")));
            }
            Map<Quest.State, Long> totals = new EnumMap<>(Quest.State.class);
            JsonObject t = Json.obj(p, "totals");
            for (String k : t.keySet()) try { totals.put(Quest.State.valueOf(k), Json.lng(t, k, 0)); } catch (IllegalArgumentException ignored) { }
            e.history().restoreTotals(totals);
        }
    }

}
