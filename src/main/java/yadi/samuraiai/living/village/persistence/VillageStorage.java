package yadi.samuraiai.living.village.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.storage.Json;
import yadi.samuraiai.living.core.DayPhase;
import yadi.samuraiai.living.core.persistence.LivingStorage;
import yadi.samuraiai.living.core.persistence.StoreSection;
import yadi.samuraiai.living.village.buildings.Building;
import yadi.samuraiai.living.village.buildings.BuildingKind;
import yadi.samuraiai.living.village.buildings.OwnerRef;
import yadi.samuraiai.living.village.citizens.Citizen;
import yadi.samuraiai.living.village.districts.District;
import yadi.samuraiai.living.village.districts.DistrictKind;
import yadi.samuraiai.living.village.engine.VillageEngine;
import yadi.samuraiai.living.village.homes.HomeRecord;
import yadi.samuraiai.living.village.life.VillageEventKind;
import yadi.samuraiai.living.village.life.VillageEventRecord;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.village.runtime.VillageLayout;
import yadi.samuraiai.living.village.security.SecurityState;
import yadi.samuraiai.living.village.visitors.VisitorRecord;

/**
 * Village persistence: an index of villages and one versioned file per village (its buildings, districts, citizens, homes,
 * guard roster, visitors, events, security, layout, renown and memory counters), so a change in one village rewrites only
 * that village. New villages get their file registered on the fly.
 */
public final class VillageStorage {
    public static final int SCHEMA = 1;

    private VillageStorage() { }

    public static void install(LivingStorage storage, VillageEngine engine) { storage.register(new Index(storage, engine)); }

    static final class Index implements StoreSection {
        private final LivingStorage storage;
        private final VillageEngine engine;
        private final Map<UUID, VillageFile> files = new LinkedHashMap<>();
        private int written = -1;
        Index(LivingStorage storage, VillageEngine engine) { this.storage = storage; this.engine = engine; }
        @Override public String domain() { return "village-index"; }
        @Override public String file() { return "village/index.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        private void registerNew() {
            for (Village v : engine.villages()) if (!files.containsKey(v.id())) { VillageFile f = new VillageFile(engine, v.id()); files.put(v.id(), f); storage.register(f); }
        }
        @Override public boolean dirty() { registerNew(); return files.size() != written; }
        @Override public JsonObject write() {
            registerNew();
            JsonObject p = new JsonObject();
            p.add("villages", Json.uuids(files.keySet()));
            written = files.size();
            return p;
        }
        @Override public void read(JsonObject p) {
            engine.loading(true);
            try {
                for (UUID id : Json.uuidList(Json.arr(p, "villages"))) {
                    if (files.containsKey(id)) continue;
                    VillageFile f = new VillageFile(engine, id);
                    files.put(id, f);
                    storage.register(f);
                    storage.load(f);
                }
            } finally { engine.loading(false); }
            written = files.size();
        }
        @Override public void clean() { engine.cleanCitizens(); }
    }

