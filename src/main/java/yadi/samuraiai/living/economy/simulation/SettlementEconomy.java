package yadi.samuraiai.living.economy.simulation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.living.economy.markets.MarketRuntime;
import yadi.samuraiai.living.economy.scarcity.MarketBalance;
import yadi.samuraiai.living.economy.taxation.TaxPolicy;

/**
 * The economy of one settlement: its main warehouse, its treasury and (if any) temple account, its market, its taxes, the
 * balance (scarce / normal / surplus) of each resource, today's production, consumption, imports, exports and shortfalls,
 * food cover, prosperity, and the work hours already accounted for each worker (so every hour is produced exactly once,
 * whether the scheduler reported it or the abstract simulation planned it).
 */
public final class SettlementEconomy {
    private final UUID settlement, warehouse, treasury;
    private UUID templeAccount;
    private final UUID region;
    private String name;
    private MarketRuntime market;
    private final TaxPolicy taxes;
    private final Map<String, MarketBalance> balances = new LinkedHashMap<>();
    private final Map<UUID, Double> accountedHours = new HashMap<>();
    private Map<String, Double> produced = new LinkedHashMap<>(), consumed = new LinkedHashMap<>(), imported = new LinkedHashMap<>(), exported = new LinkedHashMap<>(), shortfall = new LinkedHashMap<>();
    private final Map<String, Double> dailyDemand = new LinkedHashMap<>();
    private long statsDay = Long.MIN_VALUE, lastStep, lastPrices = Long.MIN_VALUE / 2, specialMarketUntil;
    private double foodCoverDays = Double.POSITIVE_INFINITY, prosperity = 0.5D, dailyFoodNeed;
    private boolean dirty = true;

    public SettlementEconomy(UUID settlement, UUID region, String name, UUID warehouse, UUID treasury, TaxPolicy taxes, long now) {
        this.settlement = settlement; this.region = region; this.name = name; this.warehouse = warehouse; this.treasury = treasury; this.taxes = taxes; this.lastStep = now;
    }

    public UUID settlement() { return settlement; }
    public UUID region() { return region; }
    public String name() { return name; }
    public void name(String v) { name = v; }
    public UUID warehouse() { return warehouse; }
    public UUID treasury() { return treasury; }
    public UUID templeAccount() { return templeAccount; }
    public void templeAccount(UUID v) { templeAccount = v; }
    public MarketRuntime market() { return market; }
    public void market(MarketRuntime m) { market = m; }
    public TaxPolicy taxes() { return taxes; }
    public Map<String, MarketBalance> balances() { return balances; }
    public MarketBalance balance(String resource) { return balances.computeIfAbsent(resource, MarketBalance::new); }
    public Map<UUID, Double> accountedHours() { return accountedHours; }
    public Map<String, Double> dailyDemand() { return dailyDemand; }

    /** Today's statistics, reset when the day changes (the previous day's go to trade memory first). */
    public boolean rollStats(long day) {
        if (day == statsDay) return false;
        boolean had = statsDay != Long.MIN_VALUE;
        statsDay = day;
        produced = new LinkedHashMap<>(); consumed = new LinkedHashMap<>(); imported = new LinkedHashMap<>(); exported = new LinkedHashMap<>(); shortfall = new LinkedHashMap<>();
        return had;
    }
    public long statsDay() { return statsDay; }
    public Map<String, Double> produced() { return produced; }
    public Map<String, Double> consumed() { return consumed; }
    public Map<String, Double> imported() { return imported; }
    public Map<String, Double> exported() { return exported; }
    public Map<String, Double> shortfall() { return shortfall; }

    public long lastStep() { return lastStep; }
    public void lastStep(long v) { lastStep = v; }
    public long lastPrices() { return lastPrices; }
    public void lastPrices(long v) { lastPrices = v; }
    public long specialMarketUntil() { return specialMarketUntil; }
    public void specialMarketUntil(long v) { specialMarketUntil = v; }
    public double foodCoverDays() { return foodCoverDays; }
    public void foodCoverDays(double v) { foodCoverDays = v; }
    public double dailyFoodNeed() { return dailyFoodNeed; }
    public void dailyFoodNeed(double v) { dailyFoodNeed = v; }
    public double prosperity() { return prosperity; }
    public void prosperity(double v) { prosperity = Math.max(0, Math.min(1, v)); }
    public boolean dirty() { return dirty; }
    public void markDirty() { dirty = true; }
    public void clean() { dirty = false; }
}
