package yadi.samuraiai.living.family.naming;

import java.util.List;
import java.util.Locale;
import yadi.samuraiai.living.core.Dice;
import yadi.samuraiai.living.family.model.KinGender;
import yadi.samuraiai.living.family.naming.cultures.NameCulture;
import yadi.samuraiai.living.family.naming.cultures.NameCultureCatalog;
import yadi.samuraiai.living.family.naming.cultures.NameCultureProfile;
import yadi.samuraiai.living.family.naming.generator.GivenNameGenerator;
import yadi.samuraiai.living.family.naming.generator.SurnameGenerator;

/**
 * Names for new people and families, drawn deterministically so a world replayed names its people the same way. Every list a
 * given culture needs lives in {@link NameCultureCatalog}, the one place they are authored; this class only orchestrates:
 * which culture a place carries ({@link #cultureFor}), a given name and a surname from it, and telling a name a system handed
 * an NPC (a spawn prefix like "Merchant_3") from one a person actually has. An NPC's existing name is split into given and
 * family name when it already has two words.
 */
public final class NamingEngine {
    private NamingEngine() { }

    /** Which culture a new family founded at {@code key} (usually its origin region) carries; stable for as long as it exists. */
    public static NameCulture cultureFor(Dice dice, String key, List<String> weightLines) { return NameCultureCatalog.weightedPick(dice, key, weightLines); }

    public static String familyName(Dice dice, String key) { return familyName(dice, key, NameCulture.YAMATO); }

    public static String familyName(Dice dice, String key, NameCulture culture) { return SurnameGenerator.roll(NameCultureCatalog.of(culture), dice, key).surname(); }

    public static String givenName(Dice dice, String key, KinGender gender) { return givenName(dice, key, gender, NameCulture.YAMATO); }

    public static String givenName(Dice dice, String key, KinGender gender, NameCulture culture) {
        return GivenNameGenerator.generate(NameCultureCatalog.of(culture), dice, key, gender);
    }

    public static NameRecord.Order orderOf(NameCulture culture) { return NameCultureCatalog.of(culture).order(); }

    public static NameCultureProfile profileOf(NameCulture culture) { return NameCultureCatalog.of(culture); }

    /**
     * Whether {@code rawName} looks like something a system handed the NPC rather than a name someone chose for it: exactly
     * the NPC's type ("merchant"), or the type with the numeric suffix {@code NPCManager.uniqueName} adds on a collision
     * ("Merchant_3"). Only single-word raw names are ever generic (a two-word name is always treated as chosen).
     */
    public static boolean isGeneric(String rawName, String npcType) {
        if (rawName == null || npcType == null || npcType.isBlank()) return false;
        String n = rawName.trim();
        if (n.contains(" ")) return false;
        String type = npcType.trim();
        if (n.equalsIgnoreCase(type)) return true;
        return n.toLowerCase(Locale.ROOT).matches(java.util.regex.Pattern.quote(type.toLowerCase(Locale.ROOT)) + "_\\d+");
    }

    /** Splits an existing NPC name ("Takeda Hiro", "Kenji") into a name record; a single word becomes the given name. */
    public static NameRecord parse(String name) {
        String n = name == null ? "" : name.trim();
        int space = n.indexOf(' ');
        if (space > 0) return new NameRecord(n.substring(space + 1), n.substring(0, space), "", "", "");
        return new NameRecord(n, "", "", "", "");
    }

    public static String key(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }
}
