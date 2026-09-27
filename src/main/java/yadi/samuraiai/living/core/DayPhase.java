package yadi.samuraiai.living.core;

import java.util.Locale;
import java.util.Optional;

/**
 * The eight phases of a Deiliora day, derived from the official calendar hour (never from the vanilla clock). Where each
 * one begins is configuration; {@link #dark()} says whether ordinary people are expected indoors.
 */
public enum DayPhase {
    LATE_NIGHT(true), DAWN(false), MORNING(false), NOON(false), AFTERNOON(false), SUNSET(false), NIGHT(true), MIDNIGHT(true);

    private final boolean dark;
    DayPhase(boolean dark) { this.dark = dark; }
    public boolean dark() { return dark; }

    /** The Spanish name players and prompts read. */
    public String label() {
        return switch (this) {
            case LATE_NIGHT -> "madrugada"; case DAWN -> "amanecer"; case MORNING -> "mañana"; case NOON -> "mediodía"; case AFTERNOON -> "tarde";
            case SUNSET -> "atardecer"; case NIGHT -> "noche"; case MIDNIGHT -> "medianoche";
        };
    }

    public static Optional<DayPhase> parse(String text) {
        if (text == null) return Optional.empty();
        try { return Optional.of(valueOf(text.trim().toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
}
