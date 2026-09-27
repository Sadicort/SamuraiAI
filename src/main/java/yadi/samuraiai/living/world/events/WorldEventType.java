package yadi.samuraiai.living.world.events;

import java.util.Locale;
import java.util.Optional;

/**
 * Kinds of world event. Each has a default preparation time and duration (minutes), whether it concerns a whole region or one
 * settlement, how much danger it adds to its region and roads while it runs, whether it blocks the roads through its region,
 * and whether it is a hostile event (villages raise their alarm). WAR, ATTACK and BANDITS are how armed conflict enters the
 * living world until a dedicated battlefield system exists.
 */
public enum WorldEventType {
    FESTIVAL(24 * 60, 3 * 24 * 60, false, 0.0D, false, false),
    RAIN(0, 12 * 60, true, 0.0D, false, false),
    STORM(60, 8 * 60, true, 0.15D, false, false),
    FLOOD(6 * 60, 2 * 24 * 60, true, 0.2D, true, false),
    FIRE(0, 6 * 60, false, 0.1D, false, false),
    ATTACK(30, 4 * 60, false, 0.35D, false, true),
    BANDITS(2 * 60, 5 * 24 * 60, true, 0.4D, false, true),
    WAR(3 * 24 * 60, 20 * 24 * 60, true, 0.5D, true, true),
    MARKET(12 * 60, 24 * 60, false, 0.0D, false, false),
    CELEBRATION(12 * 60, 24 * 60, false, 0.0D, false, false),
    DUEL(4 * 60, 60, false, 0.0D, false, false),
    EMERGENCY(0, 12 * 60, false, 0.2D, false, false);

    private final int prepareMinutes, durationMinutes;
    private final boolean regional, blocksRoads, hostile;
    private final double danger;

    WorldEventType(int prepareMinutes, int durationMinutes, boolean regional, double danger, boolean blocksRoads, boolean hostile) {
        this.prepareMinutes = prepareMinutes; this.durationMinutes = durationMinutes; this.regional = regional; this.danger = danger; this.blocksRoads = blocksRoads; this.hostile = hostile;
    }

    public int prepareMinutes() { return prepareMinutes; }
    public int durationMinutes() { return durationMinutes; }
    public boolean regional() { return regional; }
    public double danger() { return danger; }
    public boolean blocksRoads() { return blocksRoads; }
    public boolean hostile() { return hostile; }

    public static Optional<WorldEventType> parse(String text) {
        if (text == null) return Optional.empty();
        try { return Optional.of(valueOf(text.trim().toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
}
