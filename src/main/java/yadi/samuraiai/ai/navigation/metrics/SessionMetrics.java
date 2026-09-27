package yadi.samuraiai.ai.navigation.metrics;

/** Counters of one navigation session, folded into the global metrics when the session ends. Server-thread only. */
public final class SessionMetrics {
    public long ticks, searchNanos, movementNanos;
    public int nodesExpanded, recalculations, blocks, obstacles, doorsOpened, doorsClosed, jumps, chunksEntered, stuckEvents, recoveries;
    public double distance, cost;
    public boolean cacheHit;

    @Override public String toString() {
        return "ticks=" + ticks + " dist=" + String.format("%.1f", distance) + " cost=" + String.format("%.1f", cost)
                + " expanded=" + nodesExpanded + " recalc=" + recalculations + " blocks=" + blocks + " obstacles=" + obstacles
                + " doors=" + doorsOpened + " jumps=" + jumps + " chunks=" + chunksEntered + " stuck=" + stuckEvents
                + " cacheHit=" + cacheHit;
    }
}
