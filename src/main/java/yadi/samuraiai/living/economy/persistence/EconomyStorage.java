package yadi.samuraiai.living.economy.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.storage.Json;
import yadi.samuraiai.living.core.persistence.StoreSection;
import yadi.samuraiai.living.economy.caravans.Caravan;
import yadi.samuraiai.living.economy.contracts.Contract;
import yadi.samuraiai.living.economy.engine.EconomyEngine;
import yadi.samuraiai.living.economy.inventory.ResourceLot;
import yadi.samuraiai.living.economy.ledger.MovementRecord;
import yadi.samuraiai.living.economy.markets.MarketRuntime;
import yadi.samuraiai.living.economy.memory.TradeMemory;
import yadi.samuraiai.living.economy.prices.PricePoint;
import yadi.samuraiai.living.economy.scarcity.MarketBalance;
import yadi.samuraiai.living.economy.simulation.SettlementEconomy;
import yadi.samuraiai.living.economy.storage.Warehouse;
import yadi.samuraiai.living.economy.taxation.TaxPolicy;
import yadi.samuraiai.living.economy.trade_routes.TradeRoute;
import yadi.samuraiai.living.economy.traders.Merchant;
import yadi.samuraiai.living.economy.wealth.WealthAccount;

/**
 * The economy's persistent sections: the state (settlement economies with taxes, balances, prices and accounted hours;
 * accounts; merchants; caravans; routes; contracts), the stores (every warehouse with its lots and their provenance) and the
 * history (ledger and trade memory).
 */
public final class EconomyStorage {
    public static final int SCHEMA = 1;

    private EconomyStorage() { }

    public static List<StoreSection> sections(EconomyEngine e) { return List.of(new Stores(e), new State(e), new History(e)); }

    private static JsonObject map(Map<String, Double> m) { JsonObject o = new JsonObject(); m.forEach(o::addProperty); return o; }
    private static Map<String, Double> unmap(JsonObject o) { Map<String, Double> m = new LinkedHashMap<>(); for (String k : o.keySet()) m.put(k, Json.num(o, k, 0)); return m; }

    static final class Stores implements StoreSection {
        private final EconomyEngine e;
        Stores(EconomyEngine e) { this.e = e; }
        @Override public String domain() { return "economy-stores"; }
        @Override public String file() { return "economy/stores.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        private long saved = -1;
        @Override public boolean dirty() { return e.revision() != saved; }
        @Override public void clean() { saved = e.revision(); }
        @Override public JsonObject write() {
            JsonArray a = new JsonArray();
            for (Warehouse w : e.warehouses()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", w.id().toString()); o.addProperty("kind", w.ownerKind().name()); if (w.owner() != null) o.addProperty("owner", w.owner().toString());
                if (w.settlement() != null) o.addProperty("settlement", w.settlement().toString()); o.addProperty("location", w.location()); o.addProperty("capacity", w.capacity());
                o.addProperty("security", w.security()); o.addProperty("in", w.inTotal()); o.addProperty("out", w.outTotal());
                o.add("lots", Json.array(w.inventory().allLots(), ResourceLot::toJson));
                a.add(o);
            }
            JsonObject p = new JsonObject();
            p.add("warehouses", a);
            return p;
        }
        @Override public void read(JsonObject p) {
            for (JsonElement el : Json.arr(p, "warehouses")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID id = Json.uuid(o, "id");
                if (id == null) continue;
                Warehouse w = new Warehouse(id, Json.enumOf(o, "kind", Warehouse.OwnerKind.class, Warehouse.OwnerKind.SETTLEMENT), Json.uuid(o, "owner"), Json.uuid(o, "settlement"),
                        Json.str(o, "location", ""), Json.num(o, "capacity", 1000), Json.num(o, "security", 0.5), e.settings().maxLotsPerResource(), e.clock().minutesPerDay());
                for (ResourceLot l : Json.list(Json.arr(o, "lots"), ResourceLot::fromJson)) w.inventory().add(l);
                w.restoreTotals(Json.num(o, "in", 0), Json.num(o, "out", 0));
                w.clean();
                e.restoreWarehouse(w);
            }
        }
    }

