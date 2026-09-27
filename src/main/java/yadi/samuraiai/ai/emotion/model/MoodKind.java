package yadi.samuraiai.ai.emotion.model;

/** The dominant, persistent emotional state (hours or days of game time), as opposed to a single emotion. */
public enum MoodKind {
    PEACEFUL(0.5), FOCUSED(0.2), HAPPY(0.8), MELANCHOLIC(-0.6), ANGRY(-0.6), FEARFUL(-0.7), HOPEFUL(0.5), INSPIRED(0.9), EXHAUSTED(-0.3), ALERT(-0.2), NEUTRAL(0.0);

    private final double valence;
    MoodKind(double valence) { this.valence = valence; }
    public double valence() { return valence; }
}
