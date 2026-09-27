package yadi.samuraiai.ai.emotion.personality;

import java.util.List;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.PersonalityView;

/** Applies the configurable personality rules: who is quick to fear, slow to anger, long to be ashamed. No personality is hard-coded here; the rules are data. */
public final class PersonalityEmotionModel {
    private List<String> source = List.of();
    private List<EmotionRule> rules = List.of();

    public synchronized List<EmotionRule> rules(List<String> lines) {
        if (lines != source) { rules = EmotionRule.parse(lines); source = lines; }
        return rules;
    }

    /** How much more or less strongly this NPC reacts to a trigger of this emotion. */
    public double gain(List<EmotionRule> rules, EmotionKind kind, PersonalityView p) { return factor(rules, kind, p, 'G'); }

    /** How much faster (above 1) or slower (below 1) this NPC lets go of this emotion. */
    public double decay(List<EmotionRule> rules, EmotionKind kind, PersonalityView p) { return factor(rules, kind, p, 'D'); }

    private static double factor(List<EmotionRule> rules, EmotionKind kind, PersonalityView p, char type) {
        double m = 1.0D;
        for (EmotionRule r : rules) if (r.emotion() == kind && r.kind() == type) m *= Math.max(0.1D, 1.0D + r.factor() * p.lean(r.trait()));
        return m;
    }
}
