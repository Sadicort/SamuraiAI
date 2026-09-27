package yadi.samuraiai.living.world.streaming;

/**
 * The simulation level of detail of a region, from full to historical:
 * <ul>
 *   <li>{@code FULL} (LOD 0) — a player is right there: entities, Brain, navigation all run; the living world adds only
 *       schedule bias and small interactions.</li>
 *   <li>{@code ACTIVE} (LOD 1) — a player is near: villages, economy and families are simulated hour by hour.</li>
 *   <li>{@code SETTLEMENT} (LOD 2) — no player near but the region is not far: settlements are simulated every few hours.</li>
 *   <li>{@code ABSTRACT} (LOD 3) — far away: only economic and community aggregates, once a day.</li>
 *   <li>{@code HISTORICAL} (LOD 4) — nobody has been here for a long time (or the server was off): weekly steps.</li>
 * </ul>
 */
public enum SimulationLevel {
    FULL, ACTIVE, SETTLEMENT, ABSTRACT, HISTORICAL;

    public boolean atLeast(SimulationLevel other) { return ordinal() <= other.ordinal(); }
    public int lod() { return ordinal(); }
}
