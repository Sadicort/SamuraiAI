package yadi.samuraiai.ai.cognition.model;

import java.util.Locale;
import java.util.Optional;

/** The personality traits the cognitive engines read (each 0-100). The first ten mirror the behavior scheduler's own traits. */
public enum Trait {
    COURAGE, DISCIPLINE, SOCIABILITY, CURIOSITY, AGGRESSION, CAUTION, LOYALTY, PATIENCE, DILIGENCE, SPIRITUALITY, PRIDE, EMPATHY;

    public static Optional<Trait> parse(String name) {
        if (name == null) return Optional.empty();
        try { return Optional.of(valueOf(name.trim().toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
}
