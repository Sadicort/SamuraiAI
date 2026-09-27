package yadi.samuraiai.living.family.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.storage.Json;
import yadi.samuraiai.living.core.persistence.StoreSection;
import yadi.samuraiai.living.family.clan.ClanRecord;
import yadi.samuraiai.living.family.engine.FamilyEngine;
import yadi.samuraiai.living.family.family_memory.FamilyMemoryEntry;
import yadi.samuraiai.living.family.genealogy.GenealogyGraph;
import yadi.samuraiai.living.family.heritage.Heirloom;
import yadi.samuraiai.living.family.household.Household;
import yadi.samuraiai.living.family.inheritance.InheritanceRecord;
import yadi.samuraiai.living.family.knowledge.Technique;
import yadi.samuraiai.living.family.legacy.LegacyRecord;
import yadi.samuraiai.living.family.lifecycle.LifeState;
import yadi.samuraiai.living.family.lineage.Lineage;
import yadi.samuraiai.living.family.mentorship.Mentorship;
import yadi.samuraiai.living.family.model.KinGender;
import yadi.samuraiai.living.family.model.Person;
import yadi.samuraiai.living.family.naming.NameRecord;
import yadi.samuraiai.living.family.parenthood.BirthRecord;
import yadi.samuraiai.living.family.registry.FamilyRecord;
import yadi.samuraiai.living.family.reputation.CauseLedger;

/**
 * Family persistence in two versioned files: the people (persons, genealogy edges, partnerships, births) and the families
 * (records with memory and ledgers, households, lineages, mentorships, techniques, inheritances, heirlooms, legacies). After a
 * load the consistency validator audits the genealogy; problems are reported, never silently "fixed".
 */
public final class FamilyStorage {
    public static final int SCHEMA = 1;

    private FamilyStorage() { }

    public static List<StoreSection> sections(FamilyEngine e) { return List.of(new People(e), new Families(e)); }

    private static JsonObject ledger(CauseLedger l) {
        JsonObject o = new JsonObject();
        o.addProperty("value", l.value());
        JsonArray a = new JsonArray();
        for (CauseLedger.Cause c : l.causes()) { JsonObject x = new JsonObject(); x.addProperty("m", c.minute()); x.addProperty("d", c.delta()); x.addProperty("c", c.cause()); if (c.person() != null) x.addProperty("p", c.person().toString()); a.add(x); }
        o.add("causes", a);
        return o;
    }

    private static void ledger(CauseLedger l, JsonObject o) {
        List<CauseLedger.Cause> causes = new ArrayList<>();
        for (JsonElement el : Json.arr(o, "causes")) if (el.isJsonObject()) { JsonObject x = el.getAsJsonObject(); causes.add(new CauseLedger.Cause(Json.lng(x, "m", 0), Json.num(x, "d", 0), Json.str(x, "c", ""), Json.uuid(x, "p"))); }
        l.restore(Json.num(o, "value", 0), causes);
    }

