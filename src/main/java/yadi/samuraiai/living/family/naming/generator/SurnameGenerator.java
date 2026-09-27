package yadi.samuraiai.living.family.naming.generator;

import java.util.Locale;
import yadi.samuraiai.living.core.Dice;
import yadi.samuraiai.living.family.naming.cultures.NameCultureProfile;
import yadi.samuraiai.living.family.naming.validation.NameQualityValidator;

/**
 * Builds a family surname for a culture. {@code CURATED} (the culture's hand-authored pool, always valid) and
 * {@code COMPOUND} (a prefix root + a suffix root from the culture, validated, retried a few times before falling back to
 * curated) are the two strategies a new family rolls between; {@link #toponymic} and {@link #ancestral} are called only when
 * a caller actually has a place name or a founder's given name to build from (a family branch, the house-naming debug tool).
 */
public final class SurnameGenerator {
    public record Result(String surname, SurnameOrigin origin) { }

    private static final double COMPOUND_CHANCE = 0.4D;
    private static final int COMPOUND_ATTEMPTS = 5;

    private SurnameGenerator() { }

    /** The automatic roll for a brand-new family: mostly curated, sometimes a freshly composed compound. */
    public static Result roll(NameCultureProfile profile, Dice dice, String key) {
        if (dice.chance("surname-strategy:" + key, 0, COMPOUND_CHANCE)) {
            for (int attempt = 0; attempt < COMPOUND_ATTEMPTS; attempt++) {
                String candidate = compoundOnce(profile, dice, key, attempt);
                if (NameQualityValidator.acceptable(candidate, profile.forbiddenSubstrings())) return new Result(candidate, SurnameOrigin.COMPOUND);
            }
        }
        return new Result(curated(profile, dice, key), SurnameOrigin.CURATED);
    }

    public static String curated(NameCultureProfile profile, Dice dice, String key) {
        var pool = profile.curatedSurnames();
        return pool.get(dice.below("surname-curated:" + key, 0, pool.size()));
    }

    private static String compoundOnce(NameCultureProfile profile, Dice dice, String key, int attempt) {
        String prefix = profile.surnamePrefixes().get(dice.below("surname-prefix:" + key, attempt, profile.surnamePrefixes().size()));
        String suffix = profile.surnameSuffixes().get(dice.below("surname-suffix:" + key, attempt, profile.surnameSuffixes().size()));
        String joined = prefix + suffix.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(joined.charAt(0)) + joined.substring(1);
    }

    /** A surname derived from a place ("Ashborne of Greyfen" style, or just the place's own name when it already sounds like one). */
    public static Result toponymic(String placeName) {
        if (placeName == null || placeName.isBlank()) return null;
        String base = placeName.trim().replaceAll("[^\\p{L} ]", "");
        String first = base.split(" ")[0];
        String surname = Character.toUpperCase(first.charAt(0)) + first.substring(1).toLowerCase(Locale.ROOT);
        return new Result(surname, SurnameOrigin.TOPONYMIC);
    }

    /** A branch's surname derived from its own founder rather than kept identical to the parent family's ("Aldren's Ward"). */
    public static Result ancestral(NameCultureProfile profile, String founderGiven) {
        if (founderGiven == null || founderGiven.isBlank()) return null;
        String suffix = profile.surnameSuffixes().isEmpty() ? "ward" : profile.surnameSuffixes().get(0);
        String surname = founderGiven.trim() + suffix.toLowerCase(Locale.ROOT);
        return new Result(surname, SurnameOrigin.ANCESTRAL);
    }
}
