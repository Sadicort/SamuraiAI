package yadi.samuraiai.ai.navigation.pathfinding;

import java.util.*;
import yadi.samuraiai.ai.navigation.graph.*;
import yadi.samuraiai.ai.navigation.planner.PathCostModel;
import yadi.samuraiai.ai.navigation.planner.PathPreferences;

/**
 * Resumable A*. {@link #advance(int)} expands at most that many nodes and returns, so a long search is
 * spread across ticks instead of spiking one. State is fully owned by the instance; nothing is shared.
 */
public final class PathSearch {
    public enum Status { IN_PROGRESS, FOUND, NO_PATH, LIMIT }

    private static final class Entry {
        final NavPos pos;
        final Entry parent;
        final EdgeType via;
        final double g, f, h;
        Entry(NavPos pos, Entry parent, EdgeType via, double g, double h) {
            this.pos = pos; this.parent = parent; this.via = via; this.g = g; this.h = h; this.f = g + h;
        }
    }

    private final NavigationGraph graph;
    private final PathCostModel costs;
    private final PathPreferences prefs;
    private final NavPos start, goal;
    private final int maxNodes;
    private final double corridorSlack;
    private final Set<NavPos> blocked;
    private final PriorityQueue<Entry> open =
            new PriorityQueue<>(Comparator.<Entry>comparingDouble(e -> e.f).thenComparingDouble(e -> e.h));
    private final Map<NavPos, Double> bestG = new HashMap<>();
    private final Set<NavPos> closed = new HashSet<>();
    private Entry closest;
    private Entry found;
    private int expanded;
    private Status status = Status.IN_PROGRESS;
    private long spentNanos;

    public PathSearch(NavigationGraph graph, PathCostModel costs, PathPreferences prefs, NavPos start, NavPos goal,
                      int maxNodes, int maxRadius, Set<NavPos> blocked) {
        this.graph = graph; this.costs = costs; this.prefs = prefs; this.start = start; this.goal = goal;
        this.maxNodes = maxNodes; this.corridorSlack = maxRadius;
        this.blocked = blocked == null ? Set.of() : blocked;
        Entry first = new Entry(start, null, null, 0, PathCostModel.heuristic(start, goal));
        open.add(first);
        bestG.put(start, 0.0D);
        closest = first;
        if (start.equals(goal)) { found = first; status = Status.FOUND; }
    }

    public Status status() { return status; }
    public int expanded() { return expanded; }
    public long spentNanos() { return spentNanos; }
    public NavPos start() { return start; }
    public NavPos goal() { return goal; }
    public PathCostModel costs() { return costs; }
    public Set<NavPos> visited() { return Collections.unmodifiableSet(closed); }

    public Status advance(int budget) {
        if (status != Status.IN_PROGRESS) return status;
        long began = System.nanoTime();
        int spent = 0;
        double direct = start.distance(goal);
        while (spent < budget) {
            Entry current = open.poll();
            if (current == null) { status = Status.NO_PATH; break; }
            if (!closed.add(current.pos)) continue;
            if (current.pos.equals(goal)) { found = current; status = Status.FOUND; break; }
            spent++;
            expanded++;
            if (current.h < closest.h || (current.h == closest.h && current.g < closest.g)) closest = current;
            if (expanded >= maxNodes) { status = Status.LIMIT; break; }
            final Entry parent = current;
            graph.neighbors(current.pos, prefs, (to, type, base) -> {
                if (closed.contains(to) || blocked.contains(to)) return;
                if (start.distance(to) + to.distance(goal) > direct + corridorSlack * 2) return;
                double step = costs.edgeCost(graph.node(to), type, base);
                if (Double.isInfinite(step)) return;
                double g = parent.g + step;
                Double known = bestG.get(to);
                if (known != null && known <= g) return;
                bestG.put(to, g);
                open.add(new Entry(to, parent, type, g, PathCostModel.heuristic(to, goal)));
            });
        }
        spentNanos += System.nanoTime() - began;
        return status;
    }

    /** The found path, or (for NO_PATH / LIMIT) the reachable node that got closest to the goal. */
    public Optional<NavigationPath> result(long tick, int minPartialGain) {
        Entry end = found != null ? found : closest;
        boolean partial = found == null;
        if (partial && (end == null || end.parent == null || start.horizontalDistance(end.pos) < minPartialGain))
            return Optional.empty();
        ArrayDeque<PathNode> reversed = new ArrayDeque<>();
        for (Entry e = end; e != null; e = e.parent)
            reversed.addFirst(new PathNode(e.pos, e.via == null ? EdgeType.WALK : e.via));
        return Optional.of(new NavigationPath(new ArrayList<>(reversed), end.g, partial, tick, expanded));
    }
}
