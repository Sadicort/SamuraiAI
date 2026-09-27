package yadi.samuraiai.living.world.roads;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * The world road graph: roads, trails, bridges, crossroads and shortcuts between settlements, with danger and blockages. It
 * answers "how does one get from here to there" at world scale with Dijkstra over the graph, weighting length by danger
 * according to the traveller's preferences, and refuses blocked stretches. Blocking and reopening are recorded so trade
 * routes can be invalidated by event instead of re-planned by polling.
 */
public final class RoadNetwork {
    /** How a traveller weighs a route: extra cost per unit of danger (in blocks per block), whether trails are acceptable, and the most danger it will cross. */
    public record Preferences(double dangerWeight, boolean allowTrails, double maxDanger) {
        public static final Preferences CARAVAN = new Preferences(4.0D, false, 0.85D);
        public static final Preferences TRAVELLER = new Preferences(2.0D, true, 0.95D);
        public static final Preferences GUARD = new Preferences(0.5D, true, 1.0D);
    }

    private final Map<UUID, RoadNode> nodes = new LinkedHashMap<>();
    private final Map<UUID, RoadEdge> edges = new LinkedHashMap<>();
    private final Map<UUID, List<UUID>> adjacency = new HashMap<>();
    private long version;
    private boolean dirty;

    public RoadNode addNode(RoadNode node) {
        nodes.put(node.id(), node);
        adjacency.computeIfAbsent(node.id(), k -> new ArrayList<>());
        dirty = true; version++;
        return node;
    }

