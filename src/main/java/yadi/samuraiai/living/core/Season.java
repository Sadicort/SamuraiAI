package yadi.samuraiai.living.core;

import java.util.Locale;
import java.util.Optional;

/** The four seasons of Deiliora. Which months belong to each is calendar configuration, not code. */
public enum Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    public Season next() { return values()[(ordinal() + 1) % values().length]; }

    public static Optional<Season> parse(String text) {
        if (text == null) return Optional.empty();
        try { return Optional.of(valueOf(text.trim().toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
}
