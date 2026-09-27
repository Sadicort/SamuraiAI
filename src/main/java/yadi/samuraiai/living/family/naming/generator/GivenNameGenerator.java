package yadi.samuraiai.living.family.naming.generator;

import java.util.List;
import yadi.samuraiai.living.core.Dice;
import yadi.samuraiai.living.family.model.KinGender;
import yadi.samuraiai.living.family.naming.cultures.NameCultureProfile;

/** A personal given name from a culture's curated pool (hand-authored, so always valid; the safest and most used strategy). */
public final class GivenNameGenerator {
    private GivenNameGenerator() { }

    public static String generate(NameCultureProfile profile, Dice dice, String key, KinGender gender) {
        List<String> pool = gender == KinGender.MASCULINE ? profile.masculineGiven() : gender == KinGender.FEMININE ? profile.feminineGiven() : profile.neutralGiven();
        if (pool.isEmpty()) pool = profile.neutralGiven();
        return pool.get(dice.below("given-name:" + key, 0, pool.size()));
    }
}
