package yadi.samuraiai.ai.emotion.personality;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.Trait;

/** "How a personality trait bends an emotion": {@code TRAIT|EMOTION|G/D|factor}. The multiplier is {@code 1 + factor * lean(trait)}; G applies to how strongly it is triggered, D to how fast it decays. */
public record EmotionRule(Trait trait, EmotionKind emotion, char kind, double factor) {
    public static List<EmotionRule> parse(List<String> lines) {
        List<EmotionRule> rules = new ArrayList<>();
        for (String line : lines) {
            String[] p = line.split("\\|");
            if (p.length != 4) continue;
            try {
                Trait trait = Trait.parse(p[0]).orElse(null);
                EmotionKind emotion = EmotionKind.parse(p[1]).orElse(null);
                char kind = p[2].trim().toUpperCase(Locale.ROOT).charAt(0);
                if (trait == null || emotion == null || (kind != 'G' && kind != 'D')) continue;
                rules.add(new EmotionRule(trait, emotion, kind, Double.parseDouble(p[3].trim())));
            } catch (RuntimeException ignored) { /* skip the malformed rule */ }
        }
        return rules;
    }
}