    static final class State implements StoreSection {
        private final EconomyEngine e;
        State(EconomyEngine e) { this.e = e; }
        @Override public String domain() { return "economy-state"; }
        @Override public String file() { return "economy/state.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        private long saved = -1;
        @Override public boolean dirty() { return e.revision() != saved; }
        @Override public void clean() { saved = e.revision(); }

        @Override public JsonObject write() {
            JsonObject p = new JsonObject();
            JsonArray settlements = new JsonArray();
            for (SettlementEconomy se : e.settlements()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", se.settlement().toString()); if (se.region() != null) o.addProperty("region", se.region().toString()); o.addProperty("name", se.name());
                o.addProperty("warehouse", se.warehouse().toString()); o.addProperty("treasury", se.treasury().toString());
                if (se.templeAccount() != null) o.addProperty("temple", se.templeAccount().toString());
                o.addProperty("market", se.market() != null);
                if (se.market() != null) o.add("stalls", Json.uuids(se.market().stalls()));
                TaxPolicy t = se.taxes();
                JsonObject tax = new JsonObject();
                tax.addProperty("market", t.marketTax()); tax.addProperty("tithe", t.templeTithe()); tax.addProperty("levy", t.levyPerCitizen()); tax.addProperty("faction", t.factionShare());
                tax.addProperty("cMarket", t.totalMarket()); tax.addProperty("cTithe", t.totalTithe()); tax.addProperty("cLevy", t.totalLevy()); tax.addProperty("cFaction", t.totalFaction());
                o.add("taxes", tax);
                JsonArray balances = new JsonArray();
                for (MarketBalance b : se.balances().values()) { JsonObject x = new JsonObject(); x.addProperty("r", b.resource()); x.addProperty("state", b.state().name()); x.addProperty("cover", Double.isFinite(b.coverDays()) ? b.coverDays() : 1e9); x.addProperty("since", b.since()); balances.add(x); }
                o.add("balances", balances);
                JsonObject hours = new JsonObject(); se.accountedHours().forEach((k, v) -> hours.addProperty(k.toString(), v)); o.add("hours", hours);
                JsonArray prices = new JsonArray();
                for (PricePoint pp : e.prices(se.settlement()).values()) { JsonObject x = new JsonObject(); x.addProperty("r", pp.resource()); x.addProperty("p", pp.price()); x.addProperty("s", pp.supply()); x.addProperty("d", pp.demand()); x.addProperty("at", pp.updatedAt()); prices.add(x); }
                o.add("prices", prices);
                o.addProperty("lastStep", se.lastStep()); o.addProperty("lastPrices", se.lastPrices()); o.addProperty("specialUntil", se.specialMarketUntil());
                o.addProperty("foodCover", Double.isFinite(se.foodCoverDays()) ? se.foodCoverDays() : 1e9); o.addProperty("foodNeed", se.dailyFoodNeed()); o.addProperty("prosperity", se.prosperity());
                settlements.add(o);
            }
            p.add("settlements", settlements);
            JsonArray accounts = new JsonArray();
            for (WealthAccount a : e.wealth().all()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", a.id().toString()); o.addProperty("kind", a.kind().name()); o.addProperty("owner", a.owner().toString()); o.addProperty("label", a.label());
                o.addProperty("coins", a.coins()); o.addProperty("in", a.income()); o.addProperty("out", a.expenses()); o.addProperty("wIn", a.windowIncome()); o.addProperty("wOut", a.windowExpenses());
                o.addProperty("wStart", a.windowStart()); o.add("properties", Json.uuids(a.properties()));
                accounts.add(o);
            }
            p.add("accounts", accounts);
            p.addProperty("minted", e.wealth().minted());
            JsonArray merchants = new JsonArray();
            for (Merchant m : e.merchants()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", m.id().toString()); if (m.npc() != null) o.addProperty("npc", m.npc().toString()); o.addProperty("name", m.name()); o.addProperty("home", m.home().toString());
                o.addProperty("account", m.account().toString()); o.addProperty("stock", m.stock().toString()); o.addProperty("personality", m.personality().name());
                o.addProperty("reputation", m.reputation()); if (m.caravan() != null) o.addProperty("caravan", m.caravan().toString()); o.addProperty("available", m.availableAt());
                o.addProperty("trades", m.trades()); o.addProperty("profit", m.profit());
                JsonObject clients = new JsonObject(); m.clients().forEach((k, v) -> clients.addProperty(k.toString(), v)); o.add("clients", clients);
                o.add("contracts", Json.uuids(m.contracts()));
                merchants.add(o);
            }
            p.add("merchants", merchants);
            JsonArray caravans = new JsonArray();
            for (Caravan c : e.caravans()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", c.id().toString()); o.addProperty("type", c.type().name()); o.addProperty("route", c.route().toString()); o.addProperty("origin", c.origin().toString());
                o.addProperty("destination", c.destination().toString()); o.addProperty("merchant", c.merchant().toString()); o.addProperty("cargo", c.cargo().toString());
                o.addProperty("guards", c.guards()); o.addProperty("carts", c.carts()); o.addProperty("capacity", c.capacity()); o.addProperty("speed", c.speed());
                o.addProperty("created", c.createdAt()); o.addProperty("depart", c.departAt()); o.addProperty("state", c.state().name()); o.addProperty("progress", c.progress());
                o.addProperty("back", c.returnProgress()); o.addProperty("leg", c.legIndex()); o.addProperty("ambushes", c.ambushes()); o.addProperty("cost", c.purchaseCost());
                o.addProperty("sale", c.saleValue()); o.addProperty("arrived", c.arrivedAt()); o.addProperty("delayed", c.delayedSince()); o.addProperty("note", c.note());
                caravans.add(o);
            }
            p.add("caravans", caravans);
            JsonArray routes = new JsonArray();
            for (TradeRoute r : e.routes()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", r.id().toString()); o.addProperty("origin", r.origin().toString()); o.addProperty("destination", r.destination().toString());
                JsonArray legs = new JsonArray();
                for (TradeRoute.Leg l : r.legs()) { JsonObject x = new JsonObject(); x.addProperty("edge", l.edge().toString()); x.addProperty("length", l.length()); x.addProperty("danger", l.danger()); legs.add(x); }
                o.add("legs", legs); o.addProperty("bridges", r.bridges()); o.addProperty("status", r.status().name()); o.addProperty("version", r.version()); o.addProperty("last", r.lastUsed());
                o.addProperty("sent", r.sent()); o.addProperty("arrived", r.arrived()); o.addProperty("lost", r.lost()); o.addProperty("value", r.valueCarried());
                routes.add(o);
            }
            p.add("routes", routes);
            JsonArray contracts = new JsonArray();
            for (Contract c : e.contracts()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", c.id().toString()); o.addProperty("kind", c.kind().name()); o.addProperty("issuer", c.issuer().toString()); o.addProperty("settlement", c.settlement().toString());
                o.addProperty("resource", c.resource()); o.addProperty("quantity", c.quantity()); o.addProperty("price", c.unitPrice()); o.addProperty("created", c.createdAt());
                o.addProperty("deadline", c.deadline()); o.addProperty("trust", c.trustRequired()); o.addProperty("reason", c.reason()); o.addProperty("state", c.state().name());
                if (c.counterparty() != null) o.addProperty("counterparty", c.counterparty().toString()); if (c.quest() != null) o.addProperty("quest", c.quest().toString());
                o.addProperty("delivered", c.delivered()); o.addProperty("resolved", c.resolvedAt());
                contracts.add(o);
            }
            p.add("contracts", contracts);
            p.addProperty("lastCaravan", e.lastCaravanMinute());
            p.addProperty("seed", e.seed());
            return p;
        }

        @Override public void read(JsonObject p) {
            e.useSeed(Json.lng(p, "seed", e.seed()));
            for (JsonElement el : Json.arr(p, "accounts")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID id = Json.uuid(o, "id"), owner = Json.uuid(o, "owner");
                if (id == null || owner == null) continue;
                WealthAccount a = new WealthAccount(id, Json.enumOf(o, "kind", WealthAccount.OwnerKind.class, WealthAccount.OwnerKind.NPC), owner, Json.str(o, "label", ""), Json.lng(o, "wStart", 0));
                a.restore(Json.num(o, "coins", 0), Json.num(o, "in", 0), Json.num(o, "out", 0), Json.num(o, "wIn", 0), Json.num(o, "wOut", 0), Json.lng(o, "wStart", 0));
                a.properties().addAll(Json.uuidList(Json.arr(o, "properties")));
                e.wealth().restore(a);
            }
            e.wealth().restoreMinted(Json.num(p, "minted", 0));
            for (JsonElement el : Json.arr(p, "settlements")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID id = Json.uuid(o, "id"), warehouse = Json.uuid(o, "warehouse"), treasury = Json.uuid(o, "treasury");
                if (id == null || warehouse == null || treasury == null) continue;
                JsonObject tax = Json.obj(o, "taxes");
                TaxPolicy t = new TaxPolicy(Json.num(tax, "market", 0.05), Json.num(tax, "tithe", 0.02), Json.num(tax, "levy", 0.05), Json.num(tax, "faction", 0));
                t.restoreTotals(Json.num(tax, "cMarket", 0), Json.num(tax, "cTithe", 0), Json.num(tax, "cLevy", 0), Json.num(tax, "cFaction", 0));
                SettlementEconomy se = new SettlementEconomy(id, Json.uuid(o, "region"), Json.str(o, "name", ""), warehouse, treasury, t, Json.lng(o, "lastStep", 0));
                se.templeAccount(Json.uuid(o, "temple"));
                if (Json.bool(o, "market", false)) { MarketRuntime m = new MarketRuntime(id); m.stalls().addAll(Json.uuidList(Json.arr(o, "stalls"))); se.market(m); }
                for (JsonElement b : Json.arr(o, "balances")) if (b.isJsonObject()) {
                    JsonObject x = b.getAsJsonObject();
                    se.balance(Json.str(x, "r", "")).restore(Json.enumOf(x, "state", MarketBalance.State.class, MarketBalance.State.NORMAL), Json.num(x, "cover", 1e9), Json.lng(x, "since", 0));
                }
                JsonObject hours = Json.obj(o, "hours");
                for (String k : hours.keySet()) try { se.accountedHours().put(UUID.fromString(k), Json.num(hours, k, 0)); } catch (IllegalArgumentException ignored) { }
                se.lastPrices(Json.lng(o, "lastPrices", 0)); se.specialMarketUntil(Json.lng(o, "specialUntil", 0)); se.foodCoverDays(Json.num(o, "foodCover", 1e9));
                se.dailyFoodNeed(Json.num(o, "foodNeed", 0)); se.prosperity(Json.num(o, "prosperity", 0.5));
                e.restoreSettlement(se);
                for (JsonElement pr : Json.arr(o, "prices")) if (pr.isJsonObject()) {
                    JsonObject x = pr.getAsJsonObject();
                    e.restorePrice(id, new PricePoint(Json.str(x, "r", ""), Json.num(x, "p", 1), Json.num(x, "s", 0), Json.num(x, "d", 0), Map.of(), Json.lng(x, "at", 0)));
                }
                se.clean();
            }
            for (JsonElement el : Json.arr(p, "merchants")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID id = Json.uuid(o, "id"), home = Json.uuid(o, "home"), account = Json.uuid(o, "account"), stock = Json.uuid(o, "stock");
                if (id == null || home == null || account == null || stock == null) continue;
                Merchant m = new Merchant(id, Json.uuid(o, "npc"), Json.str(o, "name", ""), home, account, stock, Json.enumOf(o, "personality", Merchant.Personality.class, Merchant.Personality.HONORABLE));
                m.restoreStats(Json.num(o, "reputation", 50), Json.uuid(o, "caravan"), Json.lng(o, "available", 0), Json.integer(o, "trades", 0), Json.num(o, "profit", 0));
                JsonObject clients = Json.obj(o, "clients");
                for (String k : clients.keySet()) try { m.clients().put(UUID.fromString(k), Json.integer(clients, k, 0)); } catch (IllegalArgumentException ignored) { }
                m.contracts().addAll(Json.uuidList(Json.arr(o, "contracts")));
                e.restoreMerchant(m);
            }
            for (JsonElement el : Json.arr(p, "routes")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID id = Json.uuid(o, "id"), origin = Json.uuid(o, "origin"), destination = Json.uuid(o, "destination");
                if (id == null || origin == null || destination == null) continue;
                TradeRoute r = new TradeRoute(id, origin, destination);
                List<TradeRoute.Leg> legs = new java.util.ArrayList<>();
                for (JsonElement l : Json.arr(o, "legs")) if (l.isJsonObject()) { JsonObject x = l.getAsJsonObject(); UUID edge = Json.uuid(x, "edge"); if (edge != null) legs.add(new TradeRoute.Leg(edge, Json.num(x, "length", 1), Json.num(x, "danger", 0))); }
                r.plan(legs, Json.integer(o, "bridges", 0), Json.lng(o, "version", 0));
                r.restoreStats(Json.enumOf(o, "status", TradeRoute.Status.class, TradeRoute.Status.OPEN), Json.lng(o, "version", 0), Json.lng(o, "last", 0), Json.integer(o, "sent", 0),
                        Json.integer(o, "arrived", 0), Json.integer(o, "lost", 0), Json.num(o, "value", 0));
                e.restoreRoute(r);
            }
            for (JsonElement el : Json.arr(p, "caravans")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID id = Json.uuid(o, "id");
                if (id == null) continue;
                Caravan c = new Caravan(id, Json.enumOf(o, "type", Caravan.Type.class, Caravan.Type.COMMERCIAL), Json.uuid(o, "route"), Json.uuid(o, "origin"), Json.uuid(o, "destination"),
                        Json.uuid(o, "merchant"), Json.uuid(o, "cargo"), Json.integer(o, "guards", 0), Json.integer(o, "carts", 1), Json.num(o, "capacity", 400), Json.num(o, "speed", 3),
                        Json.lng(o, "created", 0), Json.lng(o, "depart", 0));
                c.restore(Json.enumOf(o, "state", Caravan.State.class, Caravan.State.PLANNED), Json.num(o, "progress", 0), Json.num(o, "back", 0), Json.integer(o, "leg", 0),
                        Json.integer(o, "ambushes", 0), Json.num(o, "cost", 0), Json.num(o, "sale", 0), Json.lng(o, "arrived", 0), Json.lng(o, "delayed", 0), Json.str(o, "note", ""));
                e.restoreCaravan(c);
            }
            for (JsonElement el : Json.arr(p, "contracts")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID id = Json.uuid(o, "id"), issuer = Json.uuid(o, "issuer"), settlement = Json.uuid(o, "settlement");
                if (id == null || issuer == null || settlement == null) continue;
                Contract c = new Contract(id, Json.enumOf(o, "kind", Contract.Kind.class, Contract.Kind.DELIVERY), issuer, settlement, Json.str(o, "resource", ""), Json.num(o, "quantity", 0),
                        Json.num(o, "price", 0), Json.lng(o, "created", 0), Json.lng(o, "deadline", 0), Json.num(o, "trust", 0), Json.str(o, "reason", ""));
                c.restore(Json.enumOf(o, "state", Contract.State.class, Contract.State.OPEN), Json.uuid(o, "counterparty"), Json.uuid(o, "quest"), Json.num(o, "delivered", 0), Json.lng(o, "resolved", 0));
                e.restoreContract(c);
            }
            e.restoreLastCaravanMinute(Json.lng(p, "lastCaravan", Long.MIN_VALUE));
        }
    }

