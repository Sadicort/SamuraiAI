package yadi.samuraiai.ai.emotion.model;

import yadi.samuraiai.ai.cognition.model.EmotionKind;

/** The current emotional mixture: the dominant emotion, the one that colours it (if strong enough), a label, and the mixture's valence and arousal. */
public record Blend(EmotionKind dominant, EmotionKind secondary, String label, double intensity, double valence, double arousal) {
    public static final Blend CALM = new Blend(EmotionKind.CALM, null, "CALM", 0.0D, 0.0D, 0.1D);
    public boolean mixed() { return secondary != null; }
}
