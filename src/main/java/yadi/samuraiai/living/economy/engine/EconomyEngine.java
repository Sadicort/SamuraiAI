package yadi.samuraiai.living.economy.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.event.NpcEvent;
import yadi.samuraiai.living.core.Dice;
import yadi.samuraiai.living.core.LivingIds;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.core.Skill;
import yadi.samuraiai.living.core.WorldClock;
import yadi.samuraiai.living.economy.caravans.Caravan;
import yadi.samuraiai.living.economy.consumption.NeedProfile;
import yadi.samuraiai.living.economy.contracts.Contract;
import yadi.samuraiai.living.economy.events.CaravanAmbushedEvent;
import yadi.samuraiai.living.economy.events.CaravanArrivedEvent;
import yadi.samuraiai.living.economy.events.CaravanCompletedEvent;
import yadi.samuraiai.living.economy.events.CaravanCreatedEvent;
import yadi.samuraiai.living.economy.events.CaravanDelayedEvent;
import yadi.samuraiai.living.economy.events.CaravanDepartedEvent;
import yadi.samuraiai.living.economy.events.CaravanLostEvent;
import yadi.samuraiai.living.economy.events.ContractCreatedEvent;
import yadi.samuraiai.living.economy.events.ContractResolvedEvent;
import yadi.samuraiai.living.economy.events.EconomicEventEvent;
import yadi.samuraiai.living.economy.events.PriceChangedEvent;
import yadi.samuraiai.living.economy.events.ResourceConsumedEvent;
import yadi.samuraiai.living.economy.events.ResourceProducedEvent;
import yadi.samuraiai.living.economy.events.ScarcityEndedEvent;
import yadi.samuraiai.living.economy.events.ScarcityStartedEvent;
import yadi.samuraiai.living.economy.events.SurplusEndedEvent;
import yadi.samuraiai.living.economy.events.SurplusStartedEvent;
import yadi.samuraiai.living.economy.events.TaxCollectedEvent;
import yadi.samuraiai.living.economy.events.TradeCompletedEvent;
import yadi.samuraiai.living.economy.events.TradeRouteDisruptedEvent;
import yadi.samuraiai.living.economy.events.TradeRouteRestoredEvent;
import yadi.samuraiai.living.economy.events.WarehouseLossEvent;
import yadi.samuraiai.living.economy.inventory.ResourceLot;
import yadi.samuraiai.living.economy.ledger.EconomyLedger;
import yadi.samuraiai.living.economy.ledger.MovementRecord;
import yadi.samuraiai.living.economy.markets.MarketRuntime;
import yadi.samuraiai.living.economy.memory.TradeMemory;
import yadi.samuraiai.living.economy.metrics.EconomyMetrics;
import yadi.samuraiai.living.economy.prices.PriceEngine;
import yadi.samuraiai.living.economy.prices.PricePoint;
import yadi.samuraiai.living.economy.production.Recipe;
import yadi.samuraiai.living.economy.production.RecipeCatalog;
import yadi.samuraiai.living.economy.resources.ResourceCatalog;
import yadi.samuraiai.living.economy.resources.ResourceDef;
import yadi.samuraiai.living.economy.scarcity.MarketBalance;
import yadi.samuraiai.living.economy.simulation.SettlementEconomy;
import yadi.samuraiai.living.economy.storage.Warehouse;
import yadi.samuraiai.living.economy.taxation.TaxPolicy;
import yadi.samuraiai.living.economy.trade_routes.TradeRoute;
import yadi.samuraiai.living.economy.traders.Merchant;
import yadi.samuraiai.living.economy.wealth.WealthAccount;
import yadi.samuraiai.living.economy.wealth.WealthEngine;

/**
 * The Economy & Trade Engine: "what is produced, consumed, stored, bought, sold and carried". Nothing economic appears from
 * nothing — every lot of goods has an origin (a deposit, the animals of a region, a farm, a workshop, a caravan, a player) and
 * every coin entered through a recorded mint. It owns resources, production, consumption, warehouses, markets, prices,
 * merchants, caravans, trade routes, contracts, taxes, wealth, scarcity and surplus, and trade memory.
 *
 * <p>A settlement's economy advances in steps given by the world simulation (an hour near a player, a day far away):
 * workers' hours become goods, people consume, food spoils, balances and prices update. Caravans travel in the background on
 * their own cadence. Pure Java; the other engines are reached through {@link EconomyPorts}.
 */
public final class EconomyEngine {
    public static final String FOOD = "food";

    private final Supplier<EconomySettings> settingsSupplier;
    private final WorldClock clock;
    private final EconomyMetrics metrics = new EconomyMetrics();
    private final EconomyLedger ledger = new EconomyLedger();
    private final WealthEngine wealth = new WealthEngine(ledger);
    private final TradeMemory memory = new TradeMemory();
    private final PriceEngine prices = new PriceEngine();
    private final Map<UUID, SettlementEconomy> settlements = new LinkedHashMap<>();
    private final Map<UUID, Warehouse> warehouses = new LinkedHashMap<>();
    private final Map<UUID, Merchant> merchants = new LinkedHashMap<>();
    private final Map<UUID, Caravan> caravans = new LinkedHashMap<>();
    private final Map<String, TradeRoute> routes = new LinkedHashMap<>();
    private final Map<UUID, Contract> contracts = new LinkedHashMap<>();
    private EventSink bus;
    private EconomySettings settings;
    private ResourceCatalog resources;
    private RecipeCatalog recipes;
    private NeedProfile needs;
    private Dice dice;
    private long seed, tickCount, lastCaravanMinute = Long.MIN_VALUE, lastLevyDay = Long.MIN_VALUE;
    private EconomyPorts.World world;
    private EconomyPorts.Villages villages;
    private EconomyPorts.Calendar calendar;
    private EconomyPorts.Relations relations = EconomyPorts.NEUTRAL_TRUST;
    private EconomyPorts.Chronicle chronicle = EconomyPorts.SILENT;
    private boolean dirty;
    private long revision;

    public EconomyEngine(Supplier<EconomySettings> settings, EventSink bus, WorldClock clock, long seed) {
        this.settingsSupplier = Objects.requireNonNull(settings);
        this.bus = Objects.requireNonNull(bus);
        this.clock = Objects.requireNonNull(clock);
        this.seed = seed;
        this.dice = new Dice(seed);
        rebuild(settings.get());
    }

    private void rebuild(EconomySettings s) {
        settings = s;
        resources = new ResourceCatalog(s.resources());
        recipes = new RecipeCatalog(s.recipes());
        needs = NeedProfile.parse(s.needs().isEmpty() ? null : s.needs().get(0));
        prices.configure(s.priceElasticity(), s.priceSmoothing(), 0.25D, 5.0D);
        ledger.configure(s.ledgerMax(), s.ledgerPerSettlement());
        memory.configure(s.memoryDays(), 500);
    }

    private void refresh() { EconomySettings s = settingsSupplier.get(); if (s != settings) rebuild(s); }

    public void useEventSink(EventSink sink) { bus = Objects.requireNonNull(sink); }
    public void useWorld(EconomyPorts.World w) { world = Objects.requireNonNull(w); }
    public void useVillages(EconomyPorts.Villages v) { villages = Objects.requireNonNull(v); }
    public void useCalendar(EconomyPorts.Calendar c) { calendar = Objects.requireNonNull(c); }
    public void useRelations(EconomyPorts.Relations r) { relations = Objects.requireNonNull(r); }
    public void useChronicle(EconomyPorts.Chronicle c) { chronicle = Objects.requireNonNull(c); }
    public void useSeed(long s) { seed = s; dice = new Dice(s); }
    private void publish(NpcEvent e) { bus.publish(e); }
    private long now() { return clock.now(); }
    private int perDay() { return clock.minutesPerDay(); }

    // ------------------------------------------------------------------ settlements, stores, accounts

