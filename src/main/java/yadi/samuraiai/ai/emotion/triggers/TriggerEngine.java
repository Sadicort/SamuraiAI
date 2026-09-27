package yadi.samuraiai.ai.emotion.triggers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.PersonalityView;
import yadi.samuraiai.ai.emotion.engine.EmotionSettings;
import yadi.samuraiai.ai.emotion.model.EmotionEffect;
import yadi.samuraiai.ai.emotion.model.EmotionTrigger;
import yadi.samuraiai.ai.emotion.personality.EmotionRule;
import yadi.samuraiai.ai.emotion.personality.PersonalityEmotionModel;

/** Turns a trigger into the initial emotions it causes: each effect is scaled by how much the event mattered, by who the NPC is (configurable rules) and, for unpleasant ones, by its resilience. */
public final class TriggerEngine {
    public record Initial(EmotionKind kind, double intensity, String originKey) { }

    private final PersonalityEmotionModel model;

    public TriggerEngine(PersonalityEmotionModel model) { this.model = model; }

    public List<Initial> evaluate(EmotionTrigger t, PersonalityView personality, List<EmotionRule> rules, double resilience, EmotionSettings s) {
        List<Initial> result = new ArrayList<>();
        double scale = 0.5D + 0.5D * t.weight();
        String origin = originKey(t);
        for (EmotionEffect effect : t.effects()) {
            if (effect.intensity() <= 0) continue;
            double gain = model.gain(rules, effect.kind(), personality);
            double resilienceFactor = effect.kind().unpleasant() ? 1.0D / Math.max(0.5D, resilience) : 1.0D;
            double intensity = Math.min(100.0D, effect.intensity() * scale * gain * resilienceFactor);
            if (intensity < s.minIntensityKeep()) continue;
            result.add(new Initial(effect.kind(), intensity, effect.kind() + "|" + origin));
        }
        return result;
    }

    public static String originKey(EmotionTrigger t) {
        UUID memory = t.memoryId();
        if (memory != null) return memory.toString();
        if (!t.ref().isEmpty()) return t.ref();
        return t.source().name();
    }
}
