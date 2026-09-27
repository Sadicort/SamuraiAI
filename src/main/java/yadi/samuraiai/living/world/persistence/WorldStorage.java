package yadi.samuraiai.living.world.persistence;

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
import yadi.samuraiai.living.world.engine.WorldEngine;
import yadi.samuraiai.living.world.events.WorldEventPhase;
import yadi.samuraiai.living.world.events.WorldEventRecord;
import yadi.samuraiai.living.world.events.WorldEventType;
import yadi.samuraiai.living.world.population.PopulationLedger;
import yadi.samuraiai.living.world.regions.Region;
import yadi.samuraiai.living.world.regions.RegionType;
import yadi.samuraiai.living.world.regions.ResourceDeposit;
import yadi.samuraiai.living.world.roads.RoadEdge;
import yadi.samuraiai.living.world.roads.RoadNode;
import yadi.samuraiai.living.world.settlements.Settlement;
import yadi.samuraiai.living.world.settlements.SettlementType;
import yadi.samuraiai.living.world.streaming.SimulationLevel;
import yadi.samuraiai.living.world.wildlife.WildlifePopulation;

/** The world's persistent sections: regions (with deposits, wildlife and memory), settlements, the road graph, the world events and the population ledger. */
public final class WorldStorage {
    public static final int SCHEMA = 1;

    private WorldStorage() { }

    public static List<StoreSection> sections(WorldEngine world) {
        return List.of(new Regions(world), new Settlements(world), new Roads(world), new Events(world), new Population(world));
    }

    static final class Regions implements StoreSection {
        private final WorldEngine world;
        Regions(WorldEngine world) { this.world = world; }
        @Override public String domain() { return "world-regions"; }
        @Override public String file() { return "world/regions.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        @Override public boolean dirty() { return world.regionsDirty(); }
        @Override public void clean() { world.cleanRegions(); }
        @Override public JsonObject write() {
            JsonArray a = new JsonArray();
            for (Region r : world.regions()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", r.id().toString()); o.addProperty("key", r.key()); o.addProperty("dim", r.dimension()); o.addProperty("cx", r.cellX()); o.addProperty("cz", r.cellZ());
                o.addProperty("size", r.cellSize()); o.addProperty("name", r.name()); o.addProperty("type", r.type().name()); o.addProperty("biome", r.biome()); o.addProperty("culture", r.cultureId());
                o.addProperty("baseDanger", r.baseDanger()); o.addProperty("eventDanger", r.eventDanger()); o.addProperty("altitude", r.altitude()); o.addProperty("population", r.population());
                o.addProperty("level", r.level().name()); o.addProperty("created", r.created()); o.addProperty("lastSimulated", r.lastSimulated()); o.addProperty("lastPlayer", r.lastPlayerSeen());
                JsonArray deposits = new JsonArray();
                for (ResourceDeposit d : r.deposits().values()) {
                    JsonObject x = new JsonObject();
                    x.addProperty("resource", d.resource()); x.addProperty("capacity", d.capacity()); x.addProperty("regen", d.regenPerDay());
                    x.addProperty("stock", d.stock(d.updatedAt(), 1440)); x.addProperty("extracted", d.extracted()); x.addProperty("at", d.updatedAt());
                    deposits.add(x);
                }
                o.add("deposits", deposits);
                JsonArray animals = new JsonArray();
                for (WildlifePopulation p : r.wildlife().values()) {
                    JsonObject x = new JsonObject();
                    x.addProperty("species", p.species()); x.addProperty("count", p.count()); x.addProperty("capacity", p.capacity()); x.addProperty("taken", p.taken());
                    x.addProperty("preyed", p.preyedOn()); x.addProperty("at", p.updatedAt());
                    animals.add(x);
                }
                o.add("wildlife", animals);
                JsonObject rel = new JsonObject();
                r.relations().forEach((k, v) -> rel.addProperty(k.toString(), v.name()));
                o.add("relations", rel);
                JsonObject counters = new JsonObject();
                r.counters().forEach((k, v) -> counters.addProperty(k.name(), v));
                o.add("counters", counters);
                a.add(o);
            }
            JsonObject p = new JsonObject();
            p.add("regions", a);
            p.addProperty("seed", world.seed());
            return p;
        }
        @Override public void read(JsonObject p) {
            for (JsonElement e : Json.arr(p, "regions")) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                UUID id = Json.uuid(o, "id");
                if (id == null) continue;
                Region r = new Region(id, Json.str(o, "key", ""), Json.str(o, "dim", "minecraft:overworld"), Json.integer(o, "cx", 0), Json.integer(o, "cz", 0), Json.integer(o, "size", 512),
                        Json.str(o, "name", "?"), Json.enumOf(o, "type", RegionType.class, RegionType.FIELDS), Json.str(o, "biome", ""), Json.str(o, "culture", "village"), Json.lng(o, "created", 0));
                r.baseDanger(Json.num(o, "baseDanger", 0)); r.eventDanger(Json.num(o, "eventDanger", 0)); r.altitude(Json.num(o, "altitude", 64)); r.population(Json.integer(o, "population", 0));
                r.level(Json.enumOf(o, "level", SimulationLevel.class, SimulationLevel.ABSTRACT)); r.lastSimulated(Json.lng(o, "lastSimulated", 0)); r.lastPlayerSeen(Json.lng(o, "lastPlayer", 0));
                for (JsonElement d : Json.arr(o, "deposits")) if (d.isJsonObject()) {
                    JsonObject x = d.getAsJsonObject();
                    ResourceDeposit dep = new ResourceDeposit(Json.str(x, "resource", ""), Json.num(x, "capacity", 0), Json.num(x, "stock", 0), Json.num(x, "regen", 0), Json.lng(x, "at", 0));
                    dep.restore(Json.num(x, "stock", 0), Json.num(x, "extracted", 0), Json.lng(x, "at", 0));
                    r.deposits().put(dep.resource(), dep);
                }
                for (JsonElement d : Json.arr(o, "wildlife")) if (d.isJsonObject()) {
                    JsonObject x = d.getAsJsonObject();
                    WildlifePopulation pop = new WildlifePopulation(Json.str(x, "species", ""), 0, 0, 0);
                    pop.restore(Json.num(x, "count", 0), Json.num(x, "capacity", 0), Json.num(x, "taken", 0), Json.num(x, "preyed", 0), Json.lng(x, "at", 0));
                    r.wildlife().put(pop.species(), pop);
                }
                JsonObject rel = Json.obj(o, "relations");
                for (String k : rel.keySet()) try { r.relations().put(UUID.fromString(k), Region.Relation.valueOf(Json.str(rel, k, "ADJACENT"))); } catch (IllegalArgumentException ignored) { }
                JsonObject counters = Json.obj(o, "counters");
                for (String k : counters.keySet()) try { r.counters().put(Region.Counter.valueOf(k), Json.integer(counters, k, 0)); } catch (IllegalArgumentException ignored) { }
                r.clean();
                world.restoreRegion(r);
            }
            world.useSeed(Json.lng(p, "seed", world.seed()));
        }
    }

