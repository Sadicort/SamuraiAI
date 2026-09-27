package yadi.samuraiai.emotion;

import java.util.Locale;

/**
 * A short-lived feeling an NPC can hold, each with the resting level it
 * drifts back to. Keeping the baseline on the enum means adding an emotion
 * is a one-line data change rather than an edit to {@link EmotionState}.
 */
public enum Emotion {

    HAPPINESS(0, "contento"),
    SADNESS(0, "triste"),
    FEAR(0, "asustado"),
    ANGER(0, "furioso"),
    ANXIETY(0, "inquieto"),
    TRUST(0, "confiado"),
    SHAME(0, "avergonzado"),
    SURPRISE(0, "sorprendido"),
    CALM(70, "sereno"),
    FRUSTRATION(0, "frustrado");

    private final int baseline;

    /** Word the prompt uses, so the model reads feelings rather than enum names. */
    private final String spanishAdjective;

    Emotion(int baseline, String spanishAdjective) {
        this.baseline = baseline;
        this.spanishAdjective = spanishAdjective;
    }

    public int baseline() {
        return baseline;
    }

    public String adjective() {
        return spanishAdjective;
    }

    public String lowerName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
