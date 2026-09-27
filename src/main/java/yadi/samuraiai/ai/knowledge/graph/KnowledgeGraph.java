package yadi.samuraiai.ai.knowledge.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeRuntime;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.Predicate;

/**
 * A graph view over a holder's beliefs: the subjects and objects of its records are the nodes, each record with an object is an
 * edge labelled by its predicate. It reads the runtime's indexes, so a query starts from the node it names and never scans the
 * whole knowledge base. The graph stores nothing itself.
 */
public final class KnowledgeGraph {
    public record Edge(KnowledgeRecord record, UUID from, UUID to, Predicate predicate) { }

    private final KnowledgeRuntime rt;
    private final int queryLimit;

    public KnowledgeGraph(KnowledgeRuntime rt, int queryLimit) { this.rt = rt; this.queryLimit = queryLimit; }

    /** Edges leaving the node, optionally of one predicate. */
    public List<Edge> outgoing(UUID node, Predicate predicate) {
        List<Edge> edges = new ArrayList<>();
        for (UUID id : rt.aboutSubject(node)) {
            KnowledgeRecord r = rt.get(id);
            if (r == null || r.object() == null || !r.alive() || (predicate != null && r.predicate() != predicate)) continue;
            edges.add(new Edge(r, node, r.object().id(), r.predicate()));
            if (edges.size() >= queryLimit) break;
        }
        return edges;
    }

    /** Edges arriving at the node, optionally of one predicate. */
    public List<Edge> incoming(UUID node, Predicate predicate) {
        List<Edge> edges = new ArrayList<>();
        for (UUID id : rt.aboutObject(node)) {
            KnowledgeRecord r = rt.get(id);
            if (r == null || !r.alive() || (predicate != null && r.predicate() != predicate)) continue;
            edges.add(new Edge(r, r.subject().id(), node, r.predicate()));
            if (edges.size() >= queryLimit) break;
        }
        return edges;
    }

    /** Nodes reachable in one step (either direction) by the predicate. */
    public Set<UUID> neighbors(UUID node, Predicate predicate) {
        Set<UUID> result = new HashSet<>();
        for (Edge e : outgoing(node, predicate)) result.add(e.to());
        for (Edge e : incoming(node, predicate)) result.add(e.from());
        return result;
    }

    /** Graph query: records matching any combination of subject, predicate and object (null = anything). */
    public List<KnowledgeRecord> query(UUID subject, Predicate predicate, UUID object) {
        List<KnowledgeRecord> result = new ArrayList<>();
        java.util.Collection<UUID> candidates = subject != null ? rt.aboutSubject(subject) : object != null ? rt.aboutObject(object) : predicate != null ? rt.ofPredicate(predicate) : rt.ids();
        for (UUID id : candidates) {
            KnowledgeRecord r = rt.get(id);
            if (r == null || !r.alive()) continue;
            if (predicate != null && r.predicate() != predicate) continue;
            if (object != null && (r.object() == null || !r.object().id().equals(object))) continue;
            if (subject != null && !r.subject().id().equals(subject)) continue;
            result.add(r);
            if (result.size() >= queryLimit) break;
        }
        return result;
    }

    /** Shortest chain of edges between two nodes (breadth first, bounded depth), or empty. For example: which places connect the house to the temple. */
    public List<Edge> path(UUID from, UUID to, int maxDepth) {
        if (from.equals(to)) return List.of();
        Map<UUID, Edge> cameBy = new HashMap<>();
        Set<UUID> seen = new HashSet<>();
        Deque<UUID> frontier = new ArrayDeque<>();
        Deque<Integer> depths = new ArrayDeque<>();
        frontier.add(from); depths.add(0); seen.add(from);
        while (!frontier.isEmpty()) {
            UUID node = frontier.poll();
            int depth = depths.poll();
            if (depth >= maxDepth) continue;
            List<Edge> edges = new ArrayList<>(outgoing(node, null));
            edges.addAll(incoming(node, null));
            for (Edge e : edges) {
                UUID next = e.from().equals(node) ? e.to() : e.from();
                if (!seen.add(next)) continue;
                cameBy.put(next, e);
                if (next.equals(to)) {
                    List<Edge> path = new ArrayList<>();
                    UUID at = to;
                    while (!at.equals(from)) { Edge step = cameBy.get(at); path.add(0, step); at = step.from().equals(at) ? step.to() : step.from(); }
                    return path;
                }
                frontier.add(next); depths.add(depth + 1);
            }
        }
        return List.of();
    }

    public int nodeCount() { return rt.subjects(); }
    public int edgeCount() { int n = 0; for (UUID id : rt.ids()) { KnowledgeRecord r = rt.get(id); if (r != null && r.object() != null && r.alive()) n++; } return n; }
}
