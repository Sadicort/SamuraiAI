package yadi.samuraiai.living.family.succession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.scheduler.util.Segments;

/**
 * The succession pipeline: HEAD UNAVAILABLE → SUCCESSION RULES → CANDIDATES → EVALUATION → SUCCESSOR → EVENT → FAMILY UPDATE.
 * Rules are per culture and weigh age, seniority of generation, personal reputation, honour, the family's trade, knowledge
 * held, an explicit designation and closeness to the previous head. There is no universal patriarchal or matriarchal rule:
 * gender is not an input. Lines look like {@code temple;age=0.5;generation=0.5;reputation=0.5;honor=0.5;profession=0.2;knowledge=1.5;designation=3;tradition=0.5}.
 */
public final class SuccessionEngine {
    /** What is known about one candidate, each value normalised to 0..1 by the caller. */
    public record Candidate(UUID person, String name, double age, double seniority, double reputation, double honor, double profession, double knowledge,
                            boolean designated, boolean closeToHead) { }
    public record Ranked(UUID person, String name, double score, List<String> reasons) { }
    public record Rules(double age, double generation, double reputation, double honor, double profession, double knowledge, double designation, double tradition) { }

    public static final List<String> DEFAULT_LINES = List.of(
            "village;age=1.0;generation=1.0;reputation=0.5;honor=0.3;profession=0.5;knowledge=0.4;designation=3.0;tradition=0.8",
            "temple;age=0.5;generation=0.5;reputation=0.5;honor=0.5;profession=0.2;knowledge=1.5;designation=3.0;tradition=0.5",
            "guard;age=0.5;generation=0.5;reputation=0.6;honor=1.5;profession=0.6;knowledge=0.6;designation=3.0;tradition=0.5",
            "market;age=0.6;generation=0.6;reputation=1.2;honor=0.3;profession=1.0;knowledge=0.4;designation=3.0;tradition=0.6");

    private final Map<String, Rules> rules = new LinkedHashMap<>();

    public SuccessionEngine(List<String> lines) {
        for (String l : DEFAULT_LINES) parse(l);
        if (lines != null) for (String l : lines) parse(l);
    }

    private void parse(String line) {
        Segments.parse(line).ifPresent(s -> rules.put(s.id().toLowerCase(Locale.ROOT), new Rules(s.number("age", 1), s.number("generation", 1), s.number("reputation", 0.5),
                s.number("honor", 0.3), s.number("profession", 0.5), s.number("knowledge", 0.4), s.number("designation", 3), s.number("tradition", 0.8))));
    }

    public Rules rules(String culture) { Rules r = rules.get(culture == null ? "" : culture.toLowerCase(Locale.ROOT)); return r != null ? r : rules.get("village"); }

    public List<Ranked> rank(List<Candidate> candidates, String culture) {
        Rules r = rules(culture);
        List<Ranked> out = new ArrayList<>();
        for (Candidate c : candidates) {
            List<String> why = new ArrayList<>();
            double score = 0;
            score += add(why, "edad", r.age() * c.age());
            score += add(why, "generación mayor", r.generation() * c.seniority());
            score += add(why, "reputación", r.reputation() * c.reputation());
            score += add(why, "honor", r.honor() * c.honor());
            score += add(why, "oficio de la familia", r.profession() * c.profession());
            score += add(why, "conocimiento", r.knowledge() * c.knowledge());
            if (c.designated()) score += add(why, "designado", r.designation());
            if (c.closeToHead()) score += add(why, "cercanía al jefe anterior", r.tradition());
            out.add(new Ranked(c.person(), c.name(), score, why));
        }
        out.sort((a, b) -> Double.compare(b.score(), a.score()));
        return out;
    }

    private static double add(List<String> why, String label, double v) { if (v > 0.01) why.add(String.format("%s %.2f", label, v)); return v; }
}
