package yadi.samuraiai.living.family.naming.validation;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Rejects a generated name before it ever reaches a person: repeated syllables, runs of consonants or vowels no human tongue
 * would attempt, impossible lengths, a given name identical to the surname, and a short list of sequences a culture profile
 * wants to forbid. Structural checks (length, runs, repetition) apply to every culture; the forbidden list is the profile's
 * own. Curated pool entries never fail this (they are hand-authored), which is why the generator always has a safe fallback.
 */
public final class NameQualityValidator {
    private static final int MIN_LENGTH = 2, MAX_LENGTH = 18;
    // 'y' reads as a vowel here ("Aldwyn", "Wystan"), as it does in English and in most of the culture profiles' roots. English
    // compounds routinely cluster 5 consonants at the seam ("Nightgrave" -> ...ghtgr...); 6 is where a run stops being a word.
    private static final Pattern CONSONANT_RUN = Pattern.compile("(?i)[bcdfghjklmnpqrstvwxz]{6,}");
    private static final Pattern VOWEL_RUN = Pattern.compile("(?i)[aeiouy]{4,}");
    // three of the same letter in a row ("Aaa", "Lll"): real names double a letter ("Anna", "Sasaki") but never triple one.
    private static final Pattern TRIPLED_LETTER = Pattern.compile("(?i)(.)\\1\\1");
    private static final List<String> BASE_FORBIDDEN = List.of("mierda", "puta", "shit", "fuck");

    private NameQualityValidator() { }

    /** Whether a single name (given or surname) is well-formed on its own. */
    public static boolean acceptable(String name, List<String> profileForbidden) {
        if (name == null) return false;
        String n = name.trim();
        if (n.length() < MIN_LENGTH || n.length() > MAX_LENGTH) return false;
        if (!n.chars().allMatch(c -> Character.isLetter(c) || c == '\'' || c == '-')) return false;
        if (CONSONANT_RUN.matcher(n).find() || VOWEL_RUN.matcher(n).find() || TRIPLED_LETTER.matcher(n).find()) return false;
        String lower = n.toLowerCase(Locale.ROOT);
        for (String bad : BASE_FORBIDDEN) if (lower.contains(bad)) return false;
        if (profileForbidden != null) for (String bad : profileForbidden) if (!bad.isBlank() && lower.contains(bad.toLowerCase(Locale.ROOT))) return false;
        return true;
    }

    /** Whether a given name and surname make a sound full name together (distinct, both individually acceptable). */
    public static boolean acceptablePair(String given, String surname, List<String> profileForbidden) {
        if (!acceptable(given, profileForbidden) || !acceptable(surname, profileForbidden)) return false;
        return !given.equalsIgnoreCase(surname);
    }
}