    static final class People implements StoreSection {
        private final FamilyEngine e;
        private long saved = -1;
        People(FamilyEngine e) { this.e = e; }
        @Override public String domain() { return "family-people"; }
        @Override public String file() { return "family/people.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        @Override public boolean dirty() { return e.revision() != saved; }
        @Override public void clean() { saved = e.revision(); }
        @Override public JsonObject write() {
            JsonArray people = new JsonArray();
            for (Person p : e.people()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", p.id().toString());
                o.addProperty("given", p.name().given()); o.addProperty("familyName", p.name().family()); o.addProperty("honorific", p.name().honorific());
                o.addProperty("title", p.name().title()); o.addProperty("lineageName", p.name().lineageName()); o.addProperty("gender", p.gender().name());
                o.addProperty("epithet", p.name().epithet()); o.addProperty("nameCulture", p.name().cultureId()); o.addProperty("nameOrder", p.name().order().name());
                if (p.family() != null) o.addProperty("family", p.family().toString()); if (p.birthFamily() != null) o.addProperty("birthFamily", p.birthFamily().toString());
                if (p.household() != null) o.addProperty("household", p.household().toString()); o.addProperty("generation", p.generation());
                o.addProperty("birth", p.birth()); o.addProperty("estimated", p.birthEstimated()); if (p.birthPlace() != null) o.addProperty("birthPlace", p.birthPlace().toString());
                o.addProperty("death", p.death()); o.addProperty("state", p.state().name()); o.addProperty("role", p.role()); o.addProperty("succession", p.successionPosition());
                o.addProperty("profession", p.profession()); o.addProperty("embodied", p.embodied()); o.addProperty("aging", p.agingEnabled());
                o.add("legacyTags", Json.strings(p.legacyTags())); o.add("memories", Json.strings(p.importantMemories()));
                people.add(o);
            }
            JsonArray edges = new JsonArray();
            for (UUID[] edge : e.graph().parentEdges()) { JsonObject o = new JsonObject(); o.addProperty("parent", edge[0].toString()); o.addProperty("child", edge[1].toString()); edges.add(o); }
            JsonArray partners = new JsonArray();
            for (GenealogyGraph.Partnership p : e.graph().allPartnerships()) { JsonObject o = new JsonObject(); o.addProperty("a", p.a().toString()); o.addProperty("b", p.b().toString()); o.addProperty("since", p.since()); o.addProperty("until", p.until()); partners.add(o); }
            JsonArray births = new JsonArray();
            for (BirthRecord b : e.births()) {
                JsonObject o = new JsonObject();
                o.addProperty("person", b.person().toString()); o.addProperty("minute", b.minute()); o.addProperty("year", b.calendarYear());
                if (b.birthPlace() != null) o.addProperty("place", b.birthPlace().toString()); if (b.parentA() != null) o.addProperty("a", b.parentA().toString());
                if (b.parentB() != null) o.addProperty("b", b.parentB().toString()); if (b.family() != null) o.addProperty("family", b.family().toString());
                if (b.household() != null) o.addProperty("household", b.household().toString()); o.addProperty("generation", b.generation());
                births.add(o);
            }
            JsonObject p = new JsonObject();
            p.add("people", people); p.add("parents", edges); p.add("partners", partners); p.add("births", births); p.add("stories", Json.strings(e.storiesTold()));
            p.addProperty("seed", e.seed());
            return p;
        }
        @Override public void read(JsonObject p) {
            e.useSeed(Json.lng(p, "seed", e.seed()));
            e.loading(true);
            try {
                for (JsonElement el : Json.arr(p, "people")) if (el.isJsonObject()) {
                    JsonObject o = el.getAsJsonObject();
                    UUID id = Json.uuid(o, "id");
                    if (id == null) continue;
                    NameRecord nameRecord = new NameRecord(Json.str(o, "given", ""), Json.str(o, "familyName", ""), Json.str(o, "honorific", ""), Json.str(o, "title", ""), Json.str(o, "lineageName", ""),
                            Json.str(o, "epithet", ""), Json.str(o, "nameCulture", ""), Json.enumOf(o, "nameOrder", NameRecord.Order.class, NameRecord.Order.FAMILY_FIRST));
                    Person x = new Person(id, nameRecord,
                            Json.enumOf(o, "gender", KinGender.class, KinGender.UNSPECIFIED), Json.uuid(o, "family"), Json.integer(o, "generation", 0), Json.lng(o, "birth", 0), Json.bool(o, "estimated", true),
                            Json.enumOf(o, "state", LifeState.class, LifeState.ALIVE), Json.bool(o, "embodied", false));
                    x.birthFamily(Json.uuid(o, "birthFamily")); x.household(Json.uuid(o, "household")); x.birthPlace(Json.uuid(o, "birthPlace")); x.death(Json.lng(o, "death", Long.MIN_VALUE));
                    x.role(Json.str(o, "role", "MEMBER")); x.successionPosition(Json.integer(o, "succession", 0)); x.profession(Json.str(o, "profession", "")); x.agingEnabled(Json.bool(o, "aging", true));
                    x.legacyTags().addAll(Json.stringList(Json.arr(o, "legacyTags")));
                    for (String m : Json.stringList(Json.arr(o, "memories"))) x.remember(m);
                    e.restorePerson(x);
                }
                for (JsonElement el : Json.arr(p, "parents")) if (el.isJsonObject()) {
                    JsonObject o = el.getAsJsonObject();
                    UUID a = Json.uuid(o, "parent"), c = Json.uuid(o, "child");
                    if (a != null && c != null) e.graph().linkParent(a, c);
                }
                for (JsonElement el : Json.arr(p, "partners")) if (el.isJsonObject()) {
                    JsonObject o = el.getAsJsonObject();
                    UUID a = Json.uuid(o, "a"), b = Json.uuid(o, "b");
                    if (a != null && b != null) e.graph().linkPartners(a, b, Json.lng(o, "since", 0), Json.lng(o, "until", Long.MAX_VALUE));
                }
                for (BirthRecord b : Json.list(Json.arr(p, "births"), o -> {
                    UUID person = Json.uuid(o, "person");
                    return person == null ? null : new BirthRecord(person, Json.lng(o, "minute", 0), Json.integer(o, "year", 0), Json.uuid(o, "place"), Json.uuid(o, "a"), Json.uuid(o, "b"),
                            Json.uuid(o, "family"), Json.uuid(o, "household"), Json.integer(o, "generation", 0), List.of());
                })) e.restoreBirth(b);
                e.storiesTold().addAll(Json.stringList(Json.arr(p, "stories")));
            } finally { e.loading(false); }
            saved = e.revision();
        }
    }

