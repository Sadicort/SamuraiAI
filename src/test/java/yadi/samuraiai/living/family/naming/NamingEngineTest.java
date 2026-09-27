package yadi.samuraiai.living.family.naming;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.living.core.Dice;
import yadi.samuraiai.living.family.model.KinGender;
import yadi.samuraiai.living.family.naming.cultures.NameCulture;
import yadi.samuraiai.living.family.naming.cultures.NameCultureCatalog;
import yadi.samuraiai.living.family.naming.cultures.NameCultureProfile;
import yadi.samuraiai.living.family.naming.generator.SurnameGenerator;
import yadi.samuraiai.living.family.naming.validation.NameQualityValidator;

/** The naming engine on its own, with a fixed seed: deterministic generation, real cultural variety, and a validator that
 * only ever rejects a name a human would stumble on. */
class NamingEngineTest {
    private static final Dice DICE = new Dice(9001L);

    @Test void theSameSeedAndKeyAlwaysNameTheSamePerson() {
        String given1 = NamingEngine.givenName(DICE, "npc-77", KinGender.MASCULINE, NameCulture.ASHEN);
        String given2 = NamingEngine.givenName(DICE, "npc-77", KinGender.MASCULINE, NameCulture.ASHEN);
        assertEquals(given1, given2, "the same world simulated twice names its people the same way");
        assertEquals(NamingEngine.familyName(DICE, "family-3", NameCulture.WESTERN_MARCH), NamingEngine.familyName(DICE, "family-3", NameCulture.WESTERN_MARCH));
        assertEquals(NameCulture.HOLLOW, NamingEngine.cultureFor(DICE, "region-fixed", List.of("hollow:100")), "a weight of 100 for one culture always picks it");
    }

    @Test void everyBuiltInCultureNamesRealPeopleWithoutBreakingQuality() {
        for (NameCulture culture : NameCulture.values()) {
            NameCultureProfile profile = NameCultureCatalog.of(culture);
            assertNotNull(profile, culture + " has a profile");
            for (int i = 0; i < 60; i++) {
                String given = NamingEngine.givenName(DICE, "person-" + culture + "-" + i, i % 2 == 0 ? KinGender.MASCULINE : KinGender.FEMININE, culture);
                assertTrue(NameQualityValidator.acceptable(given, profile.forbiddenSubstrings()), culture + " gave an unacceptable given name: " + given);
                var surname = SurnameGenerator.roll(profile, DICE, "family-" + culture + "-" + i);
                assertTrue(NameQualityValidator.acceptable(surname.surname(), profile.forbiddenSubstrings()), culture + " gave an unacceptable surname: " + surname);
            }
        }
    }

    @Test void everyCuratedWordInEveryCulturePassesQualityOnItsOwn() {
        for (NameCulture culture : NameCulture.values()) {
            NameCultureProfile profile = NameCultureCatalog.of(culture);
            for (String given : profile.masculineGiven()) assertTrue(NameQualityValidator.acceptable(given, profile.forbiddenSubstrings()), culture + " masculine: " + given);
            for (String given : profile.feminineGiven()) assertTrue(NameQualityValidator.acceptable(given, profile.forbiddenSubstrings()), culture + " feminine: " + given);
            for (String given : profile.neutralGiven()) assertTrue(NameQualityValidator.acceptable(given, profile.forbiddenSubstrings()), culture + " neutral: " + given);
            for (String surname : profile.curatedSurnames()) assertTrue(NameQualityValidator.acceptable(surname, profile.forbiddenSubstrings()), culture + " surname: " + surname);
            // most prefix+suffix combinations the compound generator could produce are valid; the validator's job is to
            // catch the rare accidental collision (e.g. a Yamato "Taka" + "shita" spells an English word inside it) and
            // SurnameGenerator retries when that happens, so a small minority failing here is the safety net working, not a bug
            int total = 0, bad = 0;
            for (String prefix : profile.surnamePrefixes()) for (String suffix : profile.surnameSuffixes()) {
                total++;
                if (!NameQualityValidator.acceptable(prefix + suffix.toLowerCase(java.util.Locale.ROOT), profile.forbiddenSubstrings())) bad++;
            }
            assertTrue(bad * 10 < total, culture + ": too many bad compounds (" + bad + "/" + total + ") for the retry loop to cope with");
        }
    }

