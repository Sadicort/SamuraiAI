package yadi.samuraiai.ai.knowledge.culture;

/** How alive a tradition is in one community: its current strength (0-1, reinforced when observed, eroding when neglected), how often it was observed and when last. */
public final class TraditionState {
    private double strength;
    private int observed;
    private long lastObserved, lastDecay;

    public TraditionState(double strength, long now) { this.strength = strength; this.lastDecay = now; }

    public double strength() { return strength; }
    public void strength(double v) { strength = Math.max(0.0D, Math.min(1.0D, v)); }
    public int observed() { return observed; }
    public void observed(int v) { observed = Math.max(0, v); }
    public long lastObserved() { return lastObserved; }
    public void lastObserved(long v) { lastObserved = v; }
    public long lastDecay() { return lastDecay; }
    public void lastDecay(long v) { lastDecay = v; }
}
