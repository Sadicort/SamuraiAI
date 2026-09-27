package yadi.samuraiai.ai.memory.model;

import java.util.List;
import yadi.samuraiai.ai.cognition.model.EmotionKind;

/** The feeling a memory carries: main and secondary emotions, how strong (0-1), how pleasant (-1..1) and whether it left a scar. */
public record EmotionalSignature(EmotionKind primary, List<EmotionKind> secondary, double intensity, double valence, boolean traumatic) {
    public EmotionalSignature {
        primary = primary == null ? EmotionKind.CALM : primary;
        secondary = secondary == null ? List.of() : List.copyOf(secondary);
        intensity = Double.isFinite(intensity) ? Math.max(0.0D, Math.min(1.0D, intensity)) : 0.0D;
        valence = Double.isFinite(valence) ? Math.max(-1.0D, Math.min(1.0D, valence)) : 0.0D;
    }

    public static EmotionalSignature neutral() { return new EmotionalSignature(EmotionKind.CALM, List.of(), 0.0D, 0.0D, false); }
    public static EmotionalSignature of(EmotionKind primary, double intensity) { return new EmotionalSignature(primary, List.of(), intensity, primary.valence(), false); }
    public EmotionalSignature withIntensity(double next) { return new EmotionalSignature(primary, secondary, next, valence, traumatic); }
    public EmotionalSignature withValence(double next) { return new EmotionalSignature(primary, secondary, intensity, next, traumatic); }
    public EmotionalSignature asTraumatic(boolean flag) { return new EmotionalSignature(primary, secondary, intensity, valence, flag); }
}
