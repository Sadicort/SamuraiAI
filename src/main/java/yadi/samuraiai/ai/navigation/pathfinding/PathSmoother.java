package yadi.samuraiai.ai.navigation.pathfinding;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.ai.navigation.graph.*;
import yadi.samuraiai.ai.navigation.planner.PathCostModel;
import yadi.samuraiai.ai.navigation.planner.PathPreferences;
import yadi.samuraiai.ai.navigation.terrain.TerrainType;

/**
 * Removes the grid staircase from long flat stretches (string pulling) while keeping every node where the
 * edge type or terrain matters: steps, drops, doors, ladders, bridges, water and cliffs stay exactly as found.
 */
public final class PathSmoother {
    private final NavigationGraph graph;

    public PathSmoother(NavigationGraph graph) { this.graph = graph; }

    public NavigationPath smooth(NavigationPath path, PathPreferences prefs, PathCostModel costs, long tick) {
        if (path.size() < 3) return path;
        List<PathNode> out = new ArrayList<>();
        out.add(path.get(0));
        int i = 0;
        while (i < path.size() - 1) {
            int best = i + 1;
            for (int j = path.size() - 1; j > i + 1; j--) {
                if (canShortcut(path, i, j, prefs, costs)) { best = j; break; }
            }
            out.add(path.get(best));
            i = best;
        }
        if (out.size() == path.size()) return path;
        return new NavigationPath(out, path.cost(), path.partial(), tick, path.expanded());
    }

    private static boolean plain(TerrainType terrain) {
        return terrain == TerrainType.NORMAL || terrain == TerrainType.SAFE || terrain == TerrainType.FOREST
                || terrain == TerrainType.VILLAGE;
    }

    private boolean canShortcut(NavigationPath path, int from, int to, PathPreferences prefs, PathCostModel costs) {
        int y = path.get(from).pos().y();
        for (int k = from; k <= to; k++) {
            PathNode node = path.get(k);
            if (node.pos().y() != y) return false;
            if (k > from && node.via() != EdgeType.WALK && node.via() != EdgeType.DIAGONAL) return false;
            if (!plain(graph.node(node.pos()).terrain())) return false;
        }
        NavPos a = path.get(from).pos(), b = path.get(to).pos();
        double length = a.horizontalDistance(b);
        int samples = (int) Math.ceil(length / 0.4D);
        for (int s = 1; s < samples; s++) {
            double t = (double) s / samples;
            double sx = a.centerX() + (b.centerX() - a.centerX()) * t, sz = a.centerZ() + (b.centerZ() - a.centerZ()) * t;
            NavPos cell = new NavPos((int) Math.floor(sx), y, (int) Math.floor(sz));
            if (!graph.footprintStandable(sx, sz, y, prefs, false) || !plain(graph.node(cell).terrain())) return false;
            double here = costs.danger(graph.node(cell));
            if (here > Math.max(costs.danger(graph.node(a)), costs.danger(graph.node(b))) + 5 || Double.isInfinite(costs.edgeCost(graph.node(cell), EdgeType.WALK, 1.0D))) return false;
        }
        return true;
    }
}