    @Test void culturesSoundDifferentFromEachOther() {
        // 100 given names from Ashen and 100 from the Western March: real cultural variety, not the same pool relabelled.
        Set<String> ashen = new HashSet<>(), western = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            ashen.add(NamingEngine.givenName(DICE, "a" + i, KinGender.MASCULINE, NameCulture.ASHEN));
            western.add(NamingEngine.givenName(DICE, "w" + i, KinGender.MASCULINE, NameCulture.WESTERN_MARCH));
        }
        Set<String> overlap = new HashSet<>(ashen);
        overlap.retainAll(western);
        assertTrue(overlap.isEmpty(), "Ashen and Western March do not share given names: " + overlap);
        assertTrue(ashen.size() > 5 && western.size() > 5, "not every name collapsed onto the same handful: ashen=" + ashen.size() + " western=" + western.size());
    }

    @Test void theWeightedPickRespectsItsWeightsAndFallsBackWhenTheyAreEmpty() {
        Map<NameCulture, Integer> tally = new EnumMap<>(NameCulture.class);
        for (int i = 0; i < 300; i++) tally.merge(NamingEngine.cultureFor(DICE, "spread-" + i, List.of("yamato:0", "ashen:0", "western_march:0", "old_flame:0", "hollow:100")), 1, Integer::sum);
        assertEquals(300, tally.getOrDefault(NameCulture.HOLLOW, 0), "every roll went to the only weighted culture");
        // an empty or unparsable weight list still names people (the built-in Yamato-leaning spread), never crashes
        assertNotNull(NamingEngine.cultureFor(DICE, "no-weights", List.of()));
        assertNotNull(NamingEngine.cultureFor(DICE, "garbage-weights", List.of("not a culture:5", "also-not:oops")));
    }

    @Test void yamatoKeepsFamilyNameFirstAndTheDarkFantasyCulturesPutTheGivenNameFirst() {
        assertEquals(NameRecord.Order.FAMILY_FIRST, NamingEngine.orderOf(NameCulture.YAMATO));
        for (NameCulture c : List.of(NameCulture.ASHEN, NameCulture.WESTERN_MARCH, NameCulture.OLD_FLAME, NameCulture.HOLLOW))
            assertEquals(NameRecord.Order.GIVEN_FIRST, NamingEngine.orderOf(c), c + " reads given name first");
        NameRecord yamato = new NameRecord("Hiro", "Takeda", "", "", "").withCulture("yamato", NameRecord.Order.FAMILY_FIRST);
        NameRecord ashen = new NameRecord("Aldren", "Ashborne", "", "", "").withCulture("ashen", NameRecord.Order.GIVEN_FIRST);
        assertEquals("Takeda Hiro", yamato.full());
        assertEquals("Aldren Ashborne", ashen.full());
    }

    @Test void formalNameShowsAnEarnedEpithetNaturally() {
        NameRecord plain = new NameRecord("Aldren", "Ashborne", "", "", "").withCulture("ashen", NameRecord.Order.GIVEN_FIRST);
        NameRecord withEpithet = plain.withEpithet("el Honorable");
        assertEquals("Aldren Ashborne", plain.formal());
        assertEquals("Aldren Ashborne, el Honorable", withEpithet.formal());
    }

    @Test void anAutoGeneratedSpawnLabelIsRecognisedAsGenericButAChosenNameIsNot() {
        assertTrue(NamingEngine.isGeneric("Merchant", "merchant"));
        assertTrue(NamingEngine.isGeneric("Merchant_3", "merchant"));
        assertTrue(NamingEngine.isGeneric("merchant_12", "merchant"));
        assertFalse(NamingEngine.isGeneric("Aldren", "merchant"), "a real given name is never generic even if short");
        assertFalse(NamingEngine.isGeneric("Takeda Hiro", "merchant"), "a two-word name was always chosen by someone");
        assertFalse(NamingEngine.isGeneric(null, "merchant"));
    }

    @Test void theQualityValidatorRejectsWhatAHumanWouldStumbleOnAndAcceptsRealNames() {
        assertFalse(NameQualityValidator.acceptable("A", List.of()), "too short");
        assertFalse(NameQualityValidator.acceptable("Xhrzqvklmnpq", List.of()), "an impossible consonant run");
        assertFalse(NameQualityValidator.acceptable("Aeiouae", List.of()), "an impossible vowel run");
        assertFalse(NameQualityValidator.acceptable("Aaaron", List.of()), "the same letter three times in a row");
        assertTrue(NameQualityValidator.acceptable("Anna", List.of()), "a doubled letter is a real name, not a defect");
        assertTrue(NameQualityValidator.acceptable("Sasaki", List.of()), "a repeated syllable that is a real surname is not rejected");
        assertFalse(NameQualityValidator.acceptable("Nombre123", List.of()), "digits are not a name");
        assertFalse(NameQualityValidator.acceptablePair("Aldren", "Aldren", List.of()), "the given name and surname must differ");
        assertTrue(NameQualityValidator.acceptable("Aldren", List.of()));
        assertTrue(NameQualityValidator.acceptable("Ashborne", List.of()));
        assertFalse(NameQualityValidator.acceptable("mierda", List.of()), "the base forbidden list always applies");
    }
}
