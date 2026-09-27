package yadi.samuraiai.living.world.roads;

import java.util.List;
import java.util.UUID;

/**
 * A route through the road graph: the nodes and edges in order, its length, the worst and average danger along it, the
 * bridges it crosses and the travel time factor. Physical walking of a stretch is still Navigation's job; this is the world
 * scale plan merchants, caravans, guards and travellers follow.
 */
public record RoutePlan(List<UUID> nodes, List<UUID> edges, double length, double maxDanger, double averageDanger, int bridges, double effectiveLength) {
    public RoutePlan {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }

    public boolean empty() { return edges.isEmpty(); }
}
