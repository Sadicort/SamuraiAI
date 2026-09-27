package yadi.samuraiai.living.economy;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.event.NpcEvent;
import yadi.samuraiai.living.calendar.engine.CalendarEngine;
import yadi.samuraiai.living.calendar.engine.CalendarSettings;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.core.Season;
import yadi.samuraiai.living.core.WeatherKind;
import yadi.samuraiai.living.core.persistence.LivingStorage;
import yadi.samuraiai.living.economy.caravans.Caravan;
import yadi.samuraiai.living.economy.contracts.Contract;
import yadi.samuraiai.living.economy.engine.EconomyEngine;
import yadi.samuraiai.living.economy.engine.EconomyPorts;
import yadi.samuraiai.living.economy.engine.EconomySettings;
import yadi.samuraiai.living.economy.events.CaravanArrivedEvent;
import yadi.samuraiai.living.economy.events.CaravanDelayedEvent;
import yadi.samuraiai.living.economy.events.CaravanLostEvent;
import yadi.samuraiai.living.economy.events.PriceChangedEvent;
import yadi.samuraiai.living.economy.events.ScarcityStartedEvent;
import yadi.samuraiai.living.economy.inventory.ResourceLot;
import yadi.samuraiai.living.economy.persistence.EconomyStorage;
import yadi.samuraiai.living.economy.scarcity.MarketBalance;
import yadi.samuraiai.living.economy.wealth.WealthAccount;

class EconomyEngineTest {
    private final List<NpcEvent> events = new ArrayList<>();
    private final CalendarEngine calendar = new CalendarEngine(CalendarSettings::defaults, e -> { }, 5L);
    private final UUID a = UUID.randomUUID(), b = UUID.randomUUID(), regionA = UUID.randomUUID(), regionB = UUID.randomUUID(), edge = UUID.randomUUID();
    private final Map<String, Double> deposits = new HashMap<>(Map.of("wood", 500.0, "iron", 100.0, "coal", 100.0, "stone", 1000.0, "water", 100000.0, "clay", 100.0, "herbs", 50.0));
    private final Map<UUID, List<EconomyPorts.Worker>> workers = new HashMap<>();
    private final Map<UUID, Integer> population = new HashMap<>(Map.of(a, 8, b, 8));
    private double edgeDanger = 0.0;
    private boolean blocked;
    private long roadVersion = 1;

    private EconomyEngine engine(EconomySettings settings) {
        EconomyEngine e = new EconomyEngine(() -> settings, events::add, calendar, 11L);
        e.useWorld(new EconomyPorts.World() {
            @Override public String regionScope(UUID s) { return "region:" + (s.equals(a) ? regionA : regionB); }
            @Override public double extract(UUID region, String r, double amount) { double have = deposits.getOrDefault(r, 0.0); double t = Math.min(have, amount); deposits.put(r, have - t); return t; }
            @Override public double harvestAnimals(UUID region, String r, double amount, double share) { return r.equals("fish") ? amount : 0; }
            @Override public Optional<EconomyPorts.RouteInfo> route(UUID from, UUID to) { return Optional.of(new EconomyPorts.RouteInfo(List.of(new EconomyPorts.Leg(edge, 900, edgeDanger)), 0, roadVersion)); }
            @Override public long roadVersion() { return roadVersion; }
            @Override public boolean blocked(UUID x) { return blocked; }
            @Override public double edgeDanger(UUID x) { return edgeDanger; }
            @Override public double regionDanger(UUID region) { return 0; }
            @Override public double distance(UUID x, UUID y) { return 900; }
            @Override public List<UUID> settlements() { return List.of(a, b); }
            @Override public void travelled(UUID x, long m) { }
        });
        e.useVillages(new EconomyPorts.Villages() {
            @Override public List<EconomyPorts.Worker> workers(UUID s) { return workers.getOrDefault(s, List.of()); }
            @Override public int population(UUID s) { return population.getOrDefault(s, 0); }
            @Override public int children(UUID s) { return 0; }
            @Override public int visitors(UUID s) { return 0; }
            @Override public boolean marketOpen(UUID s) { return true; }
            @Override public int footfall(UUID s) { return 5; }
            @Override public int buildings(UUID s, String kind) { return kind.equals("FARM") ? 2 : kind.equals("WELL") ? 2 : 0; }
            @Override public String culture(UUID s) { return "village"; }
        });
        e.useCalendar(new EconomyPorts.Calendar() {
            @Override public double foodFactor() { return 1; }
            @Override public double fuelFactor() { return 1; }
            @Override public double tradeFactor() { return 1; }
            @Override public double travelFactor() { return 1; }
            @Override public double agricultureFactor(String r, String scope) { return 1; }
            @Override public boolean harvestSeason(String r) { return false; }
            @Override public boolean offSeason(String r) { return false; }
            @Override public WeatherKind weather(String scope) { return WeatherKind.SUNNY; }
            @Override public Map<String, Double> festivalDemand(String culture) { return Map.of(); }
        });
        e.registerSettlement(a, regionA, "Aldea A", true, true);
        e.registerSettlement(b, regionB, "Aldea B", true, false);
        return e;
    }
    private EconomyEngine engine() { return engine(EconomySettings.defaults()); }
    private <T> List<T> of(Class<T> type) { return events.stream().filter(type::isInstance).map(type::cast).toList(); }