    static final class Families implements StoreSection {
        private final FamilyEngine e;
        Families(FamilyEngine e) { this.e = e; }
        @Override public String domain() { return "family-families"; }
        @Override public String file() { return "family/families.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        private long saved = -1;
        @Override public boolean dirty() { return e.dirty() || e.revision() != saved; }
        @Override public void clean() { saved = e.revision(); e.clean(); }

        @Override public JsonObject write() {
            JsonObject p = new JsonObject();
            JsonArray families = new JsonArray();
            for (FamilyRecord f : e.families()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", f.id().toString()); o.addProperty("name", f.name()); o.add("founders", Json.uuids(f.founders())); o.addProperty("created", f.created());
                put(o, "originVillage", f.originVillage()); put(o, "originRegion", f.originRegion()); put(o, "village", f.village()); put(o, "parentFamily", f.parentFamily());
                put(o, "head", f.head()); put(o, "successor", f.successor()); put(o, "heir", f.designatedHeir());
                o.addProperty("branchReason", f.branchReason()); o.addProperty("status", f.status().name()); o.addProperty("generations", f.generationCount());
                o.add("branches", Json.uuids(f.branches())); o.add("reputation", ledger(f.reputation())); o.add("honor", ledger(f.honor()));
                o.addProperty("importance", f.historicalImportance()); o.add("tags", Json.strings(f.tags())); o.add("households", Json.uuids(f.households()));
                o.addProperty("culture", f.cultureId()); put(o, "clan", f.clan()); o.addProperty("houseTitle", f.houseTitle()); o.addProperty("houseGrantedAt", f.houseGrantedAt());
                o.add("traditions", Json.strings(f.traditions())); o.add("knowledge", Json.strings(f.knowledge())); o.add("heirlooms", Json.uuids(f.heirlooms()));
                o.add("properties", Json.uuids(f.properties()));
                JsonObject rel = new JsonObject(); f.relations().forEach((k, v) -> rel.addProperty(k.toString(), v.name())); o.add("relations", rel);
                JsonObject prof = new JsonObject(); f.professionsByGeneration().forEach(prof::addProperty); o.add("professions", prof);
                JsonArray mem = new JsonArray();
                for (FamilyMemoryEntry m : f.memory()) { JsonObject x = new JsonObject(); x.addProperty("m", m.minute()); x.addProperty("k", m.kind().name()); x.addProperty("t", m.text()); x.add("p", Json.uuids(m.persons())); x.addProperty("s", m.significance()); mem.add(x); }
                o.add("memory", mem);
                families.add(o);
            }
            p.add("families", families);
            JsonArray households = new JsonArray();
            for (Household h : e.households()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", h.id().toString()); put(o, "home", h.home()); put(o, "village", h.village()); put(o, "head", h.head()); put(o, "wealth", h.wealthRef());
                o.add("residents", Json.uuids(h.residents())); o.add("families", Json.uuids(h.families())); o.addProperty("beds", h.beds()); o.addProperty("schedule", h.schedule());
                o.addProperty("status", h.status().name()); o.addProperty("founded", h.founded());
                households.add(o);
            }
            p.add("households", households);
            JsonArray lineages = new JsonArray();
            for (Lineage l : e.lineages()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", l.id().toString()); o.addProperty("name", l.name()); o.addProperty("type", l.type().name()); o.addProperty("founder", l.founder().toString());
                o.addProperty("founded", l.founded()); put(o, "leader", l.leader()); put(o, "school", l.school()); o.addProperty("profession", l.profession()); o.addProperty("philosophy", l.philosophy());
                JsonArray leaders = new JsonArray();
                for (Lineage.Leadership x : l.leaders()) { JsonObject j = new JsonObject(); j.addProperty("leader", x.leader().toString()); j.addProperty("from", x.from()); j.addProperty("until", x.until()); j.addProperty("how", x.how()); leaders.add(j); }
                o.add("leaders", leaders); o.add("members", Json.uuids(l.members())); o.add("knowledge", Json.strings(l.knowledge())); o.add("traditions", Json.strings(l.traditions()));
                o.add("heirlooms", Json.uuids(l.heirlooms())); o.add("history", Json.strings(l.history())); o.add("reputation", ledger(l.reputation())); o.add("honor", ledger(l.honor()));
                o.addProperty("status", l.status().name());
                lineages.add(o);
            }
            p.add("lineages", lineages);
            JsonArray mentorships = new JsonArray();
            for (Mentorship m : e.mentorships()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", m.id().toString()); o.addProperty("master", m.master().toString()); o.addProperty("disciple", m.disciple().toString()); o.addProperty("type", m.type().name());
                o.addProperty("start", m.start()); o.addProperty("end", m.end()); put(o, "lineage", m.lineage()); o.add("knowledge", Json.strings(m.knowledgeTaught()));
                o.add("techniques", Json.strings(m.techniquesTaught())); o.addProperty("progress", m.progress()); o.addProperty("trust", m.trust()); o.addProperty("respect", m.respect());
                o.addProperty("legacy", m.legacyWeight()); o.addProperty("state", m.state().name()); o.addProperty("last", m.lastProgress());
                mentorships.add(o);
            }
            p.add("mentorships", mentorships);
            JsonArray techniques = new JsonArray();
            for (Technique t : e.techniques()) {
                JsonObject o = new JsonObject();
                o.addProperty("key", t.key()); o.addProperty("name", t.name()); put(o, "creator", t.creator()); put(o, "lineage", t.lineage()); put(o, "family", t.family());
                o.addProperty("created", t.created()); o.addProperty("rarity", t.rarity()); o.add("holders", Json.uuids(t.holders())); o.addProperty("lost", t.lostAt()); o.addProperty("evidence", t.evidence());
                JsonArray tr = new JsonArray();
                for (Technique.Transmission x : t.transmissions()) { JsonObject j = new JsonObject(); j.addProperty("from", x.from().toString()); j.addProperty("to", x.to().toString()); j.addProperty("m", x.minute()); j.addProperty("via", x.via().name()); tr.add(j); }
                o.add("transmissions", tr);
                techniques.add(o);
            }
            p.add("techniques", techniques);
            JsonArray inheritances = new JsonArray();
            for (InheritanceRecord r : e.inheritances()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", r.id().toString()); o.addProperty("owner", r.owner().toString()); o.add("heirs", Json.uuids(r.heirs())); o.addProperty("conditions", r.conditions());
                o.addProperty("reason", r.reason()); o.addProperty("created", r.created()); o.addProperty("date", r.transferDate()); o.addProperty("status", r.status().name());
                JsonArray assets = new JsonArray();
                for (InheritanceRecord.Asset a : r.assets()) assets.add(asset(a));
                o.add("assets", assets);
                JsonArray transfers = new JsonArray();
                for (InheritanceRecord.Transfer t : r.transfers()) { JsonObject j = new JsonObject(); j.add("asset", asset(t.asset())); put(j, "to", t.to()); j.addProperty("m", t.minute()); j.addProperty("done", t.done()); j.addProperty("note", t.note()); transfers.add(j); }
                o.add("transfers", transfers);
                inheritances.add(o);
            }
            p.add("inheritances", inheritances);
            JsonArray heirlooms = new JsonArray();
            for (Heirloom h : e.heirlooms()) {
                JsonObject o = new JsonObject();
                o.addProperty("item", h.itemId().toString()); o.addProperty("name", h.name()); o.addProperty("kind", h.kind()); put(o, "original", h.originalOwner()); put(o, "family", h.family());
                o.addProperty("created", h.created()); put(o, "owner", h.currentOwner()); o.addProperty("lost", h.lost()); o.addProperty("reputation", h.reputation());
                o.addProperty("epithet", h.epithet());
                JsonArray tr = new JsonArray();
                for (Heirloom.TransferRecord t : h.transfers()) { JsonObject j = new JsonObject(); put(j, "from", t.from()); put(j, "to", t.to()); j.addProperty("m", t.minute()); j.addProperty("reason", t.reason()); tr.add(j); }
                o.add("transfers", tr);
                JsonArray ev = new JsonArray();
                for (Heirloom.HistoricEvent x : h.events()) { JsonObject j = new JsonObject(); j.addProperty("m", x.minute()); j.addProperty("t", x.text()); ev.add(j); }
                o.add("events", ev);
                heirlooms.add(o);
            }
            p.add("heirlooms", heirlooms);
            JsonArray legacies = new JsonArray();
            for (LegacyRecord r : e.legacies()) {
                JsonObject o = new JsonObject();
                o.addProperty("person", r.person().toString()); o.addProperty("at", r.computedAt());
                JsonArray c = new JsonArray();
                for (LegacyRecord.Cause x : r.causes()) { JsonObject j = new JsonObject(); j.addProperty("k", x.kind().name()); j.addProperty("w", x.weight()); j.addProperty("t", x.text()); c.add(j); }
                o.add("causes", c);
                legacies.add(o);
            }
            p.add("legacies", legacies);
            JsonArray clans = new JsonArray();
            for (ClanRecord c : e.clans()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", c.id().toString()); o.addProperty("name", c.name()); put(o, "founder", c.founder()); o.addProperty("founded", c.founded());
                put(o, "region", c.region()); put(o, "leaderFamily", c.leaderFamily()); o.addProperty("status", c.status().name());
                o.add("members", Json.uuids(c.memberFamilies())); o.add("traditions", Json.strings(c.traditions())); o.add("history", Json.strings(c.history()));
                o.add("reputation", ledger(c.reputation())); o.add("honor", ledger(c.honor()));
                JsonObject rel = new JsonObject();
                c.relations().forEach((k, v) -> rel.addProperty(k.toString(), v.name()));
                o.add("relations", rel);
                clans.add(o);
            }
            p.add("clans", clans);
            return p;
        }

        private static JsonObject asset(InheritanceRecord.Asset a) {
            JsonObject o = new JsonObject();
            o.addProperty("kind", a.kind().name()); put(o, "id", a.id()); o.addProperty("amount", a.amount()); o.addProperty("label", a.label());
            return o;
        }

        private static InheritanceRecord.Asset asset(JsonObject o) {
            return new InheritanceRecord.Asset(Json.enumOf(o, "kind", InheritanceRecord.Asset.Kind.class, InheritanceRecord.Asset.Kind.RESOURCES), Json.uuid(o, "id"), Json.num(o, "amount", 0), Json.str(o, "label", ""));
        }

        private static void put(JsonObject o, String key, UUID id) { if (id != null) o.addProperty(key, id.toString()); }

        @Override public void read(JsonObject p) {
            for (JsonElement el : Json.arr(p, "families")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID id = Json.uuid(o, "id");
                if (id == null) continue;
                FamilyRecord f = new FamilyRecord(id, Json.str(o, "name", "?"), Json.lng(o, "created", 0), Json.uuid(o, "originVillage"), Json.uuid(o, "originRegion"));
                f.founders().addAll(Json.uuidList(Json.arr(o, "founders")));
                f.village(Json.uuid(o, "village"));
                if (o.has("parentFamily")) f.branchOf(Json.uuid(o, "parentFamily"), Json.str(o, "branchReason", ""));
                f.head(Json.uuid(o, "head")); f.successor(Json.uuid(o, "successor")); f.designatedHeir(Json.uuid(o, "heir"));
                f.status(Json.enumOf(o, "status", FamilyRecord.Status.class, FamilyRecord.Status.ACTIVE)); f.generationCount(Json.integer(o, "generations", 1));
                f.branches().addAll(Json.uuidList(Json.arr(o, "branches")));
                ledger(f.reputation(), Json.obj(o, "reputation")); ledger(f.honor(), Json.obj(o, "honor"));
                f.historicalImportance(Json.num(o, "importance", 0)); f.tags().addAll(Json.stringList(Json.arr(o, "tags")));
                f.cultureId(Json.str(o, "culture", "yamato")); f.clan(Json.uuid(o, "clan")); f.grantHouse(Json.str(o, "houseTitle", ""), Json.lng(o, "houseGrantedAt", Long.MIN_VALUE));
                f.households().addAll(Json.uuidList(Json.arr(o, "households"))); f.traditions().addAll(Json.stringList(Json.arr(o, "traditions")));
                f.knowledge().addAll(Json.stringList(Json.arr(o, "knowledge"))); f.heirlooms().addAll(Json.uuidList(Json.arr(o, "heirlooms")));
                f.properties().addAll(Json.uuidList(Json.arr(o, "properties")));
                JsonObject rel = Json.obj(o, "relations");
                for (String k : rel.keySet()) try { f.relations().put(UUID.fromString(k), FamilyRecord.Relation.valueOf(Json.str(rel, k, "NEUTRAL"))); } catch (IllegalArgumentException ignored) { }
                JsonObject prof = Json.obj(o, "professions");
                for (String k : prof.keySet()) f.professionsByGeneration().put(k, Json.integer(prof, k, 0));
                for (JsonElement m : Json.arr(o, "memory")) if (m.isJsonObject()) {
                    JsonObject x = m.getAsJsonObject();
                    f.memory().add(new FamilyMemoryEntry(Json.lng(x, "m", 0), Json.enumOf(x, "k", FamilyMemoryEntry.Kind.class, FamilyMemoryEntry.Kind.OTHER), Json.str(x, "t", ""),
                            Json.uuidList(Json.arr(x, "p")), Json.num(x, "s", 0)));
                }
                f.clean();
                e.restoreFamily(f);
            }
            for (JsonElement el : Json.arr(p, "households")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID id = Json.uuid(o, "id");
                if (id == null) continue;
                Household h = new Household(id, Json.uuid(o, "home"), Json.uuid(o, "village"), Json.lng(o, "founded", 0));
                h.head(Json.uuid(o, "head")); h.wealthRef(Json.uuid(o, "wealth")); h.residents().addAll(Json.uuidList(Json.arr(o, "residents")));
                h.families().addAll(Json.uuidList(Json.arr(o, "families"))); h.beds(Json.integer(o, "beds", 0)); h.schedule(Json.str(o, "schedule", ""));
                h.status(Json.enumOf(o, "status", Household.Status.class, Household.Status.NORMAL));
                e.restoreHousehold(h);
            }
            for (JsonElement el : Json.arr(p, "lineages")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID id = Json.uuid(o, "id"), founder = Json.uuid(o, "founder");
                if (id == null || founder == null) continue;
                Lineage l = new Lineage(id, Json.str(o, "name", ""), Json.enumOf(o, "type", Lineage.Type.class, Lineage.Type.CUSTOM), founder, Json.lng(o, "founded", 0), Json.str(o, "profession", ""));
                l.school(Json.uuid(o, "school")); l.philosophy(Json.str(o, "philosophy", ""));
                List<Lineage.Leadership> leaders = new ArrayList<>();
                for (JsonElement x : Json.arr(o, "leaders")) if (x.isJsonObject()) { JsonObject j = x.getAsJsonObject(); UUID who = Json.uuid(j, "leader"); if (who != null) leaders.add(new Lineage.Leadership(who, Json.lng(j, "from", 0), Json.lng(j, "until", Long.MAX_VALUE), Json.str(j, "how", ""))); }
                l.restoreLeaders(leaders, Json.uuid(o, "leader"));
                l.members().addAll(Json.uuidList(Json.arr(o, "members"))); l.knowledge().addAll(Json.stringList(Json.arr(o, "knowledge")));
                l.traditions().addAll(Json.stringList(Json.arr(o, "traditions"))); l.heirlooms().addAll(Json.uuidList(Json.arr(o, "heirlooms")));
                l.history().addAll(Json.stringList(Json.arr(o, "history")));
                ledger(l.reputation(), Json.obj(o, "reputation")); ledger(l.honor(), Json.obj(o, "honor"));
                l.status(Json.enumOf(o, "status", Lineage.Status.class, Lineage.Status.ACTIVE));
                e.restoreLineage(l);
            }
            for (JsonElement el : Json.arr(p, "mentorships")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID id = Json.uuid(o, "id"), master = Json.uuid(o, "master"), disciple = Json.uuid(o, "disciple");
                if (id == null || master == null || disciple == null) continue;
                Mentorship m = new Mentorship(id, master, disciple, Json.enumOf(o, "type", Mentorship.Type.class, Mentorship.Type.CUSTOM), Json.lng(o, "start", 0));
                m.lineage(Json.uuid(o, "lineage")); m.knowledgeTaught().addAll(Json.stringList(Json.arr(o, "knowledge"))); m.techniquesTaught().addAll(Json.stringList(Json.arr(o, "techniques")));
                m.restore(Json.enumOf(o, "state", Mentorship.State.class, Mentorship.State.ACTIVE), Json.num(o, "progress", 0), Json.lng(o, "end", 0), Json.num(o, "trust", 50),
                        Json.num(o, "respect", 50), Json.num(o, "legacy", 0), Json.lng(o, "last", 0));
                e.restoreMentorship(m);
            }
            for (JsonElement el : Json.arr(p, "techniques")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                String key = Json.str(o, "key", null);
                if (key == null) continue;
                Technique t = new Technique(key, Json.str(o, "name", key), Json.uuid(o, "creator"), Json.uuid(o, "lineage"), Json.uuid(o, "family"), Json.lng(o, "created", 0), Json.num(o, "rarity", 0.5));
                List<Technique.Transmission> tr = new ArrayList<>();
                for (JsonElement x : Json.arr(o, "transmissions")) if (x.isJsonObject()) {
                    JsonObject j = x.getAsJsonObject();
                    UUID from = Json.uuid(j, "from"), to = Json.uuid(j, "to");
                    if (from != null && to != null) tr.add(new Technique.Transmission(from, to, Json.lng(j, "m", 0), Json.enumOf(j, "via", Technique.Via.class, Technique.Via.ORAL)));
                }
                t.restore(new java.util.LinkedHashSet<>(Json.uuidList(Json.arr(o, "holders"))), tr, Json.lng(o, "lost", Long.MIN_VALUE), Json.str(o, "evidence", ""));
                e.restoreTechnique(t);
            }
            for (JsonElement el : Json.arr(p, "inheritances")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID id = Json.uuid(o, "id"), owner = Json.uuid(o, "owner");
                if (id == null || owner == null) continue;
                List<InheritanceRecord.Asset> assets = new ArrayList<>();
                for (JsonElement x : Json.arr(o, "assets")) if (x.isJsonObject()) assets.add(asset(x.getAsJsonObject()));
                InheritanceRecord r = new InheritanceRecord(id, owner, Json.uuidList(Json.arr(o, "heirs")), assets, Json.str(o, "conditions", ""), Json.str(o, "reason", ""), Json.lng(o, "created", 0));
                List<InheritanceRecord.Transfer> transfers = new ArrayList<>();
                for (JsonElement x : Json.arr(o, "transfers")) if (x.isJsonObject()) {
                    JsonObject j = x.getAsJsonObject();
                    transfers.add(new InheritanceRecord.Transfer(asset(Json.obj(j, "asset")), Json.uuid(j, "to"), Json.lng(j, "m", 0), Json.bool(j, "done", false), Json.str(j, "note", "")));
                }
                r.restore(Json.enumOf(o, "status", InheritanceRecord.Status.class, InheritanceRecord.Status.PENDING), Json.lng(o, "date", 0), transfers);
                e.restoreInheritance(r);
            }
            for (JsonElement el : Json.arr(p, "heirlooms")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID item = Json.uuid(o, "item");
                if (item == null) continue;
                Heirloom h = new Heirloom(item, Json.str(o, "name", ""), Json.str(o, "kind", "objeto"), Json.uuid(o, "original"), Json.uuid(o, "family"), Json.lng(o, "created", 0));
                List<Heirloom.TransferRecord> tr = new ArrayList<>();
                for (JsonElement x : Json.arr(o, "transfers")) if (x.isJsonObject()) { JsonObject j = x.getAsJsonObject(); tr.add(new Heirloom.TransferRecord(Json.uuid(j, "from"), Json.uuid(j, "to"), Json.lng(j, "m", 0), Json.str(j, "reason", ""))); }
                List<Heirloom.HistoricEvent> ev = new ArrayList<>();
                for (JsonElement x : Json.arr(o, "events")) if (x.isJsonObject()) { JsonObject j = x.getAsJsonObject(); ev.add(new Heirloom.HistoricEvent(Json.lng(j, "m", 0), Json.str(j, "t", ""))); }
                h.restore(Json.uuid(o, "owner"), Json.bool(o, "lost", false), Json.num(o, "reputation", 0), tr, ev, Json.str(o, "epithet", ""));
                e.restoreHeirloom(h);
            }
            for (JsonElement el : Json.arr(p, "legacies")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID person = Json.uuid(o, "person");
                if (person == null) continue;
                List<LegacyRecord.Cause> causes = new ArrayList<>();
                for (JsonElement x : Json.arr(o, "causes")) if (x.isJsonObject()) { JsonObject j = x.getAsJsonObject(); causes.add(new LegacyRecord.Cause(Json.enumOf(j, "k", LegacyRecord.Kind.class, LegacyRecord.Kind.FAMILY_IMPACT), Json.num(j, "w", 0), Json.str(j, "t", ""))); }
                e.restoreLegacy(new LegacyRecord(person, Json.lng(o, "at", 0), causes));
            }
            for (JsonElement el : Json.arr(p, "clans")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID id = Json.uuid(o, "id");
                if (id == null) continue;
                ClanRecord c = new ClanRecord(id, Json.str(o, "name", "?"), Json.uuid(o, "founder"), Json.lng(o, "founded", 0), Json.uuid(o, "region"));
                c.leaderFamily(Json.uuid(o, "leaderFamily")); c.status(Json.enumOf(o, "status", ClanRecord.Status.class, ClanRecord.Status.FORMING));
                c.memberFamilies().addAll(Json.uuidList(Json.arr(o, "members"))); c.traditions().addAll(Json.stringList(Json.arr(o, "traditions")));
                c.history().addAll(Json.stringList(Json.arr(o, "history")));
                ledger(c.reputation(), Json.obj(o, "reputation")); ledger(c.honor(), Json.obj(o, "honor"));
                JsonObject rel = Json.obj(o, "relations");
                for (String k : rel.keySet()) try { c.relations().put(UUID.fromString(k), ClanRecord.Relation.valueOf(Json.str(rel, k, "NEUTRAL"))); } catch (IllegalArgumentException ignored) { }
                c.clean();
                e.restoreClan(c);
            }
        }
    }
}
