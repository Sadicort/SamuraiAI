package yadi.samuraiai.ai.memory.spatial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.PlaceRef;

/**
 * The NPC's persistent mental map: nodes for the places it has been to (home, temple, market, a bridge, a cave...) connected
 * in the order it travelled between them. It records that places are known and how they relate; the navigation engine still
 * owns the physical route between two of them. Landmarks are nodes too, with a visibility that navigation and perception can use.
 */
public final class SpatialMap {
    private final Map<UUID, SpatialNode> nodes = new LinkedHashMap<>();
    private UUID lastVisited;

    public Collection<SpatialNode> nodes() { return List.copyOf(nodes.values()); }
    public Optional<SpatialNode> node(UUID id) { return Optional.ofNullable(nodes.get(id)); }
    public int size() { return nodes.size(); }
    public void put(SpatialNode node) { nodes.put(node.id(), node); }
    public UUID lastVisited() { return lastVisited; }
    public void lastVisited(UUID id) { lastVisited = id; }

    /** The closest node within the radius, optionally of one kind. */
    public Optional<SpatialNode> nearest(PlaceRef place, double radius, LandmarkKind kind) {
        SpatialNode best = null;
        double bestDistance = radius;
        for (SpatialNode n : nodes.values()) {
            if (kind != null && n.kind() != kind) continue;
            double d = n.place().distance(place);
            if (d <= bestDistance) { best = n; bestDistance = d; }
        }
        return Optional.ofNullable(best);
    }

    public List<SpatialNode> byKind(LandmarkKind kind) {
        List<SpatialNode> result = new ArrayList<>();
        for (SpatialNode n : nodes.values()) if (n.kind() == kind) result.add(n);
        return result;
    }

    /** Places of any kind within the radius: what a landmark lookup for navigation or perception would use. */
    public List<SpatialNode> near(PlaceRef place, double radius) {
        List<SpatialNode> result = new ArrayList<>();
        for (SpatialNode n : nodes.values()) if (n.place().distance(place) <= radius) result.add(n);
        result.sort(java.util.Comparator.comparingDouble(n -> n.place().distance(place)));
        return result;
    }

    /**
     * The NPC was at this place. An existing node of the same kind (or the same named zone) within the merge radius is updated;
     * otherwise a node is created, and the previous node it was at within the connect radius is linked to it.
     * @return the node and whether it is new
     */
    public Visit visit(LandmarkKind kind, String name, PlaceRef place, long now, double mergeRadius, double connectRadius, double familiarityGain) {
        SpatialNode node = null;
        if (!place.zone().isEmpty()) for (SpatialNode n : nodes.values()) if (n.kind() == kind && n.place().zone().equals(place.zone())) { node = n; break; }
        if (node == null) node = nearest(place, mergeRadius, kind).orElse(null);
        boolean created = node == null;
        if (created) {
            node = new SpatialNode(UUID.randomUUID(), name, kind, place, now);
            nodes.put(node.id(), node);
        } else node.name(name);
        node.visits(node.visits() + 1);
        node.lastVisit(now);
        node.familiarity(node.familiarity() + familiarityGain * (1.0D - node.familiarity()));
        if (lastVisited != null && !lastVisited.equals(node.id())) {
            SpatialNode previous = nodes.get(lastVisited);
            if (previous != null && previous.place().distance(node.place()) <= connectRadius) { previous.connections().add(node.id()); node.connections().add(previous.id()); }
        }
        lastVisited = node.id();
        return new Visit(node, created);
    }

    public record Visit(SpatialNode node, boolean created) { }

    /** Notes that something good or bad happened at a place: the node's danger and safety follow. */
    public void impress(PlaceRef place, double radius, double danger, double safety) {
        nearest(place, radius, null).ifPresent(n -> {
            n.danger(n.danger() + danger * (1.0D - n.danger()));
            n.safety(n.safety() + safety * (1.0D - n.safety()));
        });
    }

    public void clear() { nodes.clear(); lastVisited = null; }
}
