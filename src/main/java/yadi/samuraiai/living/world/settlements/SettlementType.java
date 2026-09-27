package yadi.samuraiai.living.world.settlements;

import java.util.Locale;
import java.util.Optional;

/**
 * Kinds of settlement, each with its own rules: whether it holds a market and a warehouse, whether it has permanent residents
 * and may grow, the culture it defaults to and how well defended it starts. FORTRESS is prepared for later (it can be
 * created by command; nothing founds one on its own).
 */
public enum SettlementType {
    VILLAGE(true, true, true, true, "village", 0.3D),
    TEMPLE(false, true, true, false, "temple", 0.4D),
    CAMP(false, false, false, false, "guard", 0.2D),
    MARKET(true, true, false, true, "market", 0.3D),
    FORTRESS(false, true, true, false, "guard", 0.9D);

    private final boolean market, warehouse, residents, grows;
    private final String culture;
    private final double defence;

    SettlementType(boolean market, boolean warehouse, boolean residents, boolean grows, String culture, double defence) {
        this.market = market; this.warehouse = warehouse; this.residents = residents; this.grows = grows; this.culture = culture; this.defence = defence;
    }

    public boolean hasMarket() { return market; }
    public boolean hasWarehouse() { return warehouse; }
    public boolean hasResidents() { return residents; }
    public boolean grows() { return grows; }
    public String defaultCulture() { return culture; }
    public double baseDefence() { return defence; }

    public static Optional<SettlementType> parse(String text) {
        if (text == null) return Optional.empty();
        try { return Optional.of(valueOf(text.trim().toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
}