    static final class History implements StoreSection {
        private final EconomyEngine e;
        History(EconomyEngine e) { this.e = e; }
        @Override public String domain() { return "economy-history"; }
        @Override public String file() { return "economy/history.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        private long saved = -1;
        @Override public boolean dirty() { return e.revision() != saved; }
        @Override public void clean() { saved = e.revision(); e.clean(); }
        @Override public JsonObject write() {
            JsonObject p = new JsonObject();
            p.add("ledger", Json.array(e.ledger().recent(Integer.MAX_VALUE), MovementRecord::toJson));
            p.addProperty("recorded", e.ledger().recorded());
            JsonArray snaps = new JsonArray();
            for (var entry : e.memory().all().entrySet())
                for (TradeMemory.Snapshot s : entry.getValue()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("settlement", entry.getKey().toString()); o.addProperty("day", s.day()); o.add("prices", map(s.prices())); o.add("stock", map(s.stock()));
                    o.add("produced", map(s.produced())); o.add("consumed", map(s.consumed())); o.add("imported", map(s.imported())); o.add("exported", map(s.exported()));
                    o.add("shortfall", map(s.shortfall())); o.addProperty("treasury", s.treasury());
                    snaps.add(o);
                }
            p.add("snapshots", snaps);
            JsonArray happenings = new JsonArray();
            for (TradeMemory.Happening h : e.memory().allHappenings()) {
                JsonObject o = new JsonObject();
                o.addProperty("minute", h.minute()); if (h.settlement() != null) o.addProperty("settlement", h.settlement().toString()); o.addProperty("kind", h.kind()); o.addProperty("detail", h.detail());
                happenings.add(o);
            }
            p.add("happenings", happenings);
            return p;
        }
        @Override public void read(JsonObject p) {
            for (MovementRecord r : Json.list(Json.arr(p, "ledger"), MovementRecord::fromJson)) e.ledger().add(r);
            e.ledger().restoreRecorded(Json.lng(p, "recorded", 0));
            for (JsonElement el : Json.arr(p, "snapshots")) if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                UUID s = Json.uuid(o, "settlement");
                if (s == null) continue;
                e.memory().snapshot(s, new TradeMemory.Snapshot(Json.lng(o, "day", 0), unmap(Json.obj(o, "prices")), unmap(Json.obj(o, "stock")), unmap(Json.obj(o, "produced")),
                        unmap(Json.obj(o, "consumed")), unmap(Json.obj(o, "imported")), unmap(Json.obj(o, "exported")), unmap(Json.obj(o, "shortfall")), Json.num(o, "treasury", 0)));
            }
            for (TradeMemory.Happening h : Json.list(Json.arr(p, "happenings"), o -> new TradeMemory.Happening(Json.lng(o, "minute", 0), Json.uuid(o, "settlement"), Json.str(o, "kind", ""), Json.str(o, "detail", ""))))
                e.memory().happened(h);
        }
    }

}