    static final class Settlements implements StoreSection {
        private final WorldEngine world;
        Settlements(WorldEngine world) { this.world = world; }
        @Override public String domain() { return "world-settlements"; }
        @Override public String file() { return "world/settlements.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        @Override public boolean dirty() { return world.settlementsDirty(); }
        @Override public void clean() { world.cleanSettlements(); }
        @Override public JsonObject write() {
            JsonArray a = new JsonArray();
            for (Settlement s : world.settlements()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", s.id().toString()); o.addProperty("key", s.key()); o.addProperty("name", s.name()); o.addProperty("type", s.type().name());
                o.addProperty("region", s.regionId().toString()); o.addProperty("dim", s.dimension()); o.addProperty("x", s.x()); o.addProperty("y", s.y()); o.addProperty("z", s.z());
                o.addProperty("radius", s.radius()); o.addProperty("founded", s.founded()); o.add("origin", s.origin().toJson()); o.addProperty("status", s.status().name());
                if (s.roadNode() != null) o.addProperty("roadNode", s.roadNode().toString());
                o.add("tags", Json.strings(s.tags()));
                a.add(o);
            }
            JsonObject p = new JsonObject();
            p.add("settlements", a);
            return p;
        }
        @Override public void read(JsonObject p) {
            for (JsonElement e : Json.arr(p, "settlements")) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                UUID id = Json.uuid(o, "id"), region = Json.uuid(o, "region");
                if (id == null || region == null) continue;
                Settlement s = new Settlement(id, Json.str(o, "key", ""), Json.str(o, "name", "?"), Json.enumOf(o, "type", SettlementType.class, SettlementType.VILLAGE), region,
                        Json.str(o, "dim", "minecraft:overworld"), Json.num(o, "x", 0), Json.num(o, "y", 64), Json.num(o, "z", 0), Json.num(o, "radius", 48), Json.lng(o, "founded", 0),
                        Provenance.fromJson(Json.obj(o, "origin")));
                s.status(Json.enumOf(o, "status", Settlement.Status.class, Settlement.Status.ACTIVE));
                s.roadNode(Json.uuid(o, "roadNode"));
                s.tags().addAll(Json.stringList(Json.arr(o, "tags")));
                s.clean();
                world.restoreSettlement(s);
            }
        }
    }

