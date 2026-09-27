package yadi.samuraiai.ai.memory.model;

/** How much a memory matters. The weight (0-1) feeds retrieval ranking and forgetting. */
public enum Importance {
    TRIVIAL(0.05), LOW(0.2), NORMAL(0.4), HIGH(0.6), IMPORTANT(0.8), CRITICAL(0.95), LEGENDARY(1.0);

    private final double weight;
    Importance(double weight) { this.weight = weight; }
    public double weight() { return weight; }
    public boolean atLeast(Importance other) { return ordinal() >= other.ordinal(); }
    public Importance raise() { return values()[Math.min(values().length - 1, ordinal() + 1)]; }
}