    /**
     * Gives a settlement an economy: a warehouse, a treasury endowed with coins and some founding stores (both recorded with the
     * provenance "founding"), a temple account if it has a temple, and a market with a merchant house if it has a market.
     */
    public SettlementEconomy registerSettlement(UUID id, UUID region, String name, boolean market, boolean temple) {
        refresh();
        SettlementEconomy existing = settlements.get(id);
        if (existing != null) return existing;
        long now = now();
        Warehouse w = new Warehouse(LivingIds.named("warehouse", id.toString()), Warehouse.OwnerKind.SETTLEMENT, id, id, "almacén", settings.warehouseCapacity(), 0.5D, settings.maxLotsPerResource(), perDay());
        warehouses.put(w.id(), w);
        WealthAccount treasury = wealth.account(WealthAccount.OwnerKind.SETTLEMENT, id, "tesoro de " + name, now);
        Provenance founding = Provenance.of("founding", id.toString(), "fundación de " + name, now);
        wealth.mint(treasury, settings.settlementEndowment(), founding, now, id);
        SettlementEconomy se = new SettlementEconomy(id, region, name, w.id(), treasury.id(), new TaxPolicy(settings.marketTax(), settings.templeTithe(), settings.levyPerCitizen(), 0), now);
        settlements.put(id, se);
        if (temple) se.templeAccount(wealth.account(WealthAccount.OwnerKind.TEMPLE, LivingIds.named("temple-account", id.toString()), "templo de " + name, now).id());
        double pop = settings.foundingPopulation(), days = settings.foundingStoreDays();
        if (pop > 0 && days > 0) {
            foundingStore(se, w, "rice", needs.food() * pop * days, founding);
            foundingStore(se, w, "water", needs.water() * pop * 2, founding);
            foundingStore(se, w, "wood", needs.fuel() * pop * days, founding);
            foundingStore(se, w, "tools", Math.max(2, pop / 3), founding);
        }
        if (market) {
            MarketRuntime m = new MarketRuntime(id);
            se.market(m);
            Merchant house = createMerchant(LivingIds.named("merchant-house", id.toString()), null, "Casa mercantil de " + name, id);
            wealth.mint(wealth.get(house.account()).orElseThrow(), settings.merchantEndowment(), Provenance.of("founding", id.toString(), "casa mercantil", now), now, id);
            m.stalls().add(house.id());
        }
        dirty = true; revision++;
        return se;
    }

    private void foundingStore(SettlementEconomy se, Warehouse w, String resource, double quantity, Provenance founding) {
        deposit(w, ResourceLot.fresh(resource, quantity, 0.6D, founding, se.settlement(), now()), MovementRecord.Kind.MINTED, "reservas fundacionales", se.settlement());
    }

    private Merchant createMerchant(UUID id, UUID npc, String name, UUID home) {
        long now = now();
        WealthAccount acc = wealth.account(npc == null ? WealthAccount.OwnerKind.MERCHANT : WealthAccount.OwnerKind.NPC, npc == null ? id : npc, name, now);
        Warehouse stock = new Warehouse(LivingIds.named("merchant-stock", id.toString()), Warehouse.OwnerKind.MERCHANT, id, home, "puesto", settings.merchantStockCapacity(), 0.4D, settings.maxLotsPerResource(), perDay());
        warehouses.put(stock.id(), stock);
        Merchant.Personality[] kinds = Merchant.Personality.values();
        Merchant m = new Merchant(id, npc, name, home, acc.id(), stock.id(), kinds[dice.below("merchant:" + id, 0, kinds.length)]);
        merchants.put(id, m);
        dirty = true; revision++;
        return m;
    }

    /** A citizen merchant of a settlement: gets a stall, a stock and, if the treasury can afford it, a loan to start trading. */
    public Merchant registerMerchant(UUID npc, String name, UUID settlement) {
        for (Merchant m : merchants.values()) if (npc.equals(m.npc())) return m;
        SettlementEconomy se = settlements.get(settlement);
        if (se == null) throw new IllegalArgumentException("settlement without economy " + settlement);
        Merchant m = createMerchant(LivingIds.named("merchant", npc.toString()), npc, name, settlement);
        WealthAccount treasury = wealth.get(se.treasury()).orElseThrow();
        if (treasury.coins() > settings.merchantEndowment() * 3)
            wealth.transfer(treasury, wealth.get(m.account()).orElseThrow(), settings.merchantEndowment() * 0.5D, MovementRecord.Kind.TRANSFER, "préstamo de la aldea a " + name, now(), settlement);
        if (se.market() != null) se.market().stalls().add(m.id());
        return m;
    }

    public Optional<SettlementEconomy> settlement(UUID id) { return Optional.ofNullable(settlements.get(id)); }
    public Collection<SettlementEconomy> settlements() { return List.copyOf(settlements.values()); }
    public Optional<Warehouse> warehouse(UUID id) { return Optional.ofNullable(warehouses.get(id)); }
    public Warehouse storeOf(UUID settlement) { SettlementEconomy se = settlements.get(settlement); return se == null ? null : warehouses.get(se.warehouse()); }
    public Collection<Warehouse> warehouses() { return List.copyOf(warehouses.values()); }
    public Collection<Merchant> merchants() { return List.copyOf(merchants.values()); }
    public Optional<Merchant> merchant(UUID id) { return Optional.ofNullable(merchants.get(id)); }
    public Collection<Caravan> caravans() { return List.copyOf(caravans.values()); }
    public Optional<Caravan> caravan(UUID id) { return Optional.ofNullable(caravans.get(id)); }
    public Collection<TradeRoute> routes() { return List.copyOf(routes.values()); }
    public Collection<Contract> contracts() { return List.copyOf(contracts.values()); }
    public Optional<Contract> contract(UUID id) { return Optional.ofNullable(contracts.get(id)); }
    public WealthAccount treasury(UUID settlement) { SettlementEconomy se = settlements.get(settlement); return se == null ? null : wealth.get(se.treasury()).orElse(null); }

    public double stock(UUID settlement, String resource) {
        Warehouse w = storeOf(settlement);
        if (w == null) return 0;
        if (FOOD.equals(resource)) return foodUnits(w);
        return w.inventory().amount(resource);
    }

    private double foodUnits(Warehouse w) {
        double units = 0;
        for (ResourceDef d : resources.byCategory(ResourceDef.Category.FOOD)) units += w.inventory().amount(d.id()) * d.food();
        return units;
    }

    private double baseValue(String resource) { return resources.get(resource).map(ResourceDef::baseValue).orElse(1.0D); }
    private double unitWeight(String resource) { return resources.get(resource).map(ResourceDef::weight).orElse(1.0D); }

    /** The current market price of a resource in a settlement (the base value where there is no price yet). */
    public double price(UUID settlement, String resource) {
        SettlementEconomy se = settlements.get(settlement);
        if (se != null && se.market() != null) { PricePoint p = se.market().prices().get(resource); if (p != null) return p.price(); }
        if (se != null) { PricePoint p = localPrices.getOrDefault(settlement, Map.of()).get(resource); if (p != null) return p.price(); }
        return baseValue(resource);
    }

    private final Map<UUID, Map<String, PricePoint>> localPrices = new HashMap<>();

    private Map<String, PricePoint> priceTable(SettlementEconomy se) {
        return se.market() != null ? se.market().prices() : localPrices.computeIfAbsent(se.settlement(), k -> new LinkedHashMap<>());
    }

    // ------------------------------------------------------------------ goods movement (always recorded)

    private double deposit(Warehouse w, ResourceLot lot, MovementRecord.Kind kind, String reference, UUID settlement) {
        if (lot.quantity() <= 0) return 0;
        double free = w.capacity() - w.inventory().weight(this::unitWeight);
        double fits = Math.max(0, free / unitWeight(lot.resource()));
        double kept = Math.min(lot.quantity(), fits);
        if (kept < lot.quantity() - 1e-9) {
            ledger.record(now(), MovementRecord.Kind.DESTROYED, lot.resource(), lot.quantity() - kept, null, w.id(), 0, "sin espacio en " + w.location(), lot.origin(), settlement);
            lot = new ResourceLot(lot.id(), lot.resource(), kept, lot.quality(), lot.durability(), lot.origin(), lot.originSettlement(), lot.producedAt());
        }
        if (kept <= 0) return 0;
        w.inventory().add(lot);
        w.counted(kept, 0);
        ledger.record(now(), kind, lot.resource(), kept, null, w.id(), kept * baseValue(lot.resource()), reference, lot.origin(), settlement);
        return kept;
    }

    private List<ResourceLot> take(Warehouse w, String resource, double quantity, MovementRecord.Kind kind, String reference, UUID to, UUID settlement) {
        if (quantity <= 0) return List.of();
        List<ResourceLot> lots = w.inventory().take(resource, quantity);
        double total = 0;
        for (ResourceLot l : lots) {
            total += l.quantity();
            ledger.record(now(), kind, resource, l.quantity(), w.id(), to, l.quantity() * baseValue(resource), reference, l.origin(), settlement);
        }
        if (total > 0) w.counted(0, total);
        return lots;
    }