    static final class Roads implements StoreSection {
        private final WorldEngine world;
        Roads(WorldEngine world) { this.world = world; }
        @Override public String domain() { return "world-roads"; }
        @Override public String file() { return "world/roads.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        @Override public boolean dirty() { return world.roads().dirty(); }
        @Override public void clean() { world.roads().clean(); }
        @Override public JsonObject write() {
            JsonArray nodes = new JsonArray(), edges = new JsonArray();
            for (RoadNode n : world.roads().nodes()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", n.id().toString()); o.addProperty("kind", n.kind().name()); o.addProperty("name", n.name()); o.addProperty("dim", n.dimension());
                o.addProperty("x", n.x()); o.addProperty("z", n.z()); if (n.settlement() != null) o.addProperty("settlement", n.settlement().toString());
                nodes.add(o);
            }
            for (RoadEdge e : world.roads().edges()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", e.id().toString()); o.addProperty("a", e.a().toString()); o.addProperty("b", e.b().toString()); o.addProperty("kind", e.kind().name());
                o.addProperty("length", e.length()); o.addProperty("bridges", e.bridges()); o.add("regions", Json.uuids(e.regions())); o.addProperty("condition", e.condition());
                o.addProperty("danger", e.landDanger()); o.addProperty("eventDanger", e.eventDanger()); o.addProperty("blocked", e.blocked()); o.addProperty("reason", e.blockedReason());
                o.addProperty("traffic", e.traffic()); o.addProperty("last", e.lastTravelled());
                edges.add(o);
            }
            JsonObject p = new JsonObject();
            p.add("nodes", nodes); p.add("edges", edges); p.add("eventBlocked", Json.uuids(world.eventBlockedRoads()));
            return p;
        }
        @Override public void read(JsonObject p) {
            for (JsonElement e : Json.arr(p, "nodes")) if (e.isJsonObject()) {
                JsonObject o = e.getAsJsonObject();
                UUID id = Json.uuid(o, "id");
                if (id == null) continue;
                world.roads().addNode(new RoadNode(id, Json.enumOf(o, "kind", RoadNode.Kind.class, RoadNode.Kind.SETTLEMENT), Json.str(o, "name", ""), Json.str(o, "dim", "minecraft:overworld"),
                        Json.num(o, "x", 0), Json.num(o, "z", 0), Json.uuid(o, "settlement")));
            }
            for (JsonElement e : Json.arr(p, "edges")) if (e.isJsonObject()) {
                JsonObject o = e.getAsJsonObject();
                UUID id = Json.uuid(o, "id"), a = Json.uuid(o, "a"), b = Json.uuid(o, "b");
                if (id == null || a == null || b == null) continue;
                RoadEdge edge = new RoadEdge(id, a, b, Json.enumOf(o, "kind", RoadEdge.Kind.class, RoadEdge.Kind.ROAD), Json.num(o, "length", 1), Json.integer(o, "bridges", 0), Json.uuidList(Json.arr(o, "regions")));
                edge.restore(Json.num(o, "condition", 1), Json.num(o, "danger", 0), Json.num(o, "eventDanger", 0), Json.bool(o, "blocked", false), Json.str(o, "reason", ""), Json.lng(o, "traffic", 0), Json.lng(o, "last", 0));
                world.roads().restoreEdge(edge);
            }
            world.restoreEventBlocked(Json.uuidList(Json.arr(p, "eventBlocked")));
        }
    }

