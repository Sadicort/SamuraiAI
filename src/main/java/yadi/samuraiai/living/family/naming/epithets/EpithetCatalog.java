package yadi.samuraiai.living.family.naming.epithets;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import yadi.samuraiai.living.core.Dice;
import yadi.samuraiai.living.family.model.KinGender;

/**
 * Epithets in the Castilian royal-chronicle style ("Alfonso el Sabio", "Pedro el Cruel", "Juana la Loca") rather than
 * translated English ones, so they read naturally next to the rest of Deiliora's Spanish text regardless of a person's given
 * name or surname culture. Grouped by {@link EpithetCategory}: a category is picked from a real, traceable cause
 * ({@code FamilyEngine.considerEpithet}), never at random; which phrase within it is a deterministic roll.
 */
public final class EpithetCatalog {
    private record Phrase(String masculine, String feminine) { }

    private static final Map<EpithetCategory, List<Phrase>> BANK = new EnumMap<>(EpithetCategory.class);

    static {
        BANK.put(EpithetCategory.LEADERSHIP, List.of(
                new Phrase("el Guardián de la Casa", "la Guardiana de la Casa"), new Phrase("el Primero de los Suyos", "la Primera de los Suyos"),
                new Phrase("el Patriarca", "la Matriarca"), new Phrase("el Heredero", "la Heredera"), new Phrase("el Cabeza de Familia", "la Cabeza de Familia")));
        BANK.put(EpithetCategory.MASTERY, List.of(
                new Phrase("el Maestro", "la Maestra"), new Phrase("el Sabio", "la Sabia"), new Phrase("el Erudito", "la Erudita"),
                new Phrase("el Virtuoso", "la Virtuosa"), new Phrase("el de la Mano Firme", "la de la Mano Firme")));
        BANK.put(EpithetCategory.MENTORSHIP, List.of(
                new Phrase("el Discípulo Cumplido", "la Discípula Cumplida"), new Phrase("el que Terminó su Camino", "la que Terminó su Camino"),
                new Phrase("el Heredero del Maestro", "la Heredera del Maestro"), new Phrase("el Aprendiz de Antaño", "la Aprendiz de Antaño")));
        BANK.put(EpithetCategory.HONOR, List.of(
                new Phrase("el Honorable", "la Honorable"), new Phrase("el Valiente", "la Valiente"), new Phrase("el Intachable", "la Intachable"),
                new Phrase("el Leal", "la Leal"), new Phrase("el de Palabra Firme", "la de Palabra Firme"), new Phrase("el Defensor", "la Defensora")));
        BANK.put(EpithetCategory.DISHONOR, List.of(
                new Phrase("el Deshonrado", "la Deshonrada"), new Phrase("el Traidor", "la Traidora"), new Phrase("el Renegado", "la Renegada"),
                new Phrase("el de Mala Fama", "la de Mala Fama"), new Phrase("el Perjuro", "la Perjura")));
        BANK.put(EpithetCategory.FOUNDING, List.of(
                new Phrase("el Fundador", "la Fundadora"), new Phrase("el Primer Señor del Clan", "la Primera Señora del Clan"),
                new Phrase("el que Alzó el Clan", "la que Alzó el Clan")));
    }

    private EpithetCatalog() { }

    /** A deterministic epithet of a category, agreeing in gender ({@code UNSPECIFIED} reads as masculine, Spanish's default form). */
    public static String pick(EpithetCategory category, KinGender gender, Dice dice, String key) {
        List<Phrase> bank = BANK.get(category);
        Phrase p = bank.get(dice.below("epithet:" + category + ":" + key, 0, bank.size()));
        return gender == KinGender.FEMININE ? p.feminine() : p.masculine();
    }
}
