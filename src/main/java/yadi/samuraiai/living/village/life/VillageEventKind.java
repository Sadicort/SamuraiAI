package yadi.samuraiai.living.village.life;

import java.util.Map;

/**
 * Kinds of village event and what each does to the community's day: {@code bias} for everyone (routine name → points),
 * {@code guardBias} added for guards and samurai, whether it closes or extends the market and whether civilians keep to their
 * homes. FESTIVAL's bias comes from the calendar's festival definition instead.
 */
public enum VillageEventKind {
    SPECIAL_MARKET(Map.of("MERCHANT", 40.0, "SOCIAL", 20.0), Map.of(), false, true, false),
    RAIN(Map.of("WORK", -15.0, "REST", 10.0, "SOCIAL", 5.0, "PATROL", -10.0), Map.of("GUARD", 10.0), false, false, false),
    ATTACK(Map.of("REST", 60.0, "SOCIAL", -40.0, "WORK", -40.0, "MERCHANT", -60.0, "PRAYER", -20.0), Map.of("GUARD", 90.0, "PATROL", 70.0, "REST", -80.0), true, false, true),
    CELEBRATION(Map.of("SOCIAL", 40.0, "EAT", 15.0, "WORK", -20.0), Map.of(), false, true, false),
    TRAINING(Map.of(), Map.of("TRAINING", 40.0), false, false, false),
    PROCESSION(Map.of("PRAYER", 30.0, "SOCIAL", 15.0, "WORK", -10.0), Map.of("PATROL", 10.0), false, false, false),
    EMERGENCY(Map.of("WORK", -30.0, "REST", 10.0, "SOCIAL", -10.0), Map.of("GUARD", 40.0, "PATROL", 30.0), false, false, false),
    FESTIVAL(Map.of(), Map.of(), false, true, false),
    CEREMONY(Map.of("PRAYER", 35.0, "MEDITATE", 10.0, "WORK", -10.0), Map.of(), false, false, false);

    private final Map<String, Double> bias, guardBias;
    private final boolean closesMarket, extendsMarket, keepsHome;

    VillageEventKind(Map<String, Double> bias, Map<String, Double> guardBias, boolean closesMarket, boolean extendsMarket, boolean keepsHome) {
        this.bias = bias; this.guardBias = guardBias; this.closesMarket = closesMarket; this.extendsMarket = extendsMarket; this.keepsHome = keepsHome;
    }

    public Map<String, Double> bias() { return bias; }
    public Map<String, Double> guardBias() { return guardBias; }
    public boolean closesMarket() { return closesMarket; }
    public boolean extendsMarket() { return extendsMarket; }
    public boolean keepsHome() { return keepsHome; }
}
