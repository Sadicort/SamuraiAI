package yadi.samuraiai.living.village.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * The Village Layout Graph: the plaza, gates, wells, junctions and buildings of a village as nodes, and the streets between
 * them as edges. Every building hangs from the nearest hub (plaza, junction or gate), so there is always a way through the
 * village; {@link #path} answers how one gets from one building to another along the streets.
 */
public final class VillageLayout {
    public enum NodeKind { PLAZA, GATE, WELL, JUNCTION, BUILDING }

    public record Node(UUID id, NodeKind kind, double x, double z, UUID building) { }
    public record Street(UUID a, UUID b, double length) { }

    private final Map<UUID, Node> nodes = new LinkedHashMap<>();
    private final List<Street> streets = new ArrayList<>();
    private final Map<UUID, List<Street>> adjacency = new HashMap<>();

    public Node add(Node node) { nodes.put(node.id(), node); adjacency.computeIfAbsent(node.id(), k -> new ArrayList<>()); return node; }

    public void street(UUID a, UUID b) {
        if (a.equals(b) || !nodes.containsKey(a) || !nodes.containsKey(b)) return;
        for (Street s : adjacency.get(a)) if (s.a().equals(b) || s.b().equals(b)) return;
        Node na = nodes.get(a), nb = nodes.get(b);
        Street s = new Street(a, b, Math.hypot(na.x() - nb.x(), na.z() - nb.z()));
        streets.add(s);
        adjacency.get(a).add(s);
        adjacency.get(b).add(s);
    }

    /** Adds a building and connects it to the nearest hub node (plaza, junction, gate or well). */
    public Node addBuilding(UUID building, double x, double z) {
        UUID id = UUID.nameUUIDFromBytes(("layout:" + building).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Node node = nodes.get(id);
        if (node == null) node = add(new Node(id, NodeKind.BUILDING, x, z, building));
        Node hub = null;
        double best = Double.MAX_VALUE;
        for (Node n : nodes.values()) {
            if (n.kind() == NodeKind.BUILDING) continue;
            double d = Math.hypot(n.x() - x, n.z() - z);
            if (d < best) { best = d; hub = n; }
        }
        if (hub != null) street(id, hub.id());
        return node;
    }

    public void removeBuilding(UUID building) {
        UUID id = UUID.nameUUIDFromBytes(("layout:" + building).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (nodes.remove(id) == null) return;
        streets.removeIf(s -> s.a().equals(id) || s.b().equals(id));
        adjacency.remove(id);
        for (List<Street> l : adjacency.values()) l.removeIf(s -> s.a().equals(id) || s.b().equals(id));
    }

    public Collection<Node> nodes() { return List.copyOf(nodes.values()); }
    public List<Street> streets() { return List.copyOf(streets); }
    public Node plaza() { for (Node n : nodes.values()) if (n.kind() == NodeKind.PLAZA) return n; return null; }

    /** Street path between two buildings (node ids in order), empty when not connected. */
    public List<UUID> path(UUID fromBuilding, UUID toBuilding) {
        UUID from = UUID.nameUUIDFromBytes(("layout:" + fromBuilding).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        UUID to = UUID.nameUUIDFromBytes(("layout:" + toBuilding).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (!nodes.containsKey(from) || !nodes.containsKey(to)) return List.of();
        record Q(UUID n, double c) { }
        Map<UUID, Double> best = new HashMap<>();
        Map<UUID, UUID> prev = new HashMap<>();
        PriorityQueue<Q> open = new PriorityQueue<>((p, q) -> Double.compare(p.c(), q.c()));
        best.put(from, 0.0D);
        open.add(new Q(from, 0));
        while (!open.isEmpty()) {
            Q cur = open.poll();
            if (cur.n().equals(to)) break;
            if (cur.c() > best.getOrDefault(cur.n(), Double.MAX_VALUE)) continue;
            for (Street s : adjacency.getOrDefault(cur.n(), List.of())) {
                UUID next = s.a().equals(cur.n()) ? s.b() : s.a();
                double c = cur.c() + s.length();
                if (c < best.getOrDefault(next, Double.MAX_VALUE)) { best.put(next, c); prev.put(next, cur.n()); open.add(new Q(next, c)); }
            }
        }
        if (!from.equals(to) && !prev.containsKey(to)) return List.of();
        List<UUID> path = new ArrayList<>();
        for (UUID at = to; at != null; at = prev.get(at)) path.add(0, at);
        return path;
    }

    public void clear() { nodes.clear(); streets.clear(); adjacency.clear(); }
}
