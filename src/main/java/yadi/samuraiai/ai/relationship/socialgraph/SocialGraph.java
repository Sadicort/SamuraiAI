package yadi.samuraiai.ai.relationship.socialgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityKind;
import yadi.samuraiai.ai.relationship.model.RelationType;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;

/**
 * The world's social graph: a node per NPC, player, faction or place and a directed edge per relationship, kept in both
 * directions so that "who knows X?", "who respects X?" and "who is allied with this faction?" are lookups, not scans. Edges
 * are summaries; the relationship record stays the source of truth. Server thread only.
 */
public final class SocialGraph {
    public record Edge(UUID source, UUID target, EntityKind targetKind, RelationType type, double trust, double respect, double affinity, double fear,
                       double loyalty, double rivalry, double honor, int interactions, long updated) { }

    private final Map<UUID, Map<UUID, Edge>> out = new HashMap<>(), in = new HashMap<>();

    public void update(RelationshipRecord r, long now) {
        Edge edge = new Edge(r.source(), r.target().id(), r.target().kind(), r.type(), r.trust(), r.respect(), r.affinity(), r.fear(), r.loyalty(), r.rivalry(), r.honor(), r.interactions(), now);
        out.computeIfAbsent(r.source(), k -> new HashMap<>()).put(r.target().id(), edge);
        in.computeIfAbsent(r.target().id(), k -> new HashMap<>()).put(r.source(), edge);
    }

    public void remove(UUID source, UUID target) {
        Map<UUID, Edge> from = out.get(source);
        if (from != null) { from.remove(target); if (from.isEmpty()) out.remove(source); }
        Map<UUID, Edge> to = in.get(target);
        if (to != null) { to.remove(source); if (to.isEmpty()) in.remove(target); }
    }

    /** Removes an NPC that left the world for good, as source and as target. */
    public void removeNode(UUID node) {
        for (UUID target : new ArrayList<>(out.getOrDefault(node, Map.of()).keySet())) remove(node, target);
        for (UUID source : new ArrayList<>(in.getOrDefault(node, Map.of()).keySet())) remove(source, node);
    }

    public Optional<Edge> edge(UUID source, UUID target) { return Optional.ofNullable(out.getOrDefault(source, Map.of()).get(target)); }
    public List<Edge> from(UUID source) { return new ArrayList<>(out.getOrDefault(source, Map.of()).values()); }
    public List<Edge> to(UUID target) { return new ArrayList<>(in.getOrDefault(target, Map.of()).values()); }

    /** Who has met this person (knows them at all). */
    public List<UUID> whoKnows(UUID target) {
        List<UUID> result = new ArrayList<>();
        for (Edge e : in.getOrDefault(target, Map.of()).values()) if (e.interactions() > 0) result.add(e.source());
        return result;
    }

    public List<UUID> whoRespects(UUID target, double minimum) {
        List<UUID> result = new ArrayList<>();
        for (Edge e : in.getOrDefault(target, Map.of()).values()) if (e.respect() >= minimum) result.add(e.source());
        return result;
    }

    public List<UUID> whoTrusts(UUID target, double minimum) {
        List<UUID> result = new ArrayList<>();
        for (Edge e : in.getOrDefault(target, Map.of()).values()) if (e.trust() >= minimum) result.add(e.source());
        return result;
    }

    public List<UUID> whoFears(UUID target, double minimum) {
        List<UUID> result = new ArrayList<>();
        for (Edge e : in.getOrDefault(target, Map.of()).values()) if (e.fear() >= minimum) result.add(e.source());
        return result;
    }

    /** NPCs whose bond with the faction is at least the given score (allied, with the default threshold of the caller). */
    public List<UUID> alliedTo(UUID faction, double minimumScore) {
        List<UUID> result = new ArrayList<>();
        for (Edge e : in.getOrDefault(faction, Map.of()).values()) {
            double score = Math.max(0.0D, e.trust() * 0.5D + e.affinity() * 0.3D + e.loyalty() * 0.2D - e.fear() * 0.2D - e.rivalry() * 0.6D);
            if (score >= minimumScore) result.add(e.source());
        }
        return result;
    }

    public List<UUID> rivalsOf(UUID source, double minimum) {
        List<UUID> result = new ArrayList<>();
        for (Edge e : out.getOrDefault(source, Map.of()).values()) if (e.rivalry() >= minimum) result.add(e.target());
        return result;
    }

    public int edges() { int n = 0; for (Map<UUID, Edge> m : out.values()) n += m.size(); return n; }
    public int nodes() { java.util.Set<UUID> nodes = new java.util.HashSet<>(out.keySet()); nodes.addAll(in.keySet()); return nodes.size(); }
    public void clear() { out.clear(); in.clear(); }
}