    private static double sum(List<ResourceLot> lots) { double t = 0; for (ResourceLot l : lots) t += l.quantity(); return t; }

    private void move(Warehouse from, Warehouse to, String resource, double quantity, MovementRecord.Kind kind, String reference, UUID settlement) {
        for (ResourceLot l : from.inventory().take(resource, quantity)) {
            ledger.record(now(), kind, resource, l.quantity(), from.id(), to.id(), l.quantity() * baseValue(resource), reference, l.origin(), settlement);
            from.counted(0, l.quantity());
            double kept = to.inventory().amount(resource);
            to.inventory().add(l);
            to.counted(to.inventory().amount(resource) - kept, 0);
        }
    }

    // ------------------------------------------------------------------ the simulation step

    /**
     * Advances a settlement's economy over [from, to): production from the work hours accounted since the last step,
     * water from the wells, consumption, spoilage, taxes, balances (scarcity / surplus), prices, statistics.
     */
    public void simulate(UUID id, long from, long to) {
        SettlementEconomy se = settlements.get(id);
        if (se == null || to <= from || world == null || villages == null || calendar == null) return;
        refresh();
        long started = System.nanoTime();
        double days = (to - from) / (double) perDay();
        long day = Math.floorDiv(to - 1, (long) perDay());
        if (day != se.statsDay()) { snapshot(se); se.rollStats(day); }
        Warehouse w = warehouses.get(se.warehouse());
        String scope = world.regionScope(id);
        List<EconomyPorts.Worker> workers = villages.workers(id);
        int farmers = 0;
        for (EconomyPorts.Worker wk : workers) if ("farmer".equals(wk.profession())) farmers++;
        int farms = villages.buildings(id, "FARM");
        Map<String, Double> producedNow = new LinkedHashMap<>();
        int working = 0;
        for (EconomyPorts.Worker wk : workers) {
            Double seen = se.accountedHours().get(wk.npc());
            se.accountedHours().put(wk.npc(), wk.totalHours());
            if (seen == null) continue;                                           // first sight: start counting from here
            double hours = Math.min(wk.totalHours() - seen, settings.maxHoursPerDay() * Math.max(days, 1.0D / 24));
            if (hours <= 1e-6) continue;
            if (produce(se, w, wk, hours, farms, farmers, scope, producedNow)) working++;
        }
        int wells = villages.buildings(id, "WELL");
        if (wells > 0) {
            double got = world.extract(se.region(), "water", wells * settings.wellWaterPerDay() * days);
            if (got > 0) {
                deposit(w, ResourceLot.fresh("water", got, 0.9D, Provenance.of("well", id.toString(), "pozos de " + se.name(), now()), id, now()), MovementRecord.Kind.PRODUCED, "agua de los pozos", id);
                producedNow.merge("water", got, Double::sum);
            }
        }
        producedNow.forEach((k, v) -> { se.produced().merge(k, v, Double::sum); metrics.produced(v); });
        if (!producedNow.isEmpty()) publish(new ResourceProducedEvent(now(), id, producedNow, working));
        consume(se, w, days, workers);
        Map<String, Double> spoiled = w.inventory().spoil(days, r -> resources.get(r).map(ResourceDef::spoilPerDay).orElse(0.0D));
        spoiled.forEach((r, q) -> ledger.record(now(), MovementRecord.Kind.SPOILED, r, q, w.id(), null, q * baseValue(r), "se echó a perder", null, id));
        if (day != lastLevyDay) collectLevies(day);
        balances(se, w, workers);
        if (now() - se.lastPrices() >= settings.priceIntervalMinutes()) { updatePrices(se, w); se.lastPrices(now()); }
        se.prosperity(prosperity(se, w));
        se.lastStep(to);
        se.markDirty();
        w.markDirty();
        dirty = true; revision++;
        metrics.steps.incrementAndGet();
        metrics.stepNanos.addAndGet(System.nanoTime() - started);
    }

    private boolean produce(SettlementEconomy se, Warehouse w, EconomyPorts.Worker wk, double hours, int farms, int farmers, String scope, Map<String, Double> out) {
        Recipe r = recipes.of(wk.profession()).orElse(null);
        if (r == null || r.source() == Recipe.Source.SERVICE) return false;
        UUID id = se.settlement();
        double skill = Skill.multiplier(wk.totalHours());
        double toolFactor = 1.0D;
        if (r.toolWear() > 0) {
            double need = r.toolWear() * hours;
            double got = sum(take(w, "tools", need, MovementRecord.Kind.CONSUMED, "herramientas gastadas por " + wk.name(), null, id));
            toolFactor = settings.noToolsFactor() + (1.0D - settings.noToolsFactor()) * Math.min(1.0D, got / need);
        }
        double weather = r.source() == Recipe.Source.CRAFT ? 1.0D : calendar.weather(scope).outdoorFactor();
        double craft = 1.0D;
        if (r.source() == Recipe.Source.CRAFT && !r.inputs().isEmpty()) {
            for (var in : r.inputs().entrySet()) {
                double need = in.getValue() * hours * toolFactor;
                craft = Math.min(craft, need <= 0 ? 1 : w.inventory().amount(in.getKey()) / need);
            }
            craft = Math.max(0, Math.min(1, craft));
            if (craft <= 1e-6) return false;
            for (var in : r.inputs().entrySet()) take(w, in.getKey(), in.getValue() * hours * toolFactor * craft, MovementRecord.Kind.CONSUMED, "materia prima de " + wk.name() + " (" + r.profession() + ")", null, id);
        }
        double farmFactor = r.source() == Recipe.Source.FARM ? Math.min(1.0D, farms * settings.farmersPerFarm() / (double) Math.max(1, farmers)) : 1.0D;
        double quality = Math.max(0.2D, Math.min(1.0D, 0.35D + 0.5D * (skill - 0.6D) / 0.7D + 0.15D * toolFactor));
        double value = 0;
        long now = now();
        for (var e : r.outputs().entrySet()) {
            String res = e.getKey();
            double amount = e.getValue() * hours * skill * toolFactor * weather * craft * farmFactor;
            switch (r.source()) {
                case DEPOSIT -> amount = world.extract(se.region(), res, amount);
                case WILDLIFE -> amount = world.harvestAnimals(se.region(), res, amount, settings.wildlifeShare());
                case FARM -> amount *= calendar.agricultureFactor(res, scope);
                default -> { }
            }
            if (amount <= 1e-6) continue;
            Provenance origin = Provenance.of("producer", wk.npc().toString(), wk.name() + ", " + r.profession() + " (" + r.source().name().toLowerCase(java.util.Locale.ROOT) + ")", now);
            double kept = deposit(w, ResourceLot.fresh(res, amount, quality, origin, id, now), MovementRecord.Kind.PRODUCED, "producción de " + wk.name(), id);
            out.merge(res, kept, Double::sum);
            value += kept * baseValue(res);
        }
        metrics.productionRuns.incrementAndGet();
        if (value > 0) {
            WealthAccount treasury = wealth.get(se.treasury()).orElseThrow();
            WealthAccount worker = wealth.account(WealthAccount.OwnerKind.NPC, wk.npc(), wk.name(), now);
            wealth.transferUpTo(treasury, worker, value * settings.wageShare(), MovementRecord.Kind.WAGE, "jornal de " + r.profession(), now, id);
            if (se.templeAccount() != null) {
                double tithe = wealth.transferUpTo(treasury, wealth.get(se.templeAccount()).orElseThrow(), value * se.taxes().templeTithe(), MovementRecord.Kind.TAXED, "diezmo del templo", now, id);
                se.taxes().collectedTithe(tithe);
            }
        }
        return value > 0;
    }