    static final class VillageFile implements StoreSection {
        private final VillageEngine engine;
        private final UUID id;
        VillageFile(VillageEngine engine, UUID id) { this.engine = engine; this.id = id; }
        @Override public String domain() { return "village"; }
        @Override public String file() { return "village/villages/" + id + ".json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        @Override public boolean dirty() { return engine.village(id).map(Village::dirty).orElse(false); }
        @Override public void clean() { engine.village(id).ifPresent(Village::clean); }

        @Override public JsonObject write() {
            Village v = engine.village(id).orElseThrow();
            JsonObject o = new JsonObject();
            o.addProperty("id", v.id().toString()); if (v.region() != null) o.addProperty("region", v.region().toString());
            o.addProperty("name", v.name()); o.addProperty("culture", v.culture()); o.addProperty("community", v.communityKey()); o.addProperty("dim", v.dimension());
            o.addProperty("x", v.x()); o.addProperty("y", v.y()); o.addProperty("z", v.z()); o.addProperty("radius", v.radius()); o.addProperty("founded", v.founded());
            if (v.mainTemple() != null) o.addProperty("temple", v.mainTemple().toString());
            if (v.market() != null) o.addProperty("market", v.market().toString());
            o.addProperty("renown", v.renown()); o.add("renownCauses", Json.strings(v.renownCauses())); o.addProperty("unrest", v.unrest()); o.addProperty("prosperity", v.prosperity());
            o.addProperty("lastSimulated", v.lastSimulated());
            JsonObject counters = new JsonObject(); v.counters().forEach((k, c) -> counters.addProperty(k.name(), c)); o.add("counters", counters);
            JsonObject sec = new JsonObject();
            sec.addProperty("state", v.security().state().name()); sec.addProperty("threat", v.security().threat()); sec.addProperty("since", v.security().since());
            sec.addProperty("updated", v.security().updatedAt()); sec.add("attacks", Json.uuids(v.security().attacks()));
            o.add("security", sec);
            JsonArray districts = new JsonArray();
            for (District d : v.districts().values()) {
                JsonObject x = new JsonObject();
                x.addProperty("id", d.id().toString()); x.addProperty("kind", d.kind().name()); x.addProperty("name", d.name()); x.addProperty("x", d.x()); x.addProperty("z", d.z()); x.addProperty("radius", d.radius());
                x.add("buildings", Json.uuids(d.buildings()));
                districts.add(x);
            }
            o.add("districts", districts);
            JsonArray buildings = new JsonArray();
            for (Building b : v.buildings().values()) {
                JsonObject x = new JsonObject();
                x.addProperty("id", b.id().toString()); x.addProperty("kind", b.kind().name()); x.addProperty("name", b.name()); if (b.district() != null) x.addProperty("district", b.district().toString());
                x.addProperty("dim", b.dimension()); x.addProperty("x", b.x()); x.addProperty("y", b.y()); x.addProperty("z", b.z()); x.addProperty("radius", b.radius());
                x.addProperty("state", b.state().name()); x.addProperty("condition", b.condition()); x.addProperty("capacity", b.capacity());
                JsonObject owner = new JsonObject(); owner.addProperty("kind", b.owner().kind().name()); if (b.owner().id() != null) owner.addProperty("id", b.owner().id().toString()); owner.addProperty("label", b.owner().label());
                x.add("owner", owner);
                x.addProperty("inventory", b.inventoryRef()); x.addProperty("zone", b.zoneId()); x.addProperty("builtAt", b.builtAt());
                JsonArray open = new JsonArray(); for (DayPhase ph : b.openPhases()) open.add(ph.name()); x.add("open", open);
                x.add("events", Json.strings(b.events()));
                buildings.add(x);
            }
            o.add("buildings", buildings);
            JsonArray citizens = new JsonArray();
            for (Citizen c : engine.citizens()) {
                if (!id.equals(c.village())) continue;
                JsonObject x = new JsonObject();
                x.addProperty("id", c.id().toString()); x.addProperty("name", c.name()); x.addProperty("type", c.npcType()); x.addProperty("status", c.status().name());
                x.addProperty("profession", c.profession()); x.addProperty("hours", c.professionHours()); if (c.home() != null) x.addProperty("home", c.home().toString());
                JsonArray roles = new JsonArray(); for (Citizen.Role r : c.roles()) roles.add(r.name()); x.add("roles", roles);
                x.addProperty("joined", c.joined()); x.addProperty("offset", c.scheduleOffset()); x.addProperty("embodied", c.embodied());
                x.addProperty("today", c.hoursToday(c.lastWorkDay())); x.addProperty("workDay", c.lastWorkDay());
                citizens.add(x);
            }
            o.add("citizens", citizens);
            o.add("members", Json.uuids(v.citizens()));
            JsonArray homes = new JsonArray();
            for (HomeRecord h : v.homes().values()) {
                JsonObject x = new JsonObject();
                x.addProperty("citizen", h.citizen().toString()); x.addProperty("house", h.house().toString()); x.addProperty("room", h.room());
                x.addProperty("bx", h.bedX()); x.addProperty("by", h.bedY()); x.addProperty("bz", h.bedZ()); x.addProperty("private", h.privateRadius()); x.addProperty("since", h.since());
                if (h.hasChest()) { x.addProperty("cx", h.chestX()); x.addProperty("cy", h.chestY()); x.addProperty("cz", h.chestZ()); }
                x.add("objects", Json.strings(h.personalObjects()));
                homes.add(x);
            }
            o.add("homes", homes);
            JsonObject watch = new JsonObject(); v.nightWatch().forEach((k, b) -> watch.addProperty(k.toString(), b)); o.add("nightWatch", watch);
            JsonArray visitors = new JsonArray();
            for (VisitorRecord r : v.visitors()) {
                JsonObject x = new JsonObject();
                x.addProperty("id", r.id().toString()); x.addProperty("kind", r.kind().name()); x.addProperty("name", r.name()); if (r.npc() != null) x.addProperty("npc", r.npc().toString());
                if (r.origin() != null) x.addProperty("origin", r.origin().toString()); x.addProperty("arrived", r.arrived()); x.addProperty("leaves", r.leaves()); x.addProperty("purpose", r.purpose());
                x.addProperty("phase", r.phase().name());
                visitors.add(x);
            }
            o.add("visitors", visitors);
            JsonArray events = new JsonArray();
            for (VillageEventRecord e : v.events()) {
                JsonObject x = new JsonObject();
                x.addProperty("id", e.id().toString()); x.addProperty("kind", e.kind().name()); x.addProperty("title", e.title()); x.addProperty("start", e.start()); x.addProperty("end", e.end());
                x.addProperty("source", e.source()); x.add("bias", map(e.bias())); x.add("guardBias", map(e.guardBias()));
                events.add(x);
            }
            o.add("events", events);
            JsonArray nodes = new JsonArray();
            for (VillageLayout.Node n : v.layout().nodes()) {
                if (n.kind() == VillageLayout.NodeKind.BUILDING) continue;   // building nodes are rebuilt from the buildings
                JsonObject x = new JsonObject();
                x.addProperty("id", n.id().toString()); x.addProperty("kind", n.kind().name()); x.addProperty("x", n.x()); x.addProperty("z", n.z());
                nodes.add(x);
            }
            o.add("layoutHubs", nodes);
            return o;
        }

        private static JsonObject map(Map<String, Double> m) { JsonObject o = new JsonObject(); m.forEach(o::addProperty); return o; }
        private static Map<String, Double> unmap(JsonObject o) { Map<String, Double> m = new LinkedHashMap<>(); for (String k : o.keySet()) m.put(k, Json.num(o, k, 0)); return m; }

        @Override public void read(JsonObject o) {
            Village v = new Village(id, Json.uuid(o, "region"), Json.str(o, "name", "?"), Json.str(o, "culture", "village"), Json.str(o, "community", ""), Json.str(o, "dim", "minecraft:overworld"),
                    Json.num(o, "x", 0), Json.num(o, "y", 64), Json.num(o, "z", 0), Json.num(o, "radius", 64), Json.lng(o, "founded", 0));
            v.restoreRenown(Json.num(o, "renown", 0));
            v.renownCauses().addAll(Json.stringList(Json.arr(o, "renownCauses")));
            v.unrest(Json.num(o, "unrest", 0)); v.prosperity(Json.num(o, "prosperity", 0.5)); v.lastSimulated(Json.lng(o, "lastSimulated", 0));
            JsonObject counters = Json.obj(o, "counters");
            for (String k : counters.keySet()) try { v.counters().put(Village.Counter.valueOf(k), Json.integer(counters, k, 0)); } catch (IllegalArgumentException ignored) { }
            JsonObject sec = Json.obj(o, "security");
            v.security().restore(Json.enumOf(sec, "state", SecurityState.class, SecurityState.PEACE), Json.num(sec, "threat", 0), Json.lng(sec, "since", 0), Json.lng(sec, "updated", 0),
                    new HashSet<>(Json.uuidList(Json.arr(sec, "attacks"))));
            for (JsonElement e : Json.arr(o, "layoutHubs")) if (e.isJsonObject()) {
                JsonObject x = e.getAsJsonObject();
                UUID nid = Json.uuid(x, "id");
                if (nid != null) v.layout().add(new VillageLayout.Node(nid, Json.enumOf(x, "kind", VillageLayout.NodeKind.class, VillageLayout.NodeKind.JUNCTION), Json.num(x, "x", 0), Json.num(x, "z", 0), null));
            }
            for (JsonElement e : Json.arr(o, "districts")) if (e.isJsonObject()) {
                JsonObject x = e.getAsJsonObject();
                DistrictKind kind = Json.enumOf(x, "kind", DistrictKind.class, DistrictKind.RESIDENTIAL);
                District d = new District(Json.uuid(x, "id"), kind, Json.str(x, "name", ""), Json.num(x, "x", 0), Json.num(x, "z", 0), Json.num(x, "radius", 12));
                d.buildings().addAll(Json.uuidList(Json.arr(x, "buildings")));
                v.districts().put(kind, d);
            }
            for (JsonElement e : Json.arr(o, "buildings")) if (e.isJsonObject()) {
                JsonObject x = e.getAsJsonObject();
                UUID bid = Json.uuid(x, "id");
                if (bid == null) continue;
                Building b = new Building(bid, id, Json.enumOf(x, "kind", BuildingKind.class, BuildingKind.WORKSHOP), Json.str(x, "name", ""), Json.str(x, "dim", v.dimension()),
                        Json.num(x, "x", 0), Json.num(x, "y", 64), Json.num(x, "z", 0), Json.num(x, "radius", 3), Json.enumOf(x, "state", Building.State.class, Building.State.BUILT), Json.lng(x, "builtAt", 0));
                b.district(Json.uuid(x, "district")); b.condition(Json.num(x, "condition", 1)); b.capacity(Json.integer(x, "capacity", b.capacity()));
                JsonObject owner = Json.obj(x, "owner");
                b.owner(new OwnerRef(Json.enumOf(owner, "kind", OwnerRef.Kind.class, OwnerRef.Kind.NONE), Json.uuid(owner, "id"), Json.str(owner, "label", "")));
                b.inventoryRef(Json.str(x, "inventory", "")); b.zoneId(Json.str(x, "zone", ""));
                b.openPhases().clear();
                for (String ph : Json.stringList(Json.arr(x, "open"))) DayPhase.parse(ph).ifPresent(b.openPhases()::add);
                if (b.openPhases().isEmpty()) b.openPhases().addAll(EnumSet.allOf(DayPhase.class));
                for (String ev : Json.stringList(Json.arr(x, "events"))) b.note(ev);
                v.buildings().put(bid, b);
                v.layout().addBuilding(bid, b.x(), b.z());
            }
            v.mainTemple(Json.uuid(o, "temple"));
            v.market(Json.uuid(o, "market"));
            for (JsonElement e : Json.arr(o, "citizens")) if (e.isJsonObject()) {
                JsonObject x = e.getAsJsonObject();
                UUID cid = Json.uuid(x, "id");
                if (cid == null) continue;
                Citizen c = new Citizen(cid, Json.str(x, "name", "?"), Json.str(x, "type", "villager"), id, Json.lng(x, "joined", 0), Json.bool(x, "embodied", false));
                c.status(Json.enumOf(x, "status", Citizen.Status.class, Citizen.Status.RESIDENT)); c.profession(Json.str(x, "profession", "")); c.professionHours(Json.num(x, "hours", 0));
                c.home(Json.uuid(x, "home")); c.scheduleOffset(Json.integer(x, "offset", 0)); c.restoreWork(Json.num(x, "today", 0), Json.lng(x, "workDay", Long.MIN_VALUE));
                for (String r : Json.stringList(Json.arr(x, "roles"))) try { c.roles().add(Citizen.Role.valueOf(r)); } catch (IllegalArgumentException ignored) { }
                engine.restoreCitizen(c);
            }
            v.citizens().addAll(Json.uuidList(Json.arr(o, "members")));
            for (JsonElement e : Json.arr(o, "homes")) if (e.isJsonObject()) {
                JsonObject x = e.getAsJsonObject();
                UUID cid = Json.uuid(x, "citizen"), house = Json.uuid(x, "house");
                if (cid == null || house == null) continue;
                HomeRecord h = new HomeRecord(cid, house, Json.str(x, "room", ""), Json.num(x, "bx", 0), Json.num(x, "by", 64), Json.num(x, "bz", 0), Json.num(x, "private", 3), Json.lng(x, "since", 0));
                if (x.has("cx")) h.chest(Json.num(x, "cx", 0), Json.num(x, "cy", 0), Json.num(x, "cz", 0));
                h.personalObjects().addAll(Json.stringList(Json.arr(x, "objects")));
                v.homes().put(cid, h);
            }
            JsonObject watch = Json.obj(o, "nightWatch");
            for (String k : watch.keySet()) try { v.nightWatch().put(UUID.fromString(k), Json.bool(watch, k, false)); } catch (IllegalArgumentException ignored) { }
            for (JsonElement e : Json.arr(o, "visitors")) if (e.isJsonObject()) {
                JsonObject x = e.getAsJsonObject();
                UUID vid = Json.uuid(x, "id");
                if (vid == null) continue;
                VisitorRecord r = new VisitorRecord(vid, Json.enumOf(x, "kind", VisitorRecord.Kind.class, VisitorRecord.Kind.TRAVELER), Json.str(x, "name", ""), Json.uuid(x, "npc"), Json.uuid(x, "origin"),
                        Json.lng(x, "arrived", 0), Json.lng(x, "leaves", 1), Json.str(x, "purpose", ""));
                r.phase(Json.enumOf(x, "phase", VisitorRecord.Phase.class, VisitorRecord.Phase.STAY));
                v.visitors().add(r);
            }
            for (JsonElement e : Json.arr(o, "events")) if (e.isJsonObject()) {
                JsonObject x = e.getAsJsonObject();
                UUID eid = Json.uuid(x, "id");
                if (eid == null) continue;
                v.events().add(new VillageEventRecord(eid, Json.enumOf(x, "kind", VillageEventKind.class, VillageEventKind.EMERGENCY), Json.str(x, "title", ""), Json.lng(x, "start", 0), Json.lng(x, "end", 1),
                        Json.str(x, "source", ""), unmap(Json.obj(x, "bias")), unmap(Json.obj(x, "guardBias"))));
            }
            engine.restoreVillage(v);
            v.clean();
        }
    }

    public static Set<String> files(LivingStorage storage) {
        Set<String> out = new HashSet<>();
        for (StoreSection s : storage.sections()) if (s.file().startsWith("village/")) out.add(s.file());
        return out;
    }
}
