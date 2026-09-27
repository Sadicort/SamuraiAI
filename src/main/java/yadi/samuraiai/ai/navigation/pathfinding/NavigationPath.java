package yadi.samuraiai.ai.navigation.pathfinding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.navigation.graph.NavPos;

/** Immutable result of a search. The mutable progress through it lives in the navigation session. */
public final class NavigationPath {
    private final UUID id = UUID.randomUUID();
    private final List<PathNode> nodes;
    private final double cost;
    private final boolean partial;
    private final long createdTick;
    private final int expanded;
    private final Set<Long> chunks;

    public NavigationPath(List<PathNode> nodes, double cost, boolean partial, long createdTick, int expanded) {
        if (nodes == null || nodes.isEmpty()) throw new IllegalArgumentException("A path needs at least one node");
        this.nodes = List.copyOf(nodes);
        this.cost = cost;
        this.partial = partial;
        this.createdTick = createdTick;
        this.expanded = expanded;
        Set<Long> keys = new HashSet<>();
        for (PathNode node : this.nodes) keys.add(node.pos().chunkKey());
        this.chunks = Set.copyOf(keys);
    }

    public UUID id() { return id; }
    public List<PathNode> nodes() { return nodes; }
    public int size() { return nodes.size(); }
    public PathNode get(int index) { return nodes.get(index); }
    public NavPos start() { return nodes.get(0).pos(); }
    public NavPos end() { return nodes.get(nodes.size() - 1).pos(); }
    public double cost() { return cost; }
    public boolean partial() { return partial; }
    public long createdTick() { return createdTick; }
    public int expanded() { return expanded; }
    public Set<Long> chunks() { return chunks; }

    public int indexOf(NavPos pos) {
        for (int i = 0; i < nodes.size(); i++) if (nodes.get(i).pos().equals(pos)) return i;
        return -1;
    }

    /** The remainder of this path starting at {@code index}, for reuse when a walker is already partway along. */
    public NavigationPath suffixFrom(int index, long tick) {
        if (index <= 0) return this;
        double share = (double) (nodes.size() - index) / Math.max(1, nodes.size());
        return new NavigationPath(nodes.subList(index, nodes.size()), cost * share, partial, tick, 0);
    }

    /** Total geometric length in blocks. */
    public double length() {
        double total = 0;
        for (int i = 1; i < nodes.size(); i++) total += nodes.get(i - 1).pos().distance(nodes.get(i).pos());
        return total;
    }
}