    private void consume(SettlementEconomy se, Warehouse w, double days, List<EconomyPorts.Worker> workers) {
        UUID id = se.settlement();
        int pop = villages.population(id), kids = Math.min(pop, villages.children(id)), visitors = villages.visitors(id);
        double people = (pop - kids) + kids * needs.childShare() + visitors;
        double dailyFood = needs.food() * people * calendar.foodFactor();
        se.dailyFoodNeed(dailyFood);
        Map<String, Double> consumed = new LinkedHashMap<>(), missing = new LinkedHashMap<>();
        double foodNeed = dailyFood * days;
        List<String> order = new ArrayList<>(needs.foodPreference());
        for (ResourceDef d : resources.byCategory(ResourceDef.Category.FOOD)) if (!order.contains(d.id())) order.add(d.id());
        for (String f : order) {
            if (foodNeed <= 1e-9) break;
            ResourceDef d = resources.get(f).orElse(null);
            if (d == null || d.food() <= 0) continue;
            double units = sum(take(w, f, foodNeed / d.food(), MovementRecord.Kind.CONSUMED, "alimento de " + se.name(), null, id));
            if (units > 0) { consumed.merge(f, units, Double::sum); foodNeed -= units * d.food(); }
        }
        if (foodNeed > 1e-6) missing.put(FOOD, foodNeed);
        double water = needs.water() * people * days;
        double gotWater = sum(take(w, "water", water, MovementRecord.Kind.CONSUMED, "agua de " + se.name(), null, id));
        if (gotWater > 0) consumed.put("water", gotWater);
        if (water - gotWater > 1e-6) missing.put("water", water - gotWater);
        double fuel = needs.fuel() * people * days * calendar.fuelFactor();
        for (ResourceDef d : resources.byCategory(ResourceDef.Category.FUEL)) {
            if (fuel <= 1e-9 || d.fuel() <= 0) continue;
            double units = sum(take(w, d.id(), fuel / d.fuel(), MovementRecord.Kind.CONSUMED, "leña y carbón de " + se.name(), null, id));
            if (units > 0) { consumed.merge(d.id(), units, Double::sum); fuel -= units * d.fuel(); }
        }
        if (fuel > 1e-6) missing.put("wood", fuel);
        double cloth = needs.cloth() * people * days;
        double gotCloth = sum(take(w, "cloth", cloth, MovementRecord.Kind.CONSUMED, "ropa de " + se.name(), null, id));
        if (gotCloth > 0) consumed.put("cloth", gotCloth);
        consumed.forEach((k, v) -> { se.consumed().merge(k, v, Double::sum); metrics.consumed(v); });
        missing.forEach((k, v) -> se.shortfall().merge(k, v, Double::sum));
        se.foodCoverDays(dailyFood <= 1e-9 ? Double.POSITIVE_INFINITY : foodUnits(w) / dailyFood);
        if (!consumed.isEmpty() || !missing.isEmpty()) publish(new ResourceConsumedEvent(now(), id, consumed, missing, se.foodCoverDays()));
    }

    private void collectLevies(long day) {
        lastLevyDay = day;
        long now = now();
        for (SettlementEconomy se : settlements.values()) {
            if (se.taxes().levyPerCitizen() <= 0) continue;
            WealthAccount treasury = wealth.get(se.treasury()).orElse(null);
            if (treasury == null) continue;
            double levy = 0;
            for (EconomyPorts.Worker wk : villages.workers(se.settlement())) {
                WealthAccount acc = wealth.of(wk.npc()).orElse(null);
                if (acc != null) levy += wealth.transferUpTo(acc, treasury, se.taxes().levyPerCitizen(), MovementRecord.Kind.TAXED, "impuesto diario", now, se.settlement());
            }
            se.taxes().collectedLevy(levy);
            if (levy > 0) publish(new TaxCollectedEvent(now, se.settlement(), 0, 0, levy));
        }
    }

    /** Daily demand of each resource: consumption, tools worn, workshop inputs. */
    private Map<String, Double> demand(SettlementEconomy se, List<EconomyPorts.Worker> workers) {
        Map<String, Double> d = new LinkedHashMap<>();
        UUID id = se.settlement();
        int pop = villages.population(id), kids = Math.min(pop, villages.children(id));
        double people = (pop - kids) + kids * needs.childShare() + villages.visitors(id);
        d.put(FOOD, se.dailyFoodNeed());
        d.put("water", needs.water() * people);
        d.put("wood", needs.fuel() * people * calendar.fuelFactor());
        d.put("cloth", needs.cloth() * people);
        for (EconomyPorts.Worker wk : workers) recipes.of(wk.profession()).ifPresent(r -> {
            if (r.toolWear() > 0) d.merge("tools", r.toolWear() * 8, Double::sum);
            r.inputs().forEach((res, rate) -> d.merge(res, rate * 8, Double::sum));
        });
        Map<String, Double> festival = calendar.festivalDemand(villages.culture(id));
        for (ResourceDef f : resources.byCategory(ResourceDef.Category.FOOD)) d.putIfAbsent(f.id(), se.dailyFoodNeed() / Math.max(0.1D, f.food()) / 3.0D);
        festival.forEach((res, mult) -> d.computeIfPresent(res, (k, v) -> v * mult));
        return d;
    }

    private void balances(SettlementEconomy se, Warehouse w, List<EconomyPorts.Worker> workers) {
        Map<String, Double> demand = demand(se, workers);
        se.dailyDemand().clear();
        se.dailyDemand().putAll(demand);
        for (var e : demand.entrySet()) {
            String res = e.getKey();
            double daily = e.getValue();
            if (daily <= 1e-6) continue;
            if (!FOOD.equals(res) && resources.get(res).map(d -> d.category() == ResourceDef.Category.FOOD).orElse(false)) continue;   // foods are judged together
            double cover = (FOOD.equals(res) ? foodUnits(w) : w.inventory().amount(res)) / daily;
            MarketBalance b = se.balance(res);
            MarketBalance.State before = b.update(cover, settings.scarceDays(), settings.surplusDays(), now());
            if (before == null) continue;
            UUID id = se.settlement();
            if (before == MarketBalance.State.SCARCE) publish(new ScarcityEndedEvent(now(), id, res, cover));
            if (before == MarketBalance.State.SURPLUS) publish(new SurplusEndedEvent(now(), id, res, cover));
            if (b.state() == MarketBalance.State.SCARCE) {
                metrics.shortages.incrementAndGet();
                publish(new ScarcityStartedEvent(now(), id, res, cover));
                memory.happened(new TradeMemory.Happening(now(), id, "SCARCITY", res + String.format(" (%.1f días)", cover)));
                scarcityContract(se, FOOD.equals(res) ? "rice" : res, daily, cover);
            }
            if (b.state() == MarketBalance.State.SURPLUS) { metrics.surpluses.incrementAndGet(); publish(new SurplusStartedEvent(now(), id, res, cover)); }
        }
    }

    private void updatePrices(SettlementEconomy se, Warehouse w) {
        UUID id = se.settlement();
        Map<String, PricePoint> table = priceTable(se);
        double danger = world.regionDanger(se.region());
        Map<String, Double> festival = calendar.festivalDemand(villages.culture(id));
        Set<String> local = new java.util.HashSet<>();
        for (EconomyPorts.Worker wk : villages.workers(id)) recipes.of(wk.profession()).ifPresent(r -> local.addAll(r.outputs().keySet()));
        boolean special = se.specialMarketUntil() > now();
        for (ResourceDef def : resources.all()) {
            if (def.future()) continue;
            String res = def.id();
            double supply = w.inventory().amount(res);
            if (se.market() != null) for (UUID m : se.market().stalls()) { Merchant mm = merchants.get(m); if (mm != null) supply += warehouses.get(mm.stock()).inventory().amount(res); }
            double daily = def.category() == ResourceDef.Category.FOOD ? se.dailyFoodNeed() / Math.max(0.1D, def.food()) / 3.0D : se.dailyDemand().getOrDefault(res, 0.05D);
            MarketBalance bal = def.category() == ResourceDef.Category.FOOD ? se.balance(FOOD) : se.balance(res);
            double demandQty = Math.max(0.05D, daily) * settings.coverTargetDays() * festival.getOrDefault(res, 1.0D) * (special ? 1.2D : 1.0D)
                    * (bal.state() == MarketBalance.State.SCARCE && def.category() == ResourceDef.Category.FOOD ? settings.panicDemand() : 1.0D);
            double importDistance = 0;
            int n = 0;
            for (ResourceLot l : w.inventory().lotsOf(res)) if (l.originSettlement() != null && !l.originSettlement().equals(id)) { importDistance += world.distance(id, l.originSettlement()); n++; }
            boolean imported = !local.contains(res) && res.equals("water") == false;
            PricePoint before = table.get(res);
            PricePoint next = prices.price(res, new PriceEngine.Inputs(def.baseValue(), supply, demandQty, calendar.harvestSeason(res), calendar.offSeason(res), danger,
                    def.category() == ResourceDef.Category.FOOD || def.category() == ResourceDef.Category.TOOL || def.category() == ResourceDef.Category.FUEL,
                    imported, n == 0 ? 0 : importDistance / n, bal.state() == MarketBalance.State.SCARCE, bal.state() == MarketBalance.State.SURPLUS, se.prosperity()), before, now());
            table.put(res, next);
            metrics.priceUpdates.incrementAndGet();
            if (before != null && Math.abs(next.price() - before.price()) / Math.max(0.01D, before.price()) >= settings.priceEventThreshold()) {
                metrics.priceChanges.incrementAndGet();
                String main = next.factors().entrySet().stream().max((a, b) -> Double.compare(Math.abs(Math.log(a.getValue())), Math.abs(Math.log(b.getValue())))).map(Map.Entry::getKey).orElse("");
                publish(new PriceChangedEvent(now(), id, res, before.price(), next.price(), main));
            }
        }
    }