    /** Runs one step of {@code minutes} on a settlement, after a worker put in {@code hours}. */
    private void work(EconomyEngine e, UUID settlement, UUID npc, String profession, double totalHours, long minutes) {
        workers.put(settlement, List.of(new EconomyPorts.Worker(npc, "W", profession, totalHours)));
        long from = calendar.now();
        calendar.advanceMinutes(minutes, "test");
        e.simulate(settlement, from, calendar.now());
    }

    @Test void gatheringTakesFromTheRegionAndRecordsProvenance() {
        EconomyEngine e = engine();
        UUID cutter = UUID.randomUUID();
        work(e, a, cutter, "woodcutter", 0, 60);            // first sight: only starts counting
        double before = e.stock(a, "wood"), depositBefore = deposits.get("wood");
        work(e, a, cutter, "woodcutter", 8, 1440);
        double made = e.stock(a, "wood") - before;
        assertTrue(made > 5, "a day with eight hours of woodcutting: " + made);
        assertTrue(deposits.get("wood") < depositBefore, "the timber came from the region's deposit");
        ResourceLot newest = e.storeOf(a).inventory().lotsOf("wood").stream().filter(l -> l.origin().kind().equals("producer")).findFirst().orElseThrow();
        assertEquals(cutter.toString(), newest.origin().id());
        deposits.put("wood", 0.0);
        double stock = e.stock(a, "wood");
        work(e, a, cutter, "woodcutter", 16, 1440);
        assertTrue(e.stock(a, "wood") <= stock + 1e-6, "an empty deposit yields nothing");
        assertTrue(e.wealth().of(cutter).orElseThrow().coins() > 0, "the worker was paid a wage from the treasury");
    }

    @Test void workshopsNeedTheirInputs() {
        EconomyEngine e = engine();
        UUID smith = UUID.randomUUID();
        work(e, a, smith, "blacksmith", 0, 60);
        double tools = e.stock(a, "tools");
        work(e, a, smith, "blacksmith", 8, 1440);
        assertEquals(tools, e.stock(a, "tools"), 1.0, "no iron, no coal: nothing forged (a few tools may be worn)");
        e.donate(a, "iron", 10, 0.8, Provenance.of("test", "", "hierro", 0));
        e.donate(a, "coal", 10, 0.8, Provenance.of("test", "", "carbón", 0));
        work(e, a, smith, "blacksmith", 16, 1440);
        assertTrue(e.stock(a, "tools") > tools, "with iron and coal the smith makes tools");
        assertTrue(e.stock(a, "iron") < 10, "the iron was used");
    }

    @Test void peopleConsumeAndScarcityRaisesPrices() {
        EconomyEngine e = engine();
        workers.put(a, List.of());
        long from = calendar.now();
        calendar.advanceMinutes(60, "test");
        e.simulate(a, from, calendar.now());
        double price = e.price(a, "rice");
        for (int day = 0; day < 8; day++) { from = calendar.now(); calendar.advanceMinutes(1440, "test"); e.simulate(a, from, calendar.now()); }
        assertTrue(e.settlement(a).orElseThrow().foodCoverDays() < 2);
        assertEquals(MarketBalance.State.SCARCE, e.settlement(a).orElseThrow().balance(EconomyEngine.FOOD).state());
        assertTrue(of(ScarcityStartedEvent.class).stream().anyMatch(x -> x.resource().equals(EconomyEngine.FOOD)));
        assertTrue(e.price(a, "rice") > price * 1.2, e.price(a, "rice") + " vs " + price);
        assertFalse(of(PriceChangedEvent.class).isEmpty());
        assertTrue(e.settlement(a).orElseThrow().shortfall().containsKey(EconomyEngine.FOOD));
        assertEquals(Optional.of("farmer"), e.mostNeededProfession(a));
        // the hungry settlement asks for rice with a delivery contract its treasury can pay, and only once
        var asked = e.contracts().stream().filter(c -> c.settlement().equals(a) && c.resource().equals("rice") && c.open()).toList();
        assertEquals(1, asked.size(), "one open rice contract: " + e.contracts());
        assertEquals(Contract.Kind.DELIVERY, asked.get(0).kind());
        assertTrue(asked.get(0).total() <= e.treasury(a).coins() + 1e-6, "never more than the treasury could pay when offered");
        assertEquals(EconomySettings.defaults().contractDays() * 1440L, asked.get(0).deadline() - asked.get(0).createdAt());
    }

