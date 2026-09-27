package yadi.samuraiai.ai.navigation.planner;

import yadi.samuraiai.ai.navigation.graph.*;
import yadi.samuraiai.ai.navigation.terrain.*;
import yadi.samuraiai.ai.navigation.zones.DangerMap;

/**
 * Prices one move for one walker. Cost = base edge cost x terrain x floor material x the walker's own
 * biases + weighted danger, or infinity when the walker refuses the node outright (forbidden zone,
 * lava, danger above tolerance).
 */
public final class PathCostModel {
    private final TerrainCostTable table;
    private final DangerMap dangers;
    private final String dimension;
    private final PathPreferences prefs;
    private final NavEnvironment environment;
    private final long tick;

    public PathCostModel(TerrainCostTable table, DangerMap dangers, String dimension, PathPreferences prefs,
                         NavEnvironment environment, long tick) {
        this.table = table; this.dangers = dangers; this.dimension = dimension; this.prefs = prefs;
        this.environment = environment == null ? NavEnvironment.CLEAR_DAY : environment; this.tick = tick;
    }

    public double danger(NavNode node) {
        return node.staticDanger() + dangers.dynamicDanger(dimension, node.pos(), tick);
    }

    public double edgeCost(NavNode to, EdgeType type, double baseCost) {
        if (to.terrain() == TerrainType.LAVA || dangers.isForbidden(dimension, to.pos())) return Double.POSITIVE_INFINITY;
        double danger = danger(to);
        if (danger > prefs.maxDanger()) return Double.POSITIVE_INFINITY;
        double weather = 1.0D;
        if (environment.raining() && (to.floor() == Material.MUD || to.floor() == Material.SAND)) weather = 1.25D;
        if (environment.thundering() && to.terrain() == TerrainType.WATER) weather *= 1.2D;
        double multiplier = table.terrain(to.terrain()) * prefs.bias(to.terrain())
                * table.material(to.floor()) * prefs.bias(to.floor()) * weather;
        double dangerWeight = prefs.dangerWeight() * (environment.night() ? 1.25D : 1.0D);
        return baseCost * multiplier + danger * dangerWeight;
    }

    /** Lower bound on the cost of the remaining distance; valid because every multiplier is at least 1. */
    public static double heuristic(NavPos from, NavPos goal) {
        double dx = Math.abs(from.x() - goal.x()), dz = Math.abs(from.z() - goal.z());
        double horizontal = Math.max(dx, dz) + (Math.sqrt(2) - 1) * Math.min(dx, dz);
        return horizontal + 0.5D * Math.abs(from.y() - goal.y());
    }
}