    private double prosperity(SettlementEconomy se, Warehouse w) {
        int pop = Math.max(1, villages.population(se.settlement()));
        double coins = wealth.get(se.treasury()).map(WealthAccount::coins).orElse(0.0D);
        double goods = w.inventory().value(this::baseValue);
        double food = Double.isInfinite(se.foodCoverDays()) ? 1.0D : Math.min(1.0D, se.foodCoverDays() / settings.coverTargetDays());
        return 0.4D * (1.0D - Math.exp(-coins / (pop * 20.0D))) + 0.3D * food + 0.3D * Math.min(1.0D, goods / (pop * 30.0D));
    }

    private void snapshot(SettlementEconomy se) {
        if (se.statsDay() == Long.MIN_VALUE) return;
        Warehouse w = warehouses.get(se.warehouse());
        Map<String, Double> p = new LinkedHashMap<>();
        priceTable(se).forEach((k, v) -> p.put(k, v.price()));
        memory.snapshot(se.settlement(), new TradeMemory.Snapshot(se.statsDay(), p, w.inventory().totals(), se.produced(), se.consumed(), se.imported(), se.exported(), se.shortfall(),
                wealth.get(se.treasury()).map(WealthAccount::coins).orElse(0.0D)));
    }

    /** The profession whose output this settlement lacks most (its scarcest resource's producer). */
    public Optional<String> mostNeededProfession(UUID settlement) {
        SettlementEconomy se = settlements.get(settlement);
        if (se == null) return Optional.empty();
        String worst = null;
        double cover = Double.MAX_VALUE;
        for (MarketBalance b : se.balances().values()) if (b.state() == MarketBalance.State.SCARCE && b.coverDays() < cover) { cover = b.coverDays(); worst = b.resource(); }
        if (worst == null) return Optional.empty();
        return FOOD.equals(worst) ? Optional.of("farmer") : worst.equals("water") ? Optional.empty() : recipes.producerOf(worst);
    }

    // ------------------------------------------------------------------ trade: routes, planning, caravans

    private static String routeKey(UUID a, UUID b) { return a + ">" + b; }

    /** The trade route between two settlements, re-planned when the road network changed since it was planned. */
    public Optional<TradeRoute> route(UUID a, UUID b) {
        if (world == null || a.equals(b)) return Optional.empty();
        String key = routeKey(a, b);
        TradeRoute r = routes.get(key);
        long version = world.roadVersion();
        if (r != null && r.version() == version) return Optional.of(r);
        if (r == null) { r = new TradeRoute(LivingIds.named("trade-route", key), a, b); routes.put(key, r); }
        TradeRoute.Status before = r.legs().isEmpty() && r.version() == 0 ? null : r.status();
        Optional<EconomyPorts.RouteInfo> info = world.route(a, b);
        List<TradeRoute.Leg> legs = new ArrayList<>();
        info.ifPresent(i -> i.legs().forEach(l -> legs.add(new TradeRoute.Leg(l.edge(), l.length(), l.danger()))));
        r.plan(legs, info.map(EconomyPorts.RouteInfo::bridges).orElse(0), version);
        if (before != null && before != r.status()) {
            if (r.status() == TradeRoute.Status.BLOCKED) publish(new TradeRouteDisruptedEvent(now(), r.id(), a, b, "sin camino transitable"));
            else if (before == TradeRoute.Status.BLOCKED) publish(new TradeRouteRestoredEvent(now(), r.id(), a, b));
        }
        dirty = true; revision++;
        return Optional.of(r);
    }

    /** A road was blocked or reopened: the routes through it are re-planned (event-driven, never polled). */
    public void roadChanged(UUID edge, boolean blocked) {
        for (TradeRoute r : List.copyOf(routes.values())) {
            if (!r.uses(edge) && blocked) continue;
            TradeRoute.Status before = r.status();
            route(r.origin(), r.destination());
            if (blocked && r.uses(edge) && before != TradeRoute.Status.BLOCKED) {
                publish(new TradeRouteDisruptedEvent(now(), r.id(), r.origin(), r.destination(), "camino bloqueado"));
                memory.happened(new TradeMemory.Happening(now(), r.origin(), "BLOCKED_ROUTE", "ruta hacia " + r.destination()));
                publish(new EconomicEventEvent(now(), r.origin(), "BLOCKED_ROUTE", "ruta comercial cortada"));
            }
        }
    }

    /**
     * Daily trade planning: each free merchant looks for goods its home has in surplus that another settlement lacks or pays
     * better for, over a usable route, and forms a caravan when the expected profit (after transport cost and risk) is worth
     * it. The merchant pays for the goods: nothing leaves a warehouse without money coming in.
     */
    public int planTrade() {
        if (world == null || villages == null || calendar == null) return 0;
        long started = System.nanoTime();
        long now = now();
        int created = 0;
        long active = caravans.values().stream().filter(Caravan::active).count();
        for (Merchant m : List.copyOf(merchants.values())) {
            if (active + created >= settings.maxCaravans()) break;
            if (!m.available(now)) continue;
            SettlementEconomy home = settlements.get(m.home());
            if (home == null) continue;
            WealthAccount purse = wealth.get(m.account()).orElse(null);
            if (purse == null || purse.coins() < 5) continue;
            Warehouse w = warehouses.get(home.warehouse());
            record Plan(SettlementEconomy to, String resource, double quantity, double priceA, double profit, TradeRoute route) { }
            Plan best = null;
            for (var stock : w.inventory().totals().entrySet()) {
                String res = stock.getKey();
                ResourceDef def = resources.get(res).orElse(null);
                if (def == null || def.category() == ResourceDef.Category.WATER) continue;
                double daily = home.dailyDemand().getOrDefault(def.category() == ResourceDef.Category.FOOD ? FOOD : res, 0.0D);
                if (def.category() == ResourceDef.Category.FOOD) daily = home.dailyFoodNeed() / Math.max(0.1D, def.food());
                double surplus = stock.getValue() - daily * settings.coverTargetDays();
                if (surplus < 5) continue;
                double priceA = price(home.settlement(), res);
                for (SettlementEconomy to : settlements.values()) {
                    if (to == home || world.distance(home.settlement(), to.settlement()) > settings.maxTradeDistance()) continue;
                    MarketBalance bal = def.category() == ResourceDef.Category.FOOD ? to.balance(FOOD) : to.balance(res);
                    double priceB = price(to.settlement(), res);
                    if (priceB < priceA * 1.15D && bal.state() != MarketBalance.State.SCARCE) continue;
                    Optional<TradeRoute> route = route(home.settlement(), to.settlement());
                    if (route.isEmpty() || !route.get().usable()) continue;
                    // the season decides how much trade there is: smaller loads in winter, larger in summer
                    double capacity = settings.cartCapacity() * settings.maxCarts() / def.weight() * calendar.tradeFactor();
                    double qty = Math.min(Math.min(surplus, capacity), purse.coins() / Math.max(0.01D, priceA));
                    if (qty < 1) continue;
                    double revenue = qty * priceB * settings.merchantSaleShare();
                    double transport = route.get().distance() * settings.transportCostPerBlock() * qty * def.weight();
                    double risk = route.get().danger() * revenue * 0.5D;
                    double profit = revenue - qty * priceA - transport - risk;
                    if (profit >= settings.minProfit() && (best == null || profit > best.profit())) best = new Plan(to, res, qty, priceA, profit, route.get());
                }
            }
            if (best != null && createCaravan(m, home, best.to(), best.resource(), best.quantity(), best.priceA(), best.route()) != null) created++;
        }
        metrics.tradePlans.incrementAndGet();
        metrics.planNanos.addAndGet(System.nanoTime() - started);
        return created;
    }

