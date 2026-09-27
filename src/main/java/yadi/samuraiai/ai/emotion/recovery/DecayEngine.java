package yadi.samuraiai.ai.emotion.recovery;

import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.emotion.engine.EmotionSettings;
import yadi.samuraiai.ai.emotion.model.DecayCurve;
import yadi.samuraiai.ai.emotion.model.EmotionRecord;

/**
 * Fading of one emotion over elapsed time: linear, exponential, trauma (very slow, with a residue that shrinks only as the
 * trauma recovers), hope (slow and renewable) or a custom half-life. The speed multiplier carries personality, technique and
 * activity. Pleasant emotions and unpleasant ones fade at their configured scales.
 */
public final class DecayEngine {
    public void decay(EmotionRecord r, long elapsed, double speed, double traumaRemaining, EmotionSettings s) {
        if (elapsed <= 0) return;
        double scale = r.kind().unpleasant() ? s.negativeDecayScale() : s.positiveDecayScale();
        double factor = Math.max(0.01D, speed * scale) / (0.5D + r.persistence());
        double before = r.intensity();
        double after;
        switch (r.curve()) {
            case LINEAR -> after = before - s.linearPerTick() * elapsed * factor;
            case TRAUMA -> {
                double floor = r.peak() * s.traumaFloorFraction() * traumaRemaining;
                after = before <= floor ? before : floor + (before - floor) * Math.pow(0.5D, elapsed * factor / s.traumaHalfLife());
            }
            case HOPE -> after = before * Math.pow(0.5D, elapsed * factor / s.hopeHalfLife());
            case CUSTOM -> after = before * Math.pow(0.5D, elapsed * factor / Math.max(20.0D, r.customHalfLife()));
            default -> after = before * Math.pow(0.5D, elapsed * factor / s.exponentialHalfLife());
        }
        r.intensity(Math.max(0.0D, after));
    }

    public static DecayCurve curveFor(EmotionKind kind, boolean traumatic) {
        if (traumatic) return DecayCurve.TRAUMA;
        return kind == EmotionKind.HOPE ? DecayCurve.HOPE : DecayCurve.EXPONENTIAL;
    }
}
