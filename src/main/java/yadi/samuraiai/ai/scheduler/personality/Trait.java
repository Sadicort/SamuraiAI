package yadi.samuraiai.ai.scheduler.personality;

import java.util.Locale;
import java.util.Optional;

/** The ten personality traits, each measured 0-100. */
public enum Trait {
    COURAGE, DISCIPLINE, SOCIABILITY, CURIOSITY, AGGRESSION, CAUTION, LOYALTY, PATIENCE, DILIGENCE, SPIRITUALITY;

    public static Optional<Trait> parse(String name) {
        if (name == null) return Optional.empty();
        try { return Optional.of(valueOf(name.trim().toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
}