    public Caravan createCaravan(Merchant m, SettlementEconomy from, SettlementEconomy to, String resource, double quantity, double unitPrice, TradeRoute route) {
        long now = now();
        WealthAccount purse = wealth.get(m.account()).orElseThrow();
        WealthAccount treasury = wealth.get(from.treasury()).orElseThrow();
        double cost = quantity * unitPrice;
        if (!wealth.transfer(purse, treasury, cost, MovementRecord.Kind.SOLD, resource + " para caravana", now, from.settlement())) return null;
        ResourceDef def = resources.require(resource);
        UUID id = UUID.randomUUID();
        int carts = (int) Math.max(1, Math.min(settings.maxCarts(), Math.ceil(quantity * def.weight() / settings.cartCapacity())));
        Warehouse cargo = new Warehouse(LivingIds.named("cargo", id.toString()), Warehouse.OwnerKind.CARAVAN, id, from.settlement(), "carros", settings.cartCapacity() * carts, 0.3D, settings.maxLotsPerResource(), perDay());
        warehouses.put(cargo.id(), cargo);
        move(warehouses.get(from.warehouse()), cargo, resource, quantity, MovementRecord.Kind.TRANSPORTED, "caravana " + LivingIds.shortId(id), from.settlement());
        double loaded = cargo.inventory().amount(resource);
        from.exported().merge(resource, loaded, Double::sum);
        Caravan.Type type = switch (def.category()) { case FOOD -> Caravan.Type.AGRICULTURAL; case MATERIAL, TOOL, FUEL -> Caravan.Type.METALLURGICAL; case MEDICINE -> Caravan.Type.RELIGIOUS; default -> Caravan.Type.COMMERCIAL; };
        Caravan c = new Caravan(id, type, route.id(), from.settlement(), to.settlement(), m.id(), cargo.id(), settings.caravanGuards(), carts, cargo.capacity(), settings.caravanSpeed(), now, now + 60);
        c.purchaseCost(cost);
        c.state(Caravan.State.LOADING);
        c.note("carga " + String.format("%.0f %s", loaded, resource));
        caravans.put(id, c);
        m.caravan(id);
        route.used(now);
        metrics.caravansCreated.incrementAndGet();
        dirty = true; revision++;
        publish(new CaravanCreatedEvent(now, id, from.settlement(), to.settlement(), type.name(), Map.of(resource, loaded), cost));
        return c;
    }

    /** Moves every active caravan forward by the time that passed since the last call. */
    public void advanceCaravans() {
        long now = now();
        if (lastCaravanMinute == Long.MIN_VALUE) { lastCaravanMinute = now; return; }
        long dt = now - lastCaravanMinute;
        lastCaravanMinute = now;
        if (dt <= 0 || world == null) return;
        long started = System.nanoTime();
        for (Caravan c : List.copyOf(caravans.values())) if (c.active()) advance(c, now, dt);
        caravans.values().removeIf(c -> !c.active() && now - Math.max(c.arrivedAt(), c.createdAt()) > 7L * perDay());
        metrics.caravanNanos.addAndGet(System.nanoTime() - started);
    }

    private TradeRoute routeById(UUID id) { for (TradeRoute r : routes.values()) if (r.id().equals(id)) return r; return null; }

    private void advance(Caravan c, long now, long dt) {
        TradeRoute route = routeById(c.route());
        if (route == null) { c.state(Caravan.State.CANCELLED); return; }
        double factor = calendar.travelFactor() * calendar.weather(world.regionScope(c.origin())).outdoorFactor();
        double move = c.speed() * dt * factor;
        switch (c.state()) {
            case LOADING -> { if (now >= c.departAt()) { c.state(Caravan.State.TRAVELLING); c.note("sale"); publish(new CaravanDepartedEvent(now, c.id(), c.origin(), c.destination(), route.distance())); } }
            case DELAYED -> {
                TradeRoute.Leg leg = c.legIndex() < route.legs().size() ? route.legs().get(c.legIndex()) : null;
                if (leg == null || !world.blocked(leg.edge())) { c.state(Caravan.State.TRAVELLING); c.note("el camino se abre"); }
                else if (now - c.delayedSince() > settings.maxDelayMinutes()) { c.state(Caravan.State.RETURNING); c.returnProgress(route.distance() - c.progress()); c.note("vuelve: camino cortado"); }
            }
            case TRAVELLING -> travel(c, route, move, now);
            case RETURNING -> {
                c.returnProgress(c.returnProgress() + move);
                if (c.returnProgress() >= route.distance()) complete(c, now);
            }
            default -> { }
        }
    }

    private void travel(Caravan c, TradeRoute route, double move, long now) {
        List<TradeRoute.Leg> legs = route.legs();
        while (move > 1e-9 && c.legIndex() < legs.size()) {
            TradeRoute.Leg leg = legs.get(c.legIndex());
            if (world.blocked(leg.edge())) { c.state(Caravan.State.DELAYED); c.delayedSince(now); c.note("espera: camino bloqueado"); publish(new CaravanDelayedEvent(now, c.id(), leg.edge(), "camino bloqueado")); return; }
            double legStart = 0;
            for (int i = 0; i < c.legIndex(); i++) legStart += legs.get(i).length();
            double left = legStart + leg.length() - c.progress();
            double step = Math.min(move, left);
            c.progress(c.progress() + step);
            move -= step;
            if (step >= left - 1e-9) {
                world.travelled(leg.edge(), now);
                double danger = world.edgeDanger(leg.edge());
                double p = Math.min(0.95D, danger * settings.ambushScale() * leg.length() / 1000.0D * (1.0D - c.defence()));
                if (dice.chance("ambush:" + c.id(), c.legIndex(), p)) { ambush(c, route, leg, now); if (!c.active()) return; }
                c.legIndex(c.legIndex() + 1);
            }
        }
        if (c.legIndex() >= legs.size()) arrive(c, route, now);
    }

    private void ambush(Caravan c, TradeRoute route, TradeRoute.Leg leg, long now) {
        c.ambushed();
        metrics.ambushes.incrementAndGet();
        double loss = Math.min(1.0D, dice.between("ambush-loss:" + c.id(), c.legIndex(), 0.3D, 1.0D) * (1.0D - c.defence() * 0.8D));
        Warehouse cargo = warehouses.get(c.cargo());
        Map<String, Double> lost = cargo.inventory().destroy(loss);
        lost.forEach((r, q) -> ledger.record(now, MovementRecord.Kind.LOOTED, r, q, cargo.id(), null, q * baseValue(r), "emboscada", null, c.origin()));
        boolean destroyed = loss >= 0.9D || cargo.inventory().isEmpty();
        publish(new CaravanAmbushedEvent(now, c.id(), leg.edge(), loss, destroyed));
        c.note(String.format("emboscada: pierde %.0f%%", loss * 100));
        if (destroyed) {
            c.state(Caravan.State.LOST);
            c.arrivedAt(now);
            route.failed();
            metrics.caravansLost.incrementAndGet();
            Merchant m = merchants.get(c.merchant());
            if (m != null) { m.caravan(null); m.availableAt(now + 2L * perDay()); m.reputation(m.reputation() - 5); }
            publish(new CaravanLostEvent(now, c.id(), c.origin(), c.destination(), "emboscada"));
            publish(new EconomicEventEvent(now, c.destination(), "LOST_CARAVAN", "la caravana de " + (m == null ? "?" : m.name()) + " se perdió"));
            memory.happened(new TradeMemory.Happening(now, c.origin(), "LOST_CARAVAN", "hacia " + c.destination()));
            chronicle.record(now, "TRADE", "Caravana perdida", "emboscada en el camino", Set.of("settlement:" + c.origin(), "settlement:" + c.destination()), 0.5D,
                    Provenance.of("caravan", c.id().toString(), "emboscada", now));
        }
    }

