package yadi.samuraiai.ai.relationship.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.relationship.model.Dimension;

/** "How a personality trait bends an axis": {@code TRAIT|DIMENSION|G/L/B|factor}. The multiplier is {@code 1 + factor * lean(trait)}. */
public record TraitRule(Trait trait, Dimension dimension, char sign, double factor) {
    public static List<TraitRule> parse(List<String> lines) {
        List<TraitRule> rules = new ArrayList<>();
        for (String line : lines) {
            String[] p = line.split("\\|");
            if (p.length != 4) continue;
            try {
                Trait trait = Trait.parse(p[0]).orElse(null);
                if (trait == null) continue;
                Dimension dimension = Dimension.valueOf(p[1].trim().toUpperCase(Locale.ROOT));
                char sign = p[2].trim().toUpperCase(Locale.ROOT).charAt(0);
                if (sign != 'G' && sign != 'L' && sign != 'B') continue;
                rules.add(new TraitRule(trait, dimension, sign, Double.parseDouble(p[3].trim())));
            } catch (RuntimeException ignored) { /* a malformed rule is skipped */ }
        }
        return rules;
    }
}
