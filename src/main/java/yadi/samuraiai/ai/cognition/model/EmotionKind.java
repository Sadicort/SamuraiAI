package yadi.samuraiai.ai.cognition.model;

import java.util.Locale;
import java.util.Optional;

/**
 * The emotion categories (extensible: add a constant and its valence/arousal). Valence is -1 (unpleasant) to +1 (pleasant),
 * arousal 0 (subdued) to 1 (activated). Shared by memory (emotional signatures) and the emotion engine.
 */
public enum EmotionKind {
    JOY(0.9, 0.6), CALM(0.6, 0.1), CURIOSITY(0.4, 0.6), HOPE(0.6, 0.4), PRIDE(0.7, 0.5), GRATITUDE(0.8, 0.4),
    SADNESS(-0.7, 0.2), FEAR(-0.8, 0.9), ANGER(-0.7, 0.9), SHAME(-0.6, 0.4), GUILT(-0.6, 0.4), ANXIETY(-0.5, 0.7),
    COMPASSION(0.5, 0.3), DETERMINATION(0.3, 0.7), RESPECT(0.5, 0.3), DISTRUST(-0.5, 0.5), LONELINESS(-0.5, 0.2),
    INSPIRATION(0.8, 0.6), EMOTIONAL_FATIGUE(-0.3, 0.05), SURPRISE(0.0, 0.8);

    private final double valence, arousal;
    EmotionKind(double valence, double arousal) { this.valence = valence; this.arousal = arousal; }
    public double valence() { return valence; }
    public double arousal() { return arousal; }
    public boolean unpleasant() { return valence < 0; }

    public static Optional<EmotionKind> parse(String name) {
        if (name == null) return Optional.empty();
        try { return Optional.of(valueOf(name.trim().toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
}
