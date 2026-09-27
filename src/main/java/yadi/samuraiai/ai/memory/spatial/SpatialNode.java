package yadi.samuraiai.ai.memory.spatial;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.PlaceRef;

/** A place in an NPC's mental map: what it is, where, how familiar, how safe, and which other places it is connected to. */
public final class SpatialNode {
    private final UUID id;
    private String name;
    private final LandmarkKind kind;
    private PlaceRef place;
    private int visits;
    private double familiarity, danger, safety;
    private long lastVisit, firstVisit;
    private final Set<UUID> connections = new LinkedHashSet<>();

    public SpatialNode(UUID id, String name, LandmarkKind kind, PlaceRef place, long now) {
        this.id = id; this.name = name == null ? "" : name; this.kind = kind; this.place = place; this.firstVisit = now; this.lastVisit = now;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public void name(String v) { if (v != null && !v.isEmpty()) name = v; }
    public LandmarkKind kind() { return kind; }
    public PlaceRef place() { return place; }
    public void place(PlaceRef v) { place = v; }
    public int visits() { return visits; }
    public void visits(int v) { visits = Math.max(0, v); }
    public double familiarity() { return familiarity; }
    public void familiarity(double v) { familiarity = Math.max(0, Math.min(1, v)); }
    public double danger() { return danger; }
    public void danger(double v) { danger = Math.max(0, Math.min(1, v)); }
    public double safety() { return safety; }
    public void safety(double v) { safety = Math.max(0, Math.min(1, v)); }
    public long lastVisit() { return lastVisit; }
    public void lastVisit(long v) { lastVisit = v; }
    public long firstVisit() { return firstVisit; }
    public void firstVisit(long v) { firstVisit = v; }
    public Set<UUID> connections() { return connections; }
}
