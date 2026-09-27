package yadi.samuraiai.living.economy.scarcity;

/**
 * Scarcity and surplus of one resource in one settlement, from how many days its stock covers what the settlement consumes.
 * With hysteresis: scarcity begins below {@code scarceDays} and ends only above {@code scarceDays × 1.5}; surplus begins
 * above {@code surplusDays} and ends below {@code surplusDays × 0.7}. Consequences (prices, panic buying, exports, unrest,
 * quests) are applied by the Economy Engine and the hub when the state changes.
 */
public final class MarketBalance {
    public enum State { NORMAL, SCARCE, SURPLUS }

    private final String resource;
    private State state = State.NORMAL;
    private double coverDays = Double.POSITIVE_INFINITY;
    private long since;

    public MarketBalance(String resource) { this.resource = resource; }

    public String resource() { return resource; }
    public State state() { return state; }
    public double coverDays() { return coverDays; }
    public long since() { return since; }

    /** Updates with the current cover; returns the previous state when it changed, else null. */
    public State update(double cover, double scarceDays, double surplusDays, long now) {
        coverDays = cover;
        State next = state;
        switch (state) {
            case NORMAL -> { if (cover < scarceDays) next = State.SCARCE; else if (cover > surplusDays) next = State.SURPLUS; }
            case SCARCE -> { if (cover > scarceDays * 1.5D) next = cover > surplusDays ? State.SURPLUS : State.NORMAL; }
            case SURPLUS -> { if (cover < surplusDays * 0.7D) next = cover < scarceDays ? State.SCARCE : State.NORMAL; }
        }
        if (next == state) return null;
        State before = state;
        state = next;
        since = now;
        return before;
    }

    public void restore(State s, double cover, long sinceMinute) { state = s; coverDays = cover; since = sinceMinute; }
}
