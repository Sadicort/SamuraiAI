package yadi.samuraiai.living.quest.branching;

import java.util.Locale;
import java.util.Optional;

/** The ways a quest can be resolved. Each has its own consequences; a quest offers the paths its template allows. */
public enum Path {
    PEACEFUL("pacífica"), VIOLENT("violenta"), STEALTH("sigilosa"), DIPLOMATIC("diplomática"), SPIRITUAL("espiritual"), HONOR("honorable");

    private final String label;
    Path(String label) { this.label = label; }
    public String label() { return label; }

    public static Optional<Path> parse(String text) {
        if (text == null) return Optional.empty();
        try { return Optional.of(valueOf(text.trim().toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
}
