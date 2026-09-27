package yadi.samuraiai.ai.memory.procedural;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import yadi.samuraiai.ai.cognition.model.PlaceRef;

/** Skills and habits: routes, patrols, safe and resting places. Proficiency grows with use; a well-practised skill becomes protected from forgetting. */
public final class ProceduralMemory {
    private static final int MAX_WAYPOINTS = 16;
    private final Map<String, Skill> skills = new LinkedHashMap<>();

    public Optional<Skill> skill(String key) { return Optional.ofNullable(skills.get(key)); }
    public Collection<Skill> all() { return List.copyOf(skills.values()); }
    public int size() { return skills.size(); }
    public void put(Skill skill) { skills.put(skill.key(), skill); }

    /** The skill was used once more. Waypoints (when given) are remembered up to a small limit. */
    public Skill practice(String key, SkillKind kind, PlaceRef waypoint, long now, double gain, int protectUses) {
        Skill skill = skills.computeIfAbsent(key, k -> new Skill(k, kind));
        skill.uses(skill.uses() + 1);
        skill.lastUsed(now);
        skill.proficiency(skill.proficiency() + gain * (1.0D - skill.proficiency()));
        if (waypoint != null && waypoint.known() && skill.waypoints().size() < MAX_WAYPOINTS
                && skill.waypoints().stream().noneMatch(w -> w.distance(waypoint) < 4.0D)) skill.waypoints().add(waypoint);
        if (skill.uses() >= protectUses) skill.protect(true);
        return skill;
    }

    /** Skills unused for a long time slowly rust, unless protected. */
    public void rust(long now, long idleTicks, double amount) {
        for (Skill s : skills.values()) if (!s.isProtected() && now - s.lastUsed() > idleTicks) s.proficiency(s.proficiency() - amount);
    }

    public void clear() { skills.clear(); }
}