    private void surplusAtAScarcityAtB(EconomyEngine e) {
        e.donate(a, "rice", 900, 0.8, Provenance.of("farm", "a-fields", "cosecha", calendar.now()));
        e.withdraw(b, "rice", 1000, "vaciar");
        workers.put(a, List.of()); workers.put(b, List.of());
        for (int i = 0; i < 2; i++) for (UUID s : List.of(a, b)) { long from = calendar.now(); calendar.advanceMinutes(90, "test"); e.simulate(s, from, calendar.now()); }
    }

    @Test void caravansCarrySurplusToScarcityAndGoodsKeepTheirOrigin() {
        EconomyEngine e = engine();
        surplusAtAScarcityAtB(e);
        double treasuryA = e.treasury(a).coins();
        assertTrue(e.planTrade() >= 1, "a merchant of A sees B's scarcity");
        Caravan c = e.caravans().iterator().next();
        assertEquals(a, c.origin());
        assertTrue(e.treasury(a).coins() > treasuryA, "the merchant paid A for the rice");
        e.advanceCaravans();
        for (int i = 0; i < 40 && c.state() != Caravan.State.COMPLETED; i++) { calendar.advanceMinutes(60, "test"); e.advanceCaravans(); }
        assertEquals(Caravan.State.COMPLETED, c.state());
        assertFalse(of(CaravanArrivedEvent.class).isEmpty());
        assertTrue(e.stock(b, "rice") > 0);
        List<ResourceLot> lots = e.storeOf(b).inventory().lotsOf("rice");
        assertTrue(lots.stream().anyMatch(l -> l.origin().kind().equals("farm") && l.origin().id().equals("a-fields")), "the rice in B still says it was grown in A's fields: " + lots);
        assertTrue(lots.stream().noneMatch(l -> l.origin().kind().equals("producer")), "none of it was made in B");
        assertEquals(1, e.routes().iterator().next().arrived());
    }

    @Test void dangerousRoadsLoseCaravansAndBlockedRoadsDelayThem() {
        EconomySettings unguarded = EconomySettings.builder().set("caravanGuards", 0).set("ambushScale", 10.0).build();
        // An ambush is never certain (at most 0.95 per leg, and caravan ids are random), so up to five independent caravans try.
        EconomyEngine e = null;
        Caravan c = null;
        for (int attempt = 0; attempt < 5 && of(yadi.samuraiai.living.economy.events.CaravanAmbushedEvent.class).isEmpty(); attempt++) {
            e = engine(unguarded);
            edgeDanger = 0.9;
            surplusAtAScarcityAtB(e);
            e.planTrade();
            c = e.caravans().iterator().next();
            e.advanceCaravans();
            for (int i = 0; i < 20 && c.active(); i++) { calendar.advanceMinutes(60, "test"); e.advanceCaravans(); }
        }
        assertFalse(of(yadi.samuraiai.living.economy.events.CaravanAmbushedEvent.class).isEmpty(), "an unguarded caravan on a road this dangerous is ambushed");
        assertTrue(c.state() == Caravan.State.LOST ? !of(CaravanLostEvent.class).isEmpty() : e.ledger().total(yadi.samuraiai.living.economy.ledger.MovementRecord.Kind.LOOTED, "rice") > 0,
                "either the caravan is lost or part of its cargo was looted");

        EconomyEngine f = engine();
        edgeDanger = 0.0;
        surplusAtAScarcityAtB(f);
        f.planTrade();
        Caravan d = f.caravans().iterator().next();
        blocked = true;
        f.advanceCaravans();
        for (int i = 0; i < 3; i++) { calendar.advanceMinutes(60, "test"); f.advanceCaravans(); }
        assertEquals(Caravan.State.DELAYED, d.state());
        assertFalse(of(CaravanDelayedEvent.class).isEmpty());
        blocked = false;
        for (int i = 0; i < 40 && d.state() != Caravan.State.COMPLETED; i++) { calendar.advanceMinutes(60, "test"); f.advanceCaravans(); }
        assertEquals(Caravan.State.COMPLETED, d.state());
    }

