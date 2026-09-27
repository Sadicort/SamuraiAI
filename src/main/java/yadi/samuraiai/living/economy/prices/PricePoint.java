package yadi.samuraiai.living.economy.prices;

import java.util.Map;

/**
 * The price of one resource in one market, with the inputs that made it: supply (stock), demand (what the settlement needs to
 * keep {@code coverDays} in store), and the factors applied (supply/demand, season, war, scarcity, wealth, import distance).
 */
public record PricePoint(String resource, double price, double supply, double demand, Map<String, Double> factors, long updatedAt) {
    public PricePoint { factors = Map.copyOf(factors); }
}
