package yadi.samuraiai.ai.navigation.planner;

import yadi.samuraiai.ai.navigation.graph.*;
import yadi.samuraiai.ai.navigation.terrain.HazardAnalyzer;
import yadi.samuraiai.ai.navigation.zones.DangerMap;

/** Refuses impossible destinations before any search is spent: outside the world, unloaded, lava, void, forbidden, unstandable. */
public final class DestinationValidator {
    private static final int SNAP_HORIZONTAL = 2, SNAP_VERTICAL = 3;
    private final NavigationGraph graph;
    private final DangerMap dangers;

    public DestinationValidator(NavigationGraph graph, DangerMap dangers) { this.graph = graph; this.dangers = dangers; }

    public DestinationResult validate(NavPos requested, PathPreferences prefs, long tick) {
        NavWorldView view = graph.view();
        if (requested.y() < view.minY() || requested.y() >= view.maxY())
            return DestinationResult.reject(DestinationResult.Status.OUT_OF_WORLD, "y=" + requested.y() + " outside build limits");
        if (!view.isLoaded(requested.chunkX(), requested.chunkZ()))
            return DestinationResult.reject(DestinationResult.Status.CHUNK_UNLOADED,
                    "chunk " + requested.chunkX() + "," + requested.chunkZ() + " not loaded");
        if (view.profile(requested).isLava() || view.profile(requested.offset(0, -1, 0)).isLava())
            return DestinationResult.reject(DestinationResult.Status.LAVA, "destination is in or on lava");
        if (dangers.isForbidden(view.dimension(), requested))
            return DestinationResult.reject(DestinationResult.Status.FORBIDDEN, "destination inside forbidden zone");
        NavPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (int step = 0; step <= SNAP_VERTICAL * 2; step++) {
            int offsetY = (step % 2 == 0 ? 1 : -1) * ((step + 1) / 2);
            for (int dx = -SNAP_HORIZONTAL; dx <= SNAP_HORIZONTAL; dx++) for (int dz = -SNAP_HORIZONTAL; dz <= SNAP_HORIZONTAL; dz++) {
                NavPos candidate = requested.offset(dx, offsetY, dz);
                if (!view.isLoaded(candidate.chunkX(), candidate.chunkZ()) || !graph.standable(candidate, prefs)) continue;
                if (dangers.isForbidden(view.dimension(), candidate)) continue;
                double score = Math.abs(dx) + Math.abs(dz) + Math.abs(offsetY) * 1.5D + graph.node(candidate).staticDanger() * 0.05D;
                if (score < bestScore) { bestScore = score; best = candidate; }
            }
        }
        if (best == null) {
            boolean void_ = new HazardAnalyzer(view, 100).dropBelow(requested.x(), requested.y(), requested.z()) >= HazardAnalyzer.DROP_SCAN_LIMIT;
            return void_ ? DestinationResult.reject(DestinationResult.Status.VOID, "no ground below destination")
                    : DestinationResult.reject(DestinationResult.Status.BLOCKED, "no standable position near destination");
        }
        double danger = graph.node(best).staticDanger() + dangers.dynamicDanger(view.dimension(), best, tick);
        if (danger > prefs.maxDanger())
            return DestinationResult.reject(DestinationResult.Status.DANGEROUS,
                    "danger " + (int) danger + " above tolerance " + (int) prefs.maxDanger());
        return best.equals(requested) ? new DestinationResult(DestinationResult.Status.OK, best, "exact")
                : new DestinationResult(DestinationResult.Status.ADJUSTED, best, "snapped from " + requested);
    }
}
