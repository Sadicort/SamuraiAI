package yadi.samuraiai.living.quest.objectives;

import java.util.UUID;
import yadi.samuraiai.living.quest.branching.Path;

/**
 * A live objective of a quest: what to do, about what or whom ({@code target}: an NPC id, a resource, an entity type, a world
 * event or caravan id), where ({@code dimension, x, y, z, radius}; NaN when anywhere), how much is required and done, and its
 * state. Objectives of a branch the player did not take are skipped.
 */
public final class QuestObjective {
    public enum State { PENDING, ACTIVE, DONE, FAILED, SKIPPED }

    private final UUID id;
    private final ObjectiveType type;
    private String description;
    private final String target;
    private final String dimension;
    private final double x, y, z, radius;
    private double required, progress;
    private State state = State.PENDING;
    private final boolean optional;
    private final Path branch;

    public QuestObjective(UUID id, ObjectiveType type, String description, String target, String dimension, double x, double y, double z, double radius, double required, boolean optional, Path branch) {
        this.id = id; this.type = type; this.description = description; this.target = target == null ? "" : target; this.dimension = dimension == null ? "" : dimension;
        this.x = x; this.y = y; this.z = z; this.radius = radius; this.required = Math.max(1e-9, required); this.optional = optional; this.branch = branch;
    }

    public UUID id() { return id; }
    public ObjectiveType type() { return type; }
    public String description() { return description; }
    public void description(String d) { description = d; }
    public String target() { return target; }
    public String dimension() { return dimension; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public double radius() { return radius; }
    public boolean placed() { return !Double.isNaN(x) && !dimension.isEmpty(); }
    public boolean near(String dim, double px, double py, double pz) { return placed() && dimension.equals(dim) && Math.hypot(px - x, pz - z) <= radius && Math.abs(py - y) <= Math.max(16, radius); }
    public double required() { return required; }
    public void required(double r) { required = Math.max(1e-9, r); }
    public double progress() { return progress; }
    public State state() { return state; }
    public void state(State s) { state = s; }
    public boolean optional() { return optional; }
    public Path branch() { return branch; }
    public boolean done() { return state == State.DONE; }
    public boolean active() { return state == State.ACTIVE; }

    /** Adds progress; returns true when this completes the objective. */
    public boolean advance(double amount) {
        if (state != State.ACTIVE || amount <= 0) return false;
        progress = Math.min(required, progress + amount);
        if (progress >= required - 1e-9) { state = State.DONE; return true; }
        return false;
    }

    public double remaining() { return Math.max(0, required - progress); }
    public void restore(double prog, State s) { progress = prog; state = s; }
}
