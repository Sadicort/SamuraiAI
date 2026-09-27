package yadi.samuraiai.living.economy.taxation;

/**
 * Taxes of one settlement: a share of every market sale (to the settlement's treasury), the temple's tithe on the value of
 * what is produced, a daily levy per citizen who has the money, and the share a ruling faction would take (prepared for
 * politics; 0 until a faction owns the territory). Collected amounts are kept per kind.
 */
public final class TaxPolicy {
    private double marketTax, templeTithe, levyPerCitizen, factionShare;
    private double collectedMarket, collectedTithe, collectedLevy, collectedFaction;

    public TaxPolicy(double marketTax, double templeTithe, double levyPerCitizen, double factionShare) {
        this.marketTax = clamp(marketTax); this.templeTithe = clamp(templeTithe); this.levyPerCitizen = Math.max(0, levyPerCitizen); this.factionShare = clamp(factionShare);
    }

    private static double clamp(double v) { return Math.max(0.0D, Math.min(0.9D, v)); }

    public double marketTax() { return marketTax; }
    public double templeTithe() { return templeTithe; }
    public double levyPerCitizen() { return levyPerCitizen; }
    public double factionShare() { return factionShare; }
    public void set(double market, double tithe, double levy, double faction) { marketTax = clamp(market); templeTithe = clamp(tithe); levyPerCitizen = Math.max(0, levy); factionShare = clamp(faction); }
    public void collectedMarket(double v) { collectedMarket += v; }
    public void collectedTithe(double v) { collectedTithe += v; }
    public void collectedLevy(double v) { collectedLevy += v; }
    public void collectedFaction(double v) { collectedFaction += v; }
    public double totalMarket() { return collectedMarket; }
    public double totalTithe() { return collectedTithe; }
    public double totalLevy() { return collectedLevy; }
    public double totalFaction() { return collectedFaction; }
    public void restoreTotals(double m, double t, double l, double f) { collectedMarket = m; collectedTithe = t; collectedLevy = l; collectedFaction = f; }
}
