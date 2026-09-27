package yadi.samuraiai.living.family.naming.cultures;

import java.util.List;
import yadi.samuraiai.living.family.naming.NameRecord;

/**
 * Everything a naming culture needs to produce a person's given name and a family's surname: word order, the word a
 * significant family is called by ("Casa"), pools of given names by gender, a curated pool of full surnames (always valid,
 * the safest and most used strategy), and prefix/suffix roots a compound surname can be built from ({@code SurnameGenerator}).
 * {@code forbiddenSubstrings} blocks sequences that would read badly once combined (empty is fine; the quality validator
 * already screens length, repetition and vowel/consonant runs on every generated name).
 */
public record NameCultureProfile(NameCulture id, NameRecord.Order order, String houseWord,
                                 List<String> masculineGiven, List<String> feminineGiven, List<String> neutralGiven,
                                 List<String> curatedSurnames, List<String> surnamePrefixes, List<String> surnameSuffixes,
                                 List<String> forbiddenSubstrings) {
    public NameCultureProfile {
        masculineGiven = List.copyOf(masculineGiven); feminineGiven = List.copyOf(feminineGiven); neutralGiven = List.copyOf(neutralGiven);
        curatedSurnames = List.copyOf(curatedSurnames); surnamePrefixes = List.copyOf(surnamePrefixes); surnameSuffixes = List.copyOf(surnameSuffixes);
        forbiddenSubstrings = List.copyOf(forbiddenSubstrings);
    }

    public String label() { return id.label(); }
}