    public Optional<RoadEdge> connect(UUID a, UUID b, RoadEdge.Kind kind, double lengthFactor, int bridges, List<UUID> regions) {
        if (a.equals(b) || !nodes.containsKey(a) || !nodes.containsKey(b)) return Optional.empty();
        for (UUID e : adjacency.getOrDefault(a, List.of())) { RoadEdge x = edges.get(e); if (x != null && x.touches(b)) return Optional.of(x); }
        double length = nodes.get(a).distance(nodes.get(b)) * Math.max(1.0D, lengthFactor);
        UUID id = UUID.nameUUIDFromBytes(("road:" + (a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        RoadEdge edge = new RoadEdge(id, a, b, kind, length, bridges, regions);
        edges.put(id, edge);
        adjacency.get(a).add(id);
        adjacency.get(b).add(id);
        dirty = true; version++;
        return Optional.of(edge);
    }

    public void restoreEdge(RoadEdge edge) {
        if (!nodes.containsKey(edge.a()) || !nodes.containsKey(edge.b())) return;
        edges.put(edge.id(), edge);
        adjacency.computeIfAbsent(edge.a(), k -> new ArrayList<>()).add(edge.id());
        adjacency.computeIfAbsent(edge.b(), k -> new ArrayList<>()).add(edge.id());
        version++;
    }

    public boolean block(UUID edgeId, String reason) {
        RoadEdge e = edges.get(edgeId);
        if (e == null || e.blocked()) return false;
        e.block(reason);
        dirty = true; version++;
        return true;
    }

    public boolean reopen(UUID edgeId) {
        RoadEdge e = edges.get(edgeId);
        if (e == null || !e.blocked()) return false;
        e.reopen();
        dirty = true; version++;
        return true;
    }

    public Optional<RoadNode> node(UUID id) { return Optional.ofNullable(nodes.get(id)); }
    public Optional<RoadEdge> edge(UUID id) { return Optional.ofNullable(edges.get(id)); }
    public Collection<RoadNode> nodes() { return List.copyOf(nodes.values()); }
    public Collection<RoadEdge> edges() { return List.copyOf(edges.values()); }
    public List<RoadEdge> edgesOf(UUID node) {
        List<RoadEdge> out = new ArrayList<>();
        for (UUID id : adjacency.getOrDefault(node, List.of())) { RoadEdge e = edges.get(id); if (e != null) out.add(e); }
        return out;
    }
    public List<RoadEdge> crossing(UUID region) { return edges.values().stream().filter(e -> e.regions().contains(region)).toList(); }
    /** Increases every time the graph or a blockage changes: route caches compare it to know when to re-plan. */
    public long version() { return version; }
    public void touch() { version++; dirty = true; }

    /** The nearest node of any kind to a point in a dimension, within {@code maxDistance}. */
    public Optional<RoadNode> nearest(String dimension, double x, double z, double maxDistance, UUID except) {
        RoadNode best = null;
        double bestD = maxDistance;
        for (RoadNode n : nodes.values()) {
            if (!n.dimension().equals(dimension) || n.id().equals(except)) continue;
            double d = Math.hypot(n.x() - x, n.z() - z);
            if (d <= bestD) { bestD = d; best = n; }
        }
        return Optional.ofNullable(best);
    }

    /** The {@code k} nearest settlement nodes to a node, within {@code maxDistance}. */
    public List<RoadNode> nearestSettlements(RoadNode from, int k, double maxDistance) {
        List<RoadNode> all = new ArrayList<>();
        for (RoadNode n : nodes.values())
            if (n.kind() == RoadNode.Kind.SETTLEMENT && !n.id().equals(from.id()) && n.dimension().equals(from.dimension()) && n.distance(from) <= maxDistance) all.add(n);
        all.sort((p, q) -> Double.compare(p.distance(from), q.distance(from)));
        return all.size() > k ? new ArrayList<>(all.subList(0, k)) : all;
    }

    /** Shortest acceptable route, or empty when none exists (disconnected, or every way is blocked or too dangerous). */
    public Optional<RoutePlan> route(UUID from, UUID to, Preferences prefs) {
        if (!nodes.containsKey(from) || !nodes.containsKey(to)) return Optional.empty();
        if (from.equals(to)) return Optional.of(new RoutePlan(List.of(from), List.of(), 0, 0, 0, 0, 0));
        record Q(UUID node, double cost) { }
        Map<UUID, Double> best = new HashMap<>();
        Map<UUID, UUID> viaEdge = new HashMap<>();
        PriorityQueue<Q> open = new PriorityQueue<>((p, q) -> Double.compare(p.cost(), q.cost()));
        best.put(from, 0.0D);
        open.add(new Q(from, 0.0D));
        while (!open.isEmpty()) {
            Q cur = open.poll();
            if (cur.cost() > best.getOrDefault(cur.node(), Double.MAX_VALUE)) continue;
            if (cur.node().equals(to)) break;
            for (UUID eid : adjacency.getOrDefault(cur.node(), List.of())) {
                RoadEdge e = edges.get(eid);
                if (e == null || e.blocked() || e.danger() > prefs.maxDanger() || (!prefs.allowTrails() && e.kind() == RoadEdge.Kind.TRAIL)) continue;
                double cost = cur.cost() + e.length() / Math.max(0.1D, e.speedFactor()) * (1.0D + prefs.dangerWeight() * e.danger());
                UUID next = e.other(cur.node());
                if (cost < best.getOrDefault(next, Double.MAX_VALUE)) { best.put(next, cost); viaEdge.put(next, eid); open.add(new Q(next, cost)); }
            }
        }
        if (!viaEdge.containsKey(to)) return Optional.empty();
        List<UUID> pathNodes = new ArrayList<>(), pathEdges = new ArrayList<>();
        UUID at = to;
        pathNodes.add(at);
        while (!at.equals(from)) {
            UUID eid = viaEdge.get(at);
            pathEdges.add(0, eid);
            at = edges.get(eid).other(at);
            pathNodes.add(0, at);
        }
        double length = 0, maxDanger = 0, dangerSum = 0, effective = 0;
        int bridges = 0;
        for (UUID eid : pathEdges) {
            RoadEdge e = edges.get(eid);
            length += e.length(); maxDanger = Math.max(maxDanger, e.danger()); dangerSum += e.danger() * e.length(); bridges += e.bridges();
            effective += e.length() / Math.max(0.1D, e.speedFactor());
        }
        return Optional.of(new RoutePlan(pathNodes, pathEdges, length, maxDanger, length <= 0 ? 0 : dangerSum / length, bridges, effective));
    }

    public int nodeCount() { return nodes.size(); }
    public int edgeCount() { return edges.size(); }
    public long blockedCount() { return edges.values().stream().filter(RoadEdge::blocked).count(); }
    public boolean dirty() { return dirty; }
    public void clean() { dirty = false; }
    public void clear() { nodes.clear(); edges.clear(); adjacency.clear(); dirty = false; version++; }
}
