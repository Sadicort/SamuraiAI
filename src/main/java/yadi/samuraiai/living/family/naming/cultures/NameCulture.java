package yadi.samuraiai.living.family.naming.cultures;

import java.util.Locale;
import java.util.Optional;

/**
 * The naming cultures a region of Deiliora can carry. {@code YAMATO} is the samurai-era culture the calendar, festivals and
 * professions already speak; the other four are the dark-fantasy identity this extension adds: a cold northern culture of
 * fallen holds, a feudal western kingdom, an old ceremonial fire cult, and a hollow, decaying culture of the fallen. Which
 * culture a family carries is decided once, deterministically, per region (see {@code NameCultureCatalog.weightedPick}); every
 * member of that family keeps it for as long as the family exists.
 */
public enum NameCulture {
    YAMATO("Yamato"), ASHEN("Ceniza del Norte"), WESTERN_MARCH("Marca Occidental"), OLD_FLAME("Vieja Llama"), HOLLOW("Los Huecos");

    private final String label;
    NameCulture(String label) { this.label = label; }

    /** The Spanish label shown to players and operators. */
    public String label() { return label; }

    public static Optional<NameCulture> parse(String text) {
        if (text == null) return Optional.empty();
        try { return Optional.of(valueOf(text.trim().toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
}
