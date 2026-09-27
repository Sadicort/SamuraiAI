package yadi.samuraiai.ai.navigation.terrain;

import java.util.Map;
import yadi.samuraiai.ai.navigation.graph.NavPos;

/** Summary of the ground around a point at a moment in time: how much of each terrain type, how dangerous. */
public record TerrainSnapshot(String dimension, NavPos center, int radius, Map<TerrainType, Integer> counts,
                              int standableCells, double averageDanger, double maxDanger, long createdTick) {
    public TerrainSnapshot {
        counts = Map.copyOf(counts);
    }
    public int count(TerrainType type) { return counts.getOrDefault(type, 0); }
    public boolean expired(long tick, int ttlTicks) { return tick - createdTick >= ttlTicks; }
}