    static final class Events implements StoreSection {
        private final WorldEngine world;
        Events(WorldEngine world) { this.world = world; }
        @Override public String domain() { return "world-events"; }
        @Override public String file() { return "world/events.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        @Override public boolean dirty() { return world.events().dirty(); }
        @Override public void clean() { world.events().clean(); }
        private static JsonObject toJson(WorldEventRecord e) {
            JsonObject o = new JsonObject();
            o.addProperty("id", e.id().toString()); o.addProperty("type", e.type().name()); o.addProperty("title", e.title());
            if (e.region() != null) o.addProperty("region", e.region().toString());
            if (e.settlement() != null) o.addProperty("settlement", e.settlement().toString());
            o.addProperty("severity", e.severity()); o.add("cause", e.cause().toJson()); o.addProperty("created", e.createdAt()); o.addProperty("start", e.startAt()); o.addProperty("end", e.endAt());
            o.addProperty("phase", e.phase().name()); o.addProperty("phaseAt", e.phaseAt()); o.addProperty("close", e.closeAt()); o.add("participants", Json.uuids(e.participants()));
            o.add("tags", Json.strings(e.tags())); o.add("consequences", Json.strings(e.consequences())); o.addProperty("resolution", e.resolution().name());
            o.addProperty("by", e.resolvedBy()); o.addProperty("outcome", e.outcome());
            return o;
        }
        private static WorldEventRecord fromJson(JsonObject o) {
            UUID id = Json.uuid(o, "id");
            if (id == null) return null;
            WorldEventRecord e = new WorldEventRecord(id, Json.enumOf(o, "type", WorldEventType.class, WorldEventType.EMERGENCY), Json.str(o, "title", ""), Json.uuid(o, "region"), Json.uuid(o, "settlement"),
                    Json.num(o, "severity", 0.5), Provenance.fromJson(Json.obj(o, "cause")), Json.lng(o, "created", 0), Json.lng(o, "start", 0), Json.lng(o, "end", 1));
            e.restore(Json.enumOf(o, "phase", WorldEventPhase.class, WorldEventPhase.PREPARATION), Json.lng(o, "start", 0), Json.lng(o, "end", 1), Json.lng(o, "phaseAt", 0), Json.lng(o, "close", 0),
                    Json.enumOf(o, "resolution", WorldEventRecord.Resolution.class, WorldEventRecord.Resolution.NONE), Json.str(o, "by", ""), Json.str(o, "outcome", ""));
            e.participants().addAll(Json.uuidList(Json.arr(o, "participants")));
            e.tags().addAll(Json.stringList(Json.arr(o, "tags")));
            e.consequences().addAll(Json.stringList(Json.arr(o, "consequences")));
            return e;
        }
        @Override public JsonObject write() {
            JsonObject p = new JsonObject();
            p.add("open", Json.array(world.events().open(), Events::toJson));
            p.add("archive", Json.array(world.events().archive(), Events::toJson));
            return p;
        }
        @Override public void read(JsonObject p) {
            for (WorldEventRecord e : Json.list(Json.arr(p, "open"), Events::fromJson)) world.events().restoreOpen(e);
            for (WorldEventRecord e : Json.list(Json.arr(p, "archive"), Events::fromJson)) world.events().restoreArchived(e);
        }
    }

    static final class Population implements StoreSection {
        private final WorldEngine world;
        Population(WorldEngine world) { this.world = world; }
        @Override public String domain() { return "world-population"; }
        @Override public String file() { return "world/population.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        @Override public boolean dirty() { return world.population().dirty(); }
        @Override public void clean() { world.population().clean(); }
        @Override public JsonObject write() {
            PopulationLedger l = world.population();
            JsonArray log = new JsonArray();
            for (PopulationLedger.Change c : l.log()) {
                JsonObject o = new JsonObject();
                o.addProperty("minute", c.minute()); o.addProperty("kind", c.kind().name()); if (c.settlement() != null) o.addProperty("settlement", c.settlement().toString());
                if (c.person() != null) o.addProperty("person", c.person().toString()); o.addProperty("name", c.name()); o.addProperty("detail", c.detail());
                log.add(o);
            }
            JsonObject counts = new JsonObject();
            l.settlements().forEach((k, v) -> counts.addProperty(k.toString(), v));
            JsonObject totals = new JsonObject();
            l.totals().forEach((k, v) -> totals.addProperty(k.name(), v));
            JsonObject p = new JsonObject();
            p.add("log", log); p.add("counts", counts); p.add("totals", totals);
            return p;
        }
        @Override public void read(JsonObject p) {
            PopulationLedger l = world.population();
            JsonObject counts = Json.obj(p, "counts");
            for (String k : counts.keySet()) try { l.report(UUID.fromString(k), Json.integer(counts, k, 0), Map.of()); } catch (IllegalArgumentException ignored) { }
            for (PopulationLedger.Change c : Json.list(Json.arr(p, "log"), o -> new PopulationLedger.Change(Json.lng(o, "minute", 0),
                    Json.enumOf(o, "kind", PopulationLedger.ChangeKind.class, PopulationLedger.ChangeKind.ARRIVAL), Json.uuid(o, "settlement"), Json.uuid(o, "person"), Json.str(o, "name", ""), Json.str(o, "detail", ""))))
                l.record(c);
            Map<PopulationLedger.ChangeKind, Long> totals = new EnumMap<>(PopulationLedger.ChangeKind.class);
            JsonObject t = Json.obj(p, "totals");
            for (String k : t.keySet()) try { totals.put(PopulationLedger.ChangeKind.valueOf(k), Json.lng(t, k, 0)); } catch (IllegalArgumentException ignored) { }
            l.restoreTotals(totals);
        }
    }
}
