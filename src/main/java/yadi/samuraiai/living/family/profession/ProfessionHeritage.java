package yadi.samuraiai.living.family.profession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which trade a young person is drawn to. A parent's profession increases <b>exposure</b> (a smith's child grows up at the
 * forge), and so do the household, a mentor and the family's tradition; but the choice also weighs the person's own
 * personality, the village's needs and the economy. The result is a ranked list with reasons — the profession is never
 * copied automatically.
 */
public final class ProfessionHeritage {
    public record Suggestion(String profession, double score, List<String> reasons) { }

    /** Inputs for one person, all per profession. */
    public record Inputs(Map<String, Double> exposure, Map<String, Double> affinity, List<String> villageNeeds, List<String> familyTraditions, String mentorProfession) { }

    public List<Suggestion> suggest(Inputs in) {
        Map<String, Double> score = new LinkedHashMap<>();
        Map<String, List<String>> why = new LinkedHashMap<>();
        in.exposure().forEach((p, v) -> bump(score, why, p, 0.8 * Math.min(1, v), String.format("creció viéndolo (%.2f)", v)));
        in.affinity().forEach((p, v) -> bump(score, why, p, 0.6 * v, String.format("su carácter (%.2f)", v)));
        for (String p : in.villageNeeds()) bump(score, why, p, 0.4, "la aldea lo necesita");
        for (String t : in.familyTraditions()) if (t.startsWith("oficio:")) bump(score, why, t.substring(7), 0.3, "tradición familiar");
        if (in.mentorProfession() != null && !in.mentorProfession().isEmpty()) bump(score, why, in.mentorProfession(), 0.5, "su maestro");
        List<Suggestion> out = new ArrayList<>();
        score.forEach((p, s) -> out.add(new Suggestion(p, s, why.get(p))));
        out.sort((a, b) -> Double.compare(b.score(), a.score()));
        return out;
    }

    private static void bump(Map<String, Double> score, Map<String, List<String>> why, String p, double v, String reason) {
        if (p == null || p.isEmpty() || v <= 0) return;
        score.merge(p, v, Double::sum);
        why.computeIfAbsent(p, k -> new ArrayList<>()).add(reason);
    }
}
