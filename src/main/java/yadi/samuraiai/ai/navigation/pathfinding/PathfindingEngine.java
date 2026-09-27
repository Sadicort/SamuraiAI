package yadi.samuraiai.ai.navigation.pathfinding;

import java.util.Optional;
import java.util.Set;
import yadi.samuraiai.ai.navigation.graph.*;
import yadi.samuraiai.ai.navigation.planner.PathCostModel;
import yadi.samuraiai.ai.navigation.planner.PathPreferences;

/**
 * Algorithm only: builds searches, finishes them into smoothed paths, validates paths. It never touches
 * an entity and never decides where to go.
 */
public final class PathfindingEngine {
    private final NavigationGraph graph;
    private final PathSmoother smoother;
    private final PathValidator validator;

    public PathfindingEngine(NavigationGraph graph) {
        this.graph = graph;
        this.smoother = new PathSmoother(graph);
        this.validator = new PathValidator(graph);
    }

    public NavigationGraph graph() { return graph; }
    public PathValidator validator() { return validator; }

    public PathSearch begin(PathCostModel costs, PathPreferences prefs, NavPos start, NavPos goal, int maxNodes,
                            int maxRadius, Set<NavPos> blocked) {
        return new PathSearch(graph, costs, prefs, start, goal, maxNodes, maxRadius, blocked);
    }

    /** Converts a finished search into a smoothed path, or empty when nothing useful was found. */
    public Optional<NavigationPath> finish(PathSearch search, PathPreferences prefs, long tick, int minPartialGain) {
        return search.result(tick, minPartialGain).map(path -> smoother.smooth(path, prefs, search.costs(), tick));
    }

    /** Runs a search to completion; for tests and small local detours where a budgeted search is overkill. */
    public Optional<NavigationPath> solve(PathCostModel costs, PathPreferences prefs, NavPos start, NavPos goal,
                                          int maxNodes, int maxRadius, Set<NavPos> blocked, long tick) {
        PathSearch search = begin(costs, prefs, start, goal, maxNodes, maxRadius, blocked);
        search.advance(maxNodes);
        return finish(search, prefs, tick, 1);
    }
}
