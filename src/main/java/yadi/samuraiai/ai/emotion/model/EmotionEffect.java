package yadi.samuraiai.ai.emotion.model;

import yadi.samuraiai.ai.cognition.model.EmotionKind;

/** One emotion a trigger produces and how strongly (0-100 at full importance). */
public record EmotionEffect(EmotionKind kind, double intensity) {
    public EmotionEffect { intensity = Double.isFinite(intensity) ? Math.max(0.0D, Math.min(100.0D, intensity)) : 0.0D; }
}
