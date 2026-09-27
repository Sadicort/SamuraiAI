package yadi.samuraiai.living.economy.prices;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Dynamic Price Engine. A price is the base value times a factor for each variable the specification names:
 * <ul>
 *   <li>supply and demand — {@code (demand / supply)^elasticity}, bounded;</li>
 *   <li>season — crops cost less in their harvest and more out of it;</li>
 *   <li>war — danger in the region makes food, tools and fuel dearer;</li>
 *   <li>production — goods the settlement does not produce carry an import premium that grows with distance;</li>
 *   <li>scarcity / surplus — the market's mood on top of the numbers;</li>
 *   <li>wealth — richer settlements pay more.</li>
 * </ul>
 * Prices move towards the target gradually ({@code smoothing}) and are recomputed on the economy's cadence, never per tick.
 */
public final class PriceEngine {
    public record Inputs(double baseValue, double supply, double demand, boolean harvestSeason, boolean offSeasonCrop, double warDanger, boolean warSensitive, boolean importedGood,
                         double importDistance, boolean scarce, boolean surplus, double prosperity) { }

    private double elasticity = 0.6D, smoothing = 0.35D, minFactor = 0.25D, maxFactor = 5.0D;

    public void configure(double e, double s, double min, double max) { elasticity = e; smoothing = s; minFactor = min; maxFactor = max; }

    public PricePoint price(String resource, Inputs in, PricePoint previous, long now) {
        Map<String, Double> f = new LinkedHashMap<>();
        double sd = Math.pow(Math.max(0.01D, in.demand()) / Math.max(0.5D, in.supply()), elasticity);
        f.put("oferta/demanda", clamp(sd, 0.3D, 4.0D));
        f.put("estación", in.harvestSeason() ? 0.85D : in.offSeasonCrop() ? 1.15D : 1.0D);
        f.put("guerra", in.warSensitive() ? 1.0D + 0.6D * Math.max(0, Math.min(1, in.warDanger())) : 1.0D);
        f.put("importación", in.importedGood() ? 1.1D + Math.min(0.5D, in.importDistance() / 8000.0D) : 1.0D);
        f.put("escasez", in.scarce() ? 1.25D : in.surplus() ? 0.85D : 1.0D);
        f.put("riqueza", 0.85D + 0.3D * Math.max(0, Math.min(1, in.prosperity())));
        double factor = 1.0D;
        for (double v : f.values()) factor *= v;
        double target = in.baseValue() * clamp(factor, minFactor, maxFactor);
        double price = previous == null ? target : previous.price() + (target - previous.price()) * smoothing;
        return new PricePoint(resource, Math.max(0.01D, price), in.supply(), in.demand(), f, now);
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