    private void arrive(Caravan c, TradeRoute route, long now) {
        SettlementEconomy to = settlements.get(c.destination());
        Merchant m = merchants.get(c.merchant());
        Warehouse cargo = warehouses.get(c.cargo());
        Map<String, Double> delivered = new LinkedHashMap<>();
        double income = 0;
        if (to != null && m != null) {
            WealthAccount treasury = wealth.get(to.treasury()).orElseThrow();
            WealthAccount purse = wealth.get(m.account()).orElseThrow();
            for (var e : cargo.inventory().totals().entrySet()) {
                String res = e.getKey();
                double unit = price(to.settlement(), res) * settings.merchantSaleShare();
                double qty = Math.min(e.getValue(), treasury.coins() / Math.max(0.01D, unit));
                if (qty < 1e-6) continue;
                if (!wealth.transfer(treasury, purse, qty * unit, MovementRecord.Kind.BOUGHT, res + " de caravana", now, to.settlement())) continue;
                move(cargo, warehouses.get(to.warehouse()), res, qty, MovementRecord.Kind.DELIVERED, "caravana " + LivingIds.shortId(c.id()), to.settlement());
                double tax = wealth.transferUpTo(purse, treasury, qty * unit * to.taxes().marketTax(), MovementRecord.Kind.TAXED, "impuesto de mercado", now, to.settlement());
                to.taxes().collectedMarket(tax);
                to.imported().merge(res, qty, Double::sum);
                delivered.put(res, qty);
                income += qty * unit - tax;
                if (to.market() != null) to.market().sold(new MarketRuntime.Sale(now, res, qty, unit, m.id(), to.settlement()), clock.dayIndex(now));
                metrics.trades.incrementAndGet();
                publish(new TradeCompletedEvent(now, to.settlement(), m.id(), to.settlement(), res, qty, unit));
            }
        }
        c.saleValue(income);
        c.arrivedAt(now);
        c.state(Caravan.State.ARRIVED);
        route.completed(income);
        metrics.caravansArrived.incrementAndGet();
        publish(new CaravanArrivedEvent(now, c.id(), c.destination(), delivered, income));
        c.note(String.format("llega y vende por %.0f", income));
        c.state(Caravan.State.RETURNING);
        c.returnProgress(0);
    }

    private void complete(Caravan c, long now) {
        Merchant m = merchants.get(c.merchant());
        Warehouse cargo = warehouses.get(c.cargo());
        if (m != null) {
            Warehouse stock = warehouses.get(m.stock());
            for (String res : List.copyOf(cargo.inventory().totals().keySet())) move(cargo, stock, res, cargo.inventory().amount(res), MovementRecord.Kind.TRANSPORTED, "vuelve con la caravana", c.origin());
            double profit = c.saleValue() - c.purchaseCost();
            m.traded(profit);
            m.reputation(m.reputation() + (profit > 0 ? 1 : -1));
            m.caravan(null);
            m.availableAt(now + 120);
            publish(new CaravanCompletedEvent(now, c.id(), m.id(), profit));
        }
        warehouses.remove(c.cargo());
        c.state(Caravan.State.COMPLETED);
        c.note("de vuelta en casa");
    }

    // ------------------------------------------------------------------ contracts

    /**
     * A settlement short of something asks for it: a delivery contract for what it lacks to reach {@code coverTargetDays},
     * at 1.2 times its price, lasting {@code contractDays}, only as much as its treasury could pay now and only if it has no
     * open contract for that resource already. Payment happens at delivery and never exceeds what the treasury then holds.
     */
    private void scarcityContract(SettlementEconomy se, String resource, double daily, double cover) {
        if (resources.get(resource).isEmpty()) return;
        for (Contract c : contracts.values()) if (c.open() && c.settlement().equals(se.settlement()) && c.resource().equals(resource)) return;
        double want = Math.ceil(Math.max(0, settings.coverTargetDays() - cover) * daily);
        double unit = price(se.settlement(), resource) * 1.2D;
        WealthAccount treasury = wealth.get(se.treasury()).orElse(null);
        if (treasury == null || want < 1 || unit <= 0) return;
        double qty = Math.min(want, Math.floor(treasury.coins() / unit));
        if (qty < 1) return;
        offerContract(Contract.Kind.DELIVERY, se.settlement(), resource, qty, unit, settings.contractDays(), 0, "escasez de " + resources.get(resource).map(ResourceDef::name).orElse(resource).toLowerCase(java.util.Locale.ROOT));
    }

    public Contract offerContract(Contract.Kind kind, UUID settlement, String resource, double quantity, double unitPrice, int days, double trustRequired, String reason) {
        SettlementEconomy se = settlements.get(settlement);
        if (se == null) throw new IllegalArgumentException("settlement without economy");
        long now = now();
        Contract c = new Contract(UUID.randomUUID(), kind, se.treasury(), settlement, resource, quantity, unitPrice, now, now + (long) days * perDay(), trustRequired, reason);
        contracts.put(c.id(), c);
        metrics.contracts.incrementAndGet();
        dirty = true; revision++;
        publish(new ContractCreatedEvent(now, c.id(), kind.name(), settlement, resource, quantity, unitPrice, c.deadline(), reason));
        return c;
    }

    /** Goods delivered against a contract by someone (a player, a merchant): stored in the settlement, paid from its treasury. */
    public double deliverToContract(UUID contractId, UUID deliverer, String delivererName, double quantity, double quality, Provenance origin) {
        Contract c = contracts.get(contractId);
        if (c == null || !c.open() || quantity <= 0) return 0;
        SettlementEconomy se = settlements.get(c.settlement());
        double qty = c.deliver(quantity);
        long now = now();
        deposit(warehouses.get(se.warehouse()), ResourceLot.fresh(c.resource(), qty, quality, origin, null, now), MovementRecord.Kind.DELIVERED, "contrato " + LivingIds.shortId(c.id()), se.settlement());
        WealthAccount payee = wealth.account(WealthAccount.OwnerKind.PLAYER, deliverer, delivererName, now);
        wealth.transferUpTo(wealth.get(se.treasury()).orElseThrow(), payee, qty * c.unitPrice(), MovementRecord.Kind.BOUGHT, "pago del contrato", now, se.settlement());
        if (c.remaining() <= 1e-6) { c.resolve(Contract.State.FULFILLED, now); metrics.contractsFulfilled.incrementAndGet(); publish(new ContractResolvedEvent(now, c.id(), c.state().name(), c.delivered())); }
        dirty = true; revision++;
        return qty;
    }

    public boolean resolveContract(UUID id, Contract.State state) {
        Contract c = contracts.get(id);
        if (c == null || !c.open()) return false;
        c.resolve(state, now());
        dirty = true; revision++;
        publish(new ContractResolvedEvent(now(), id, state.name(), c.delivered()));
        return true;
    }

    private void expireContracts() {
        long now = now();
        for (Contract c : contracts.values()) if (c.open() && now > c.deadline()) { c.resolve(c.delivered() > 0 ? Contract.State.FAILED : Contract.State.EXPIRED, now); publish(new ContractResolvedEvent(now, c.id(), c.state().name(), c.delivered())); dirty = true; revision++; }
        contracts.values().removeIf(c -> !c.open() && now - c.resolvedAt() > 30L * perDay());
    }

    // ------------------------------------------------------------------ world happenings that hit the economy

    /** A fire in a settlement burns part of its stores. */
    public Map<String, Double> fire(UUID settlement, double severity, String cause) { return loss(settlement, 0.35D * Math.max(0, Math.min(1, severity)), "FIRE", cause); }

    /** Raiders or an army take part of a settlement's stores. */
    public Map<String, Double> loot(UUID settlement, double fraction, String cause) { return loss(settlement, fraction, "LOOTED", cause); }

    private Map<String, Double> loss(UUID settlement, double fraction, String kind, String cause) {
        SettlementEconomy se = settlements.get(settlement);
        if (se == null || fraction <= 0) return Map.of();
        Warehouse w = warehouses.get(se.warehouse());
        double effective = fraction * (1.0D - 0.5D * w.security());
        Map<String, Double> lost = w.inventory().destroy(effective);
        lost.forEach((r, q) -> ledger.record(now(), kind.equals("FIRE") ? MovementRecord.Kind.DESTROYED : MovementRecord.Kind.LOOTED, r, q, w.id(), null, q * baseValue(r), cause, null, settlement));
        publish(new WarehouseLossEvent(now(), settlement, cause, lost));
        publish(new EconomicEventEvent(now(), settlement, kind.equals("FIRE") ? "FIRE" : "LOOTING", cause));
        memory.happened(new TradeMemory.Happening(now(), settlement, kind, cause));
        w.markDirty();
        return lost;
    }

    /** A harvest verdict for a region (from the agricultural calendar), announced to the region's settlements. */
    public void harvest(List<UUID> settlementsInRegion, String crop, String verdict, double yieldFactor) {
        for (UUID s : settlementsInRegion) {
            if (!settlements.containsKey(s) || "NORMAL".equals(verdict)) continue;
            publish(new EconomicEventEvent(now(), s, verdict.equals("GREAT") ? "GREAT_HARVEST" : "BAD_HARVEST", String.format("%s x%.2f", crop, yieldFactor)));
            memory.happened(new TradeMemory.Happening(now(), s, verdict + "_HARVEST", crop));
        }
    }

