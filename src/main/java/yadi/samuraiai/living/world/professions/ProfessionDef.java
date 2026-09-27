package yadi.samuraiai.living.world.professions;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A profession as the world defines it: its tasks, the tools it wears out (resource ids), the kinds of building it works in,
 * the routine that counts as its work and how it biases the day ({@code bias}: routine name → points), which NPC types take it
 * up by default, and how fast experience grows. What it <i>produces</i> is the Economy Engine's recipe for it; which citizen
 * has it is the Village Engine's. {@code future} marks professions prepared but not yet producing (healer).
 */
public record ProfessionDef(String id, String name, List<String> tasks, List<String> tools, Set<String> locations, String workRoutine, Map<String, Double> bias,
                            Set<String> npcTypes, double experiencePerHour, boolean future, String lifestyle) {
    public ProfessionDef {
        tasks = List.copyOf(tasks); tools = List.copyOf(tools); locations = Set.copyOf(locations); bias = Map.copyOf(bias); npcTypes = Set.copyOf(npcTypes);
        workRoutine = workRoutine == null || workRoutine.isBlank() ? "WORK" : workRoutine;
        experiencePerHour = Math.max(0.0D, experiencePerHour);
        lifestyle = lifestyle == null ? "" : lifestyle;
    }

    public static String rank(double hours) { return yadi.samuraiai.living.core.Skill.rank(hours); }

    public static double skill(double hours) { return yadi.samuraiai.living.core.Skill.multiplier(hours); }
}