    @Test void contractsAreDeliveredAndPaidOrExpire() {
        EconomyEngine e = engine();
        UUID player = UUID.randomUUID();
        Contract c = e.offerContract(Contract.Kind.DELIVERY, a, "iron", 20, 9, 3, 0, "el herrero necesita hierro");
        assertEquals(10, e.deliverToContract(c.id(), player, "Jugador", 10, 0.7, Provenance.of("player", player.toString(), "Jugador", 0)), 1e-9);
        assertEquals(Contract.State.OPEN, c.state());
        e.deliverToContract(c.id(), player, "Jugador", 15, 0.7, Provenance.of("player", player.toString(), "Jugador", 0));
        assertEquals(Contract.State.FULFILLED, c.state());
        assertEquals(180, e.wealth().of(player).orElseThrow().coins(), 1e-6);
        Contract late = e.offerContract(Contract.Kind.DELIVERY, a, "stone", 10, 1, 1, 0, "muro");
        calendar.advanceMinutes(2 * 1440, "test");
        for (int i = 0; i < 20; i++) e.tick();
        assertEquals(Contract.State.EXPIRED, late.state());
    }

    @Test void moneyIsNeverCreatedByTrade() {
        EconomyEngine e = engine();
        UUID player = UUID.randomUUID();
        e.sellFromPlayer(a, player, "Jugador", "iron", 5, 0.8);
        e.buyForPlayer(a, player, "Jugador", "iron", 2);
        surplusAtAScarcityAtB(e);
        e.planTrade();
        for (int i = 0; i < 30; i++) { calendar.advanceMinutes(60, "test"); e.advanceCaravans(); }
        double total = 0;
        for (WealthAccount acc : e.wealth().all()) total += acc.coins();
        assertEquals(e.wealth().minted(), total, 1e-6, "every coin in every account was minted with a provenance");
    }

    @Test void merchantsWhoTrustAPlayerTradeOnBetterTerms() {
        EconomyEngine e = engine();
        UUID player = UUID.randomUUID(), merchant = UUID.randomUUID();
        double neutralBuy = e.playerBuyPrice(a, "rice", player), neutralSell = e.playerSellPrice(a, "rice", player);
        assertEquals(e.price(a, "rice") * 1.1, neutralBuy, 1e-9, "no embodied merchant: neutral terms");
        e.registerMerchant(merchant, "Tomoe", a);
        e.useRelations((m, p) -> m.equals(merchant) && p.equals(player) ? 0.95 : 0.5);
        assertTrue(e.playerBuyPrice(a, "rice", player) < neutralBuy, "a trusted player pays less");
        assertTrue(e.playerSellPrice(a, "rice", player) > neutralSell, "and is paid more");
        assertEquals(neutralBuy, e.playerBuyPrice(a, "rice", UUID.randomUUID()), 1e-9, "a stranger gets the usual price");
    }

    @Test void theEconomySurvivesARestart() throws Exception {
        Path dir = Files.createTempDirectory("living-economy");
        EconomyEngine x = engine();
        surplusAtAScarcityAtB(x);
        x.planTrade();
        x.offerContract(Contract.Kind.DELIVERY, a, "iron", 20, 9, 3, 0, "hierro");
        LivingStorage store = new LivingStorage(dir, true);
        EconomyStorage.sections(x).forEach(store::register);
        assertEquals(3, store.saveAll());
        EconomyEngine y = new EconomyEngine(EconomySettings::defaults, events::add, calendar, 11L);
        LivingStorage load = new LivingStorage(dir, true);
        EconomyStorage.sections(y).forEach(load::register);
        load.loadAll().values().forEach(r -> assertTrue(r.usable(), r.detail()));
        assertEquals(x.stock(a, "rice"), y.stock(a, "rice"), 1e-6);
        assertEquals(x.treasury(a).coins(), y.treasury(a).coins(), 1e-6);
        assertEquals(1, y.caravans().size());
        assertEquals(x.contracts().size(), y.contracts().size(), "the offered contract and the ones scarcity asked for");
        assertTrue(y.contracts().stream().anyMatch(c -> c.resource().equals("iron") && c.settlement().equals(a)));
        assertEquals(x.storeOf(b).inventory().totals(), y.storeOf(b).inventory().totals());
        assertEquals(x.wealth().minted(), y.wealth().minted(), 1e-6);
    }
}