    public void specialMarket(UUID settlement, long until) {
        SettlementEconomy se = settlements.get(settlement);
        if (se == null) return;
        se.specialMarketUntil(until);
        publish(new EconomicEventEvent(now(), settlement, "SPECIAL_MARKET", "gran mercado"));
    }

    // ------------------------------------------------------------------ players and quests

    /**
     * How much the settlement's merchants trust a player, 0..1 (the average of their relationships with the player, 0.5 when
     * the settlement has no embodied merchant). It moves the prices they pay and ask by up to 5% either way.
     */
    public double playerTrust(UUID settlement, UUID player) {
        double sum = 0;
        int n = 0;
        for (Merchant m : merchants.values()) if (settlement.equals(m.home()) && m.npc() != null) { sum += relations.trust01(m.npc(), player); n++; }
        return n == 0 ? 0.5D : Math.max(0, Math.min(1, sum / n));
    }

    /** What the settlement pays a player per unit: 90% of the price at neutral trust, 85%..95% with trust. */
    public double playerSellPrice(UUID settlement, String resource, UUID player) { return price(settlement, resource) * (0.85D + 0.1D * playerTrust(settlement, player)); }

    /** What a player pays the settlement per unit: 110% of the price at neutral trust, 115%..105% with trust. */
    public double playerBuyPrice(UUID settlement, String resource, UUID player) { return price(settlement, resource) * (1.15D - 0.1D * playerTrust(settlement, player)); }

    /** A player sells goods from outside the simulation: they enter with the player as origin; the treasury pays what it can. Returns coins paid. */
    public double sellFromPlayer(UUID settlement, UUID player, String playerName, String resource, double quantity, double quality) {
        SettlementEconomy se = settlements.get(settlement);
        if (se == null || quantity <= 0 || resources.get(resource).isEmpty()) return 0;
        long now = now();
        double unit = playerSellPrice(settlement, resource, player);
        WealthAccount treasury = wealth.get(se.treasury()).orElseThrow();
        double qty = Math.min(quantity, treasury.coins() / Math.max(0.01D, unit));
        if (qty <= 1e-6) return 0;
        deposit(warehouses.get(se.warehouse()), ResourceLot.fresh(resource, qty, quality, Provenance.of("player", player.toString(), playerName, now), null, now), MovementRecord.Kind.SOLD, "venta de " + playerName, settlement);
        WealthAccount acc = wealth.account(WealthAccount.OwnerKind.PLAYER, player, playerName, now);
        wealth.transfer(treasury, acc, qty * unit, MovementRecord.Kind.BOUGHT, resource + " de " + playerName, now, settlement);
        metrics.trades.incrementAndGet();
        publish(new TradeCompletedEvent(now, settlement, player, settlement, resource, qty, unit));
        return qty * unit;
    }

    /** A player buys from the settlement's stores (never more than there is). Returns the quantity bought. */
    public double buyForPlayer(UUID settlement, UUID player, String playerName, String resource, double quantity) {
        SettlementEconomy se = settlements.get(settlement);
        if (se == null || quantity <= 0) return 0;
        long now = now();
        double unit = playerBuyPrice(settlement, resource, player);
        WealthAccount acc = wealth.account(WealthAccount.OwnerKind.PLAYER, player, playerName, now);
        Warehouse w = warehouses.get(se.warehouse());
        double qty = Math.min(Math.min(quantity, w.inventory().amount(resource)), acc.coins() / Math.max(0.01D, unit));
        if (qty <= 1e-6) return 0;
        if (!wealth.transfer(acc, wealth.get(se.treasury()).orElseThrow(), qty * unit, MovementRecord.Kind.SOLD, resource + " a " + playerName, now, settlement)) return 0;
        take(w, resource, qty, MovementRecord.Kind.SOLD, "compra de " + playerName, null, settlement);
        metrics.trades.incrementAndGet();
        publish(new TradeCompletedEvent(now, settlement, settlement, player, resource, qty, unit));
        return qty;
    }

    /** Goods handed to a settlement (a quest turn-in, a gift): stored with their origin, nothing is paid. */
    public double donate(UUID settlement, String resource, double quantity, double quality, Provenance origin) {
        SettlementEconomy se = settlements.get(settlement);
        if (se == null || quantity <= 0) return 0;
        return deposit(warehouses.get(se.warehouse()), ResourceLot.fresh(resource, quantity, quality, origin, null, now()), MovementRecord.Kind.DONATED, origin.label(), settlement);
    }

    /** Pays a reward out of a settlement treasury (never more than it has). Returns what was paid. */
    public double reward(UUID settlement, UUID recipient, String recipientName, double coins, String reason) {
        WealthAccount treasury = treasury(settlement);
        if (treasury == null || coins <= 0) return 0;
        WealthAccount acc = wealth.account(WealthAccount.OwnerKind.PLAYER, recipient, recipientName, now());
        return wealth.transferUpTo(treasury, acc, coins, MovementRecord.Kind.REWARD, reason, now(), settlement);
    }

    /** Takes goods out of a settlement's stores for a reward (returns the lots, with origin). */
    public List<ResourceLot> withdraw(UUID settlement, String resource, double quantity, String reason) {
        Warehouse w = storeOf(settlement);
        return w == null ? List.of() : take(w, resource, quantity, MovementRecord.Kind.REWARD, reason, null, settlement);
    }

    // ------------------------------------------------------------------ the tick

    /** Caravans advance on their cadence; contracts expire. Settlements are advanced by the world simulation, not here. */
    public void tick() {
        refresh();
        tickCount++;
        if (tickCount % settings.caravanIntervalTicks() == 0) { advanceCaravans(); expireContracts(); }
    }

    /** Market life from the villages: whether each market is open and how busy. */
    public void syncMarkets() {
        if (villages == null) return;
        for (SettlementEconomy se : settlements.values()) if (se.market() != null) { se.market().open(villages.marketOpen(se.settlement())); se.market().footfall(villages.footfall(se.settlement())); }
    }

    // ------------------------------------------------------------------ accessors

    public EconomySettings settings() { return settings; }
    public ResourceCatalog resources() { return resources; }
    public RecipeCatalog recipes() { return recipes; }
    public NeedProfile needs() { return needs; }
    public EconomyLedger ledger() { return ledger; }
    public WealthEngine wealth() { return wealth; }
    public TradeMemory memory() { return memory; }
    public EconomyMetrics metrics() { return metrics; }
    public WorldClock clock() { return clock; }
    public Map<String, PricePoint> prices(UUID settlement) { SettlementEconomy se = settlements.get(settlement); return se == null ? Map.of() : Map.copyOf(priceTable(se)); }
    public long seed() { return seed; }
    public List<String> problems() { List<String> p = new ArrayList<>(resources.problems()); p.addAll(recipes.problems()); return p; }
    public boolean dirty() { return dirty || ledger.dirty() || wealth.dirty() || memory.dirty(); }
    /** Grows with every change (movements, steps, caravans, contracts): each persistent section saves when it moved since its own last save. */
    public long revision() { return revision + ledger.recorded() + metrics.steps.get() + metrics.caravansCreated.get(); }
    public void clean() { dirty = false; ledger.clean(); wealth.clean(); memory.clean(); settlements.values().forEach(SettlementEconomy::clean); warehouses.values().forEach(Warehouse::clean); }

    // restore hooks (persistence)
    public void restoreSettlement(SettlementEconomy se) { settlements.put(se.settlement(), se); }
    public void restoreWarehouse(Warehouse w) { warehouses.put(w.id(), w); }
    public void restoreMerchant(Merchant m) { merchants.put(m.id(), m); }
    public void restoreCaravan(Caravan c) { caravans.put(c.id(), c); }
    public void restoreRoute(TradeRoute r) { routes.put(routeKey(r.origin(), r.destination()), r); }
    public void restoreContract(Contract c) { contracts.put(c.id(), c); }
    public void restorePrice(UUID settlement, PricePoint p) { SettlementEconomy se = settlements.get(settlement); if (se != null) priceTable(se).put(p.resource(), p); }
    public long lastCaravanMinute() { return lastCaravanMinute; }
    public void restoreLastCaravanMinute(long v) { lastCaravanMinute = v; }

    public void reset() {
        settlements.clear(); warehouses.clear(); merchants.clear(); caravans.clear(); routes.clear(); contracts.clear(); localPrices.clear();
        ledger.clear(); wealth.clear(); memory.clear(); metrics.reset(); tickCount = 0; lastCaravanMinute = Long.MIN_VALUE; lastLevyDay = Long.MIN_VALUE; dirty = false;
    }
}
