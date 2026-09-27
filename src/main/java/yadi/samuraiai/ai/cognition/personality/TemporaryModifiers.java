package yadi.samuraiai.ai.cognition.personality;

import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.Trait;

/**
 * How present emotions mask traits, in trait points: fear dims courage, anger shortens patience, sadness withdraws sociability,
 * calm steadies discipline. These are temporary: they follow the emotions and vanish with them, and they never touch the stored personality.
 */
public final class TemporaryModifiers {
    private TemporaryModifiers() { }

    /** @param intensity supplies the current intensity (0-100) of an emotion */
    public static double points(Trait trait, java.util.function.Function<EmotionKind, Double> intensity, double cap) {
        double p = switch (trait) {
            case COURAGE -> -0.10D * intensity.apply(EmotionKind.FEAR) - 0.04D * intensity.apply(EmotionKind.ANXIETY) + 0.05D * intensity.apply(EmotionKind.DETERMINATION);
            case PATIENCE -> -0.10D * intensity.apply(EmotionKind.ANGER) + 0.04D * intensity.apply(EmotionKind.CALM);
            case SOCIABILITY -> -0.08D * intensity.apply(EmotionKind.SADNESS) + 0.05D * intensity.apply(EmotionKind.JOY);
            case DISCIPLINE -> 0.03D * intensity.apply(EmotionKind.CALM) - 0.05D * intensity.apply(EmotionKind.EMOTIONAL_FATIGUE);
            case CAUTION -> 0.06D * intensity.apply(EmotionKind.ANXIETY) + 0.04D * intensity.apply(EmotionKind.FEAR);
            case CURIOSITY -> 0.05D * intensity.apply(EmotionKind.INSPIRATION) - 0.04D * intensity.apply(EmotionKind.FEAR);
            case AGGRESSION -> 0.06D * intensity.apply(EmotionKind.ANGER) - 0.03D * intensity.apply(EmotionKind.COMPASSION);
            default -> 0.0D;
        };
        return Math.max(-cap, Math.min(cap, p));
    }
}
