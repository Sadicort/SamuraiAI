package yadi.samuraiai.ai.navigation.pathfinding;

import java.util.concurrent.atomic.AtomicBoolean;
import yadi.samuraiai.ai.navigation.graph.*;
import yadi.samuraiai.ai.navigation.planner.PathCostModel;
import yadi.samuraiai.ai.navigation.planner.PathPreferences;

/** Re-checks the upcoming stretch of a path against the current world, since blocks and hazards change under a walker. */
public final class PathValidator {
    /** How much hotter than both endpoints a cell in a straight run may be before the run is replanned. */
    private static final double DANGER_TOLERANCE = 20.0D;
    private final NavigationGraph graph;

    public PathValidator(NavigationGraph graph) { this.graph = graph; }

    public PathValidation validate(NavigationPath path, int fromIndex, int lookahead, PathPreferences prefs, PathCostModel costs) {
        int last = Math.min(path.size() - 1, fromIndex + lookahead);
        for (int i = Math.max(0, fromIndex); i <= last; i++) {
            NavPos pos = path.get(i).pos();
            if (!graph.view().isLoaded(pos.chunkX(), pos.chunkZ()))
                return new PathValidation(i, PathValidation.Reason.CHUNK_UNLOADED);
            if (!graph.standable(pos, prefs)) return new PathValidation(i, PathValidation.Reason.NODE_UNSTANDABLE);
            if (Double.isInfinite(costs.edgeCost(graph.node(pos), path.get(i).via(), 1.0D)))
                return new PathValidation(i, PathValidation.Reason.DANGEROUS);
            if (i > 0) {
                AtomicBoolean reachable = new AtomicBoolean();
                NavPos next = pos;
                graph.neighbors(path.get(i - 1).pos(), prefs, (to, type, base) -> { if (to.equals(next)) reachable.set(true); });
                if (!reachable.get()) {
                    if (!shortcutAllowed(path, i - 1, i) || !runIsWalkable(path.get(i - 1).pos(), pos, prefs, costs))
                        return new PathValidation(i, PathValidation.Reason.EDGE_ILLEGAL);
                }
            }
        }
        return PathValidation.VALID;
    }

    /** Every cell along a smoothed straight run must still be standable in the live world. */
    private boolean runIsWalkable(NavPos a, NavPos b, PathPreferences prefs, PathCostModel costs) {
        double ends = Math.max(costs.danger(graph.node(a)), costs.danger(graph.node(b)));
        double length = a.horizontalDistance(b);
        int samples = (int) Math.ceil(length / 0.5D);
        for (int s = 1; s < samples; s++) {
            double t = (double) s / samples;
            double sx = a.centerX() + (b.centerX() - a.centerX()) * t, sz = a.centerZ() + (b.centerZ() - a.centerZ()) * t;
            NavPos cell = new NavPos((int) Math.floor(sx), a.y(), (int) Math.floor(sz));
            if (!graph.footprintStandable(sx, sz, a.y(), prefs, true)) return false;
            var node = graph.node(cell);
            if (Double.isInfinite(costs.edgeCost(node, EdgeType.WALK, 1.0D)) || costs.danger(node) > ends + DANGER_TOLERANCE) return false;
        }
        return true;
    }

    /** Smoothed paths contain straight flat runs longer than one cell; those are validated by walkability, not adjacency. */
    private boolean shortcutAllowed(NavigationPath path, int a, int b) {
        NavPos from = path.get(a).pos(), to = path.get(b).pos();
        return from.y() == to.y() && from.horizontalDistance(to) > 1.5D;
    }
}
