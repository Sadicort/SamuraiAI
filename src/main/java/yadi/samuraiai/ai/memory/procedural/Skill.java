package yadi.samuraiai.ai.memory.procedural;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.ai.cognition.model.PlaceRef;

/** A practised procedure (a patrol route, the usual resting place): the more it is used the better it is known. */
public final class Skill {
    private final String key;
    private final SkillKind kind;
    private final List<PlaceRef> waypoints = new ArrayList<>();
    private double proficiency;
    private int uses;
    private long lastUsed;
    private boolean protectedSkill;

    public Skill(String key, SkillKind kind) { this.key = key; this.kind = kind; }

    public String key() { return key; }
    public SkillKind kind() { return kind; }
    public List<PlaceRef> waypoints() { return waypoints; }
    public double proficiency() { return proficiency; }
    public void proficiency(double v) { proficiency = Math.max(0, Math.min(1, v)); }
    public int uses() { return uses; }
    public void uses(int v) { uses = Math.max(0, v); }
    public long lastUsed() { return lastUsed; }
    public void lastUsed(long v) { lastUsed = v; }
    public boolean isProtected() { return protectedSkill; }
    public void protect(boolean v) { protectedSkill = v; }
}
