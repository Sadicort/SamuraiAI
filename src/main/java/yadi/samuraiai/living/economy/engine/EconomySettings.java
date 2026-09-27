package yadi.samuraiai.living.economy.engine;

import java.util.List;
import yadi.samuraiai.config.RecordSettingsBuilder;

/** Every tunable value of the Economy & Trade Engine ({@code samuraiai-economy.toml}). Empty lists use the built-in data. */
public record EconomySettings(
        // money and stores at founding (with provenance "founding")
        double settlementEndowment, double merchantEndowment, double foundingStoreDays, int foundingPopulation,
        // storage
        double warehouseCapacity, double merchantStockCapacity, int maxLotsPerResource,
        // production
        double wellWaterPerDay, int farmersPerFarm, double noToolsFactor, double wildlifeShare, double wageShare, double maxHoursPerDay,
        // taxes
        double marketTax, double templeTithe, double levyPerCitizen,
        // balance and prices
        double coverTargetDays, double scarceDays, double surplusDays, double panicDemand, double priceElasticity, double priceSmoothing, double priceEventThreshold, int priceIntervalMinutes,
        // trade and caravans
        int caravanIntervalTicks, double caravanSpeed, double cartCapacity, int maxCarts, int caravanGuards, double ambushScale, int maxDelayMinutes, double minProfit,
        int maxCaravans, double maxTradeDistance, double transportCostPerBlock, double merchantSaleShare,
        // contracts, memory, ledger
        int contractDays, int memoryDays, int ledgerMax, int ledgerPerSettlement,
        boolean debugLogging,
        // data
        List<String> resources, List<String> recipes, List<String> needs) {

    public EconomySettings {
        settlementEndowment = c(settlementEndowment, 0, 1e9); merchantEndowment = c(merchantEndowment, 0, 1e9); foundingStoreDays = c(foundingStoreDays, 0, 365); foundingPopulation = i(foundingPopulation, 0, 10000);
        warehouseCapacity = c(warehouseCapacity, 10, 1e9); merchantStockCapacity = c(merchantStockCapacity, 10, 1e9); maxLotsPerResource = i(maxLotsPerResource, 2, 1000);
        wellWaterPerDay = c(wellWaterPerDay, 0, 1e6); farmersPerFarm = i(farmersPerFarm, 1, 100); noToolsFactor = c(noToolsFactor, 0, 1); wildlifeShare = c(wildlifeShare, 0, 1);
        wageShare = c(wageShare, 0, 1); maxHoursPerDay = c(maxHoursPerDay, 1, 24);
        marketTax = c(marketTax, 0, 0.9); templeTithe = c(templeTithe, 0, 0.9); levyPerCitizen = c(levyPerCitizen, 0, 1000);
        coverTargetDays = c(coverTargetDays, 1, 365); scarceDays = c(scarceDays, 0.1, 100); surplusDays = c(surplusDays, scarceDays * 2, 1000); panicDemand = c(panicDemand, 1, 5);
        priceElasticity = c(priceElasticity, 0.05, 3); priceSmoothing = c(priceSmoothing, 0.01, 1); priceEventThreshold = c(priceEventThreshold, 0.001, 1); priceIntervalMinutes = i(priceIntervalMinutes, 1, 100000);
        caravanIntervalTicks = i(caravanIntervalTicks, 1, 12000); caravanSpeed = c(caravanSpeed, 0.1, 1000); cartCapacity = c(cartCapacity, 1, 1e6); maxCarts = i(maxCarts, 1, 100); caravanGuards = i(caravanGuards, 0, 100);
        ambushScale = c(ambushScale, 0, 10); maxDelayMinutes = i(maxDelayMinutes, 1, 100000); minProfit = c(minProfit, 0, 1e9); maxCaravans = i(maxCaravans, 0, 100000);
        maxTradeDistance = c(maxTradeDistance, 1, 1e7); transportCostPerBlock = c(transportCostPerBlock, 0, 10); merchantSaleShare = c(merchantSaleShare, 0.1, 1);
        contractDays = i(contractDays, 1, 3650); memoryDays = i(memoryDays, 2, 3650); ledgerMax = i(ledgerMax, 64, 10000000); ledgerPerSettlement = i(ledgerPerSettlement, 16, 1000000);
        resources = List.copyOf(resources == null ? List.of() : resources); recipes = List.copyOf(recipes == null ? List.of() : recipes); needs = List.copyOf(needs == null ? List.of() : needs);
    }

    public static EconomySettings defaults() {
        return new EconomySettings(500, 150, 5, 8,
                20000, 800, 12,
                60, 6, 0.5, 0.08, 0.3, 14,
                0.05, 0.02, 0.05,
                7, 2, 20, 1.25, 0.6, 0.35, 0.05, 60,
                20, 3.0, 400, 4, 2, 0.25, 720, 15, 64, 6000, 0.0004, 0.95,
                7, 60, 4000, 400,
                false,
                List.of(), List.of(), List.of());
    }

    private static volatile EconomySettings current = defaults();
    public static EconomySettings current() { return current; }
    public static void apply(EconomySettings next) { current = java.util.Objects.requireNonNull(next); }
    public static Builder builder() { return new Builder(defaults()); }

    public static final class Builder extends RecordSettingsBuilder<EconomySettings, Builder> {
        private Builder(EconomySettings base) { super(EconomySettings.class, base); }
        @Override protected Builder self() { return this; }
    }

    private static double c(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
    private static int i(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
