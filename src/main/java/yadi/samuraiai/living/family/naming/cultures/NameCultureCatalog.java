package yadi.samuraiai.living.family.naming.cultures;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import yadi.samuraiai.living.core.Dice;
import yadi.samuraiai.living.family.naming.NameRecord;

/**
 * The five naming cultures Deiliora's people can carry, built in (never a second copy of the Yamato lists: this is the one
 * place they live now; {@link yadi.samuraiai.living.family.naming.NamingEngine} reads them from here). Four are the
 * dark-fantasy identity this extension adds — a cold northern culture of fallen holds ({@code ASHEN}), a feudal western
 * kingdom ({@code WESTERN_MARCH}), an old ceremonial fire cult ({@code OLD_FLAME}), and a hollow, decaying culture of the
 * fallen ({@code HOLLOW}) — inspired by medieval and gothic dark fantasy, not copied from any one franchise. Word pools are
 * generous but not exhaustive: {@link yadi.samuraiai.living.family.naming.generator.SurnameGenerator} also composes new
 * surnames from each culture's prefixes and suffixes, so two families of the same culture are not limited to this list.
 */
public final class NameCultureCatalog {
    private static final Map<NameCulture, NameCultureProfile> PROFILES = new EnumMap<>(NameCulture.class);

    private NameCultureCatalog() { }

    static {
        PROFILES.put(NameCulture.YAMATO, new NameCultureProfile(NameCulture.YAMATO, NameRecord.Order.FAMILY_FIRST, "Casa",
                List.of("Hiro", "Kenji", "Taro", "Daichi", "Ren", "Kaito", "Shin", "Jun", "Takumi", "Haruto", "Sota", "Riku", "Yuto", "Kenta",
                        "Ryo", "Makoto", "Isamu", "Masaru", "Tadashi", "Hideo", "Noboru", "Susumu", "Katsuro", "Eiji"),
                List.of("Aiko", "Hana", "Yuki", "Mei", "Rin", "Emi", "Nami", "Sakura", "Haruka", "Aoi", "Mio", "Yui", "Kaede", "Chiyo", "Asami",
                        "Kiyomi", "Natsuki", "Sayuri", "Tomoko", "Hikari", "Reiko", "Shizuka", "Fumiko", "Michiko"),
                List.of("Sora", "Akira", "Hikaru", "Makoto", "Nao", "Kaoru", "Tsubasa", "Hinata", "Izumi", "Mizuki"),
                List.of("Takeda", "Sato", "Kobayashi", "Yamamoto", "Tanaka", "Ito", "Watanabe", "Nakamura", "Kato", "Yoshida", "Yamada", "Sasaki",
                        "Matsumoto", "Inoue", "Kimura", "Hayashi", "Shimizu", "Mori", "Ikeda", "Hashimoto", "Ishikawa", "Ogawa", "Fujita", "Okada",
                        "Goto", "Hasegawa", "Murakami", "Kondo", "Ishii", "Sakamoto", "Endo", "Aoki", "Fujii", "Nishimura", "Fukuda", "Ota", "Miura",
                        "Takeuchi", "Nakano", "Harada"),
                List.of("Taka", "Yama", "Kawa", "Mori", "Shira", "Kuro", "Aka", "Fuji", "Matsu", "Hana", "Ishi", "Kaze", "Tsuki", "Hoshi"),
                List.of("moto", "yama", "kawa", "mura", "hara", "saki", "zaki", "da", "no", "gawa", "hashi", "zawa", "bayashi", "eda"),
                List.of()));

        PROFILES.put(NameCulture.ASHEN, new NameCultureProfile(NameCulture.ASHEN, NameRecord.Order.GIVEN_FIRST, "Casa",
                List.of("Aldren", "Vaelor", "Edric", "Caelan", "Alric", "Mavren", "Theron", "Garran", "Lucan", "Corvin", "Edran", "Vaeren",
                        "Roderic", "Darian", "Mordren", "Aldwyn", "Brenwar", "Cedric", "Halvard", "Osric", "Thurgar", "Wulfram", "Baldric", "Estred"),
                List.of("Elyra", "Maelis", "Ysara", "Elowen", "Mireth", "Vaela", "Nerisse", "Aveline", "Isolde", "Selene", "Morrwen", "Elira",
                        "Thalia", "Sigrid", "Hesper", "Rowena", "Brynhild", "Freyda", "Rowan", "Ashwen", "Duskara", "Ingrith", "Odalys", "Vesnia"),
                List.of("Ash", "Vale", "Storm", "Raven", "Wren", "Sorrel", "Briar", "Fenn", "Rune", "Onyx"),
                List.of("Ashborne", "Blackmere", "Graveward", "Duskryn", "Ironvale", "Ravenholt", "Thornward", "Mourncrest", "Greyhaven",
                        "Cinderhelm", "Stormveil", "Hollowmere", "Nightward", "Dreadmere", "Wyrmholt", "Frostgrave", "Ebonhart", "Duskmere",
                        "Ironfen", "Blackthorn", "Gravemoor", "Ashfen", "Stonewald", "Wolfmere", "Grimhollow", "Coldwarden", "Deepvale",
                        "Ravensworth", "Direfen", "Shadowmere", "Bleakholt", "Thornfield"),
                List.of("Ash", "Black", "Grey", "Iron", "Grave", "Dusk", "Night", "Storm", "Frost", "Ebon", "Bleak", "Grim", "Hollow", "Wolf"),
                List.of("borne", "mere", "holt", "ward", "vale", "holm", "crest", "haven", "fen", "moor", "helm", "veil", "wick", "wald"),
                List.of()));

        PROFILES.put(NameCulture.WESTERN_MARCH, new NameCultureProfile(NameCulture.WESTERN_MARCH, NameRecord.Order.GIVEN_FIRST, "Casa",
                List.of("Alden", "Baldwin", "Rowland", "Godric", "Percival", "Tristan", "Gareth", "Aldous", "Everard", "Lambert", "Reynard",
                        "Humphrey", "Warrick", "Osbert", "Conrad", "Aymeric", "Bertrand", "Geoffrey", "Hallam", "Merrick", "Renfrew", "Sherwin",
                        "Thaddeus", "Wystan"),
                List.of("Elswyth", "Rosalind", "Isabeau", "Marigold", "Cecily", "Adelina", "Beatrix", "Eleanora", "Wynflaed", "Rosamund", "Alys",
                        "Maude", "Constance", "Idony", "Loveday", "Sabine", "Theodora", "Winifred", "Blanche", "Emmeline", "Genevieve", "Hollis",
                        "Iseult", "Matilda"),
                List.of("Robin", "Marlowe", "Ellery", "Sidney", "Avery", "Blythe", "Corin", "Merle", "Sherwood", "Tallis"),
                List.of("Redwyck", "Ashcombe", "Fairhollow", "Longstaff", "Winterbourne", "Kingswood", "Hartwell", "Blackwood", "Stonebridge",
                        "Fenwick", "Thistledown", "Oakhurst", "Ravensfield", "Greymantle", "Whitmore", "Ironhall", "Draycott", "Marchwood",
                        "Hollowell", "Brackenbury", "Stormcrest", "Wyndham", "Ashworth", "Cravenhall", "Hollowbrook", "Kestrelmoor", "Rookwood",
                        "Silverthorn", "Thornbury", "Valemont", "Wolfden", "Yewgarde"),
                List.of("Red", "Ash", "Fair", "Long", "Winter", "Kings", "Hart", "Black", "Stone", "Fen", "Thistle", "Oak", "Raven", "Grey"),
                List.of("wyck", "combe", "hollow", "staff", "bourne", "wood", "well", "bridge", "wick", "down", "hurst", "field", "mantle", "more"),
                List.of()));

        PROFILES.put(NameCulture.OLD_FLAME, new NameCultureProfile(NameCulture.OLD_FLAME, NameRecord.Order.GIVEN_FIRST, "Casa",
                List.of("Vaelis", "Aerion", "Solthar", "Ethanor", "Iralon", "Ornath", "Ielos", "Vaerith", "Solmund", "Aethric", "Ignarion",
                        "Pyrrhus", "Solendil", "Aurvael", "Ashendor", "Ravenor", "Emberic", "Cindrath", "Solaric", "Vaelthorn", "Aethon",
                        "Pyralis", "Sunder", "Solvane"),
                List.of("Vaelara", "Aeris", "Solenne", "Ietha", "Ornia", "Ielara", "Solwen", "Aethera", "Ignara", "Pyrria", "Solacia", "Aurelia",
                        "Cindra", "Emberly", "Solara", "Vaeliss", "Ashenne", "Ravena", "Ietheria", "Sunniva", "Solume", "Pyrenne", "Vestia",
                        "Ignatia"),
                List.of("Ember", "Cinder", "Solace", "Vesper", "Flint", "Dawn", "Sun", "Rowan", "Kindle", "Aeris"),
                List.of("Sunborne", "Emberfall", "Flamecrest", "Solward", "Ashenlight", "Cinderhall", "Pyreholt", "Vaelfire", "Dawnward",
                        "Suncrest", "Emberhart", "Flameborne", "Solmere", "Ashwarden", "Cinderveil", "Pyrewood", "Sunhollow", "Vaelward",
                        "Duskflame", "Emberholt", "Solthorn", "Flarewood", "Ashencrest", "Cindermoor", "Pyrehall", "Sunfen", "Vaelmoor",
                        "Lightholt", "Emberwyn", "Solveil", "Flamewrought", "Radiantholt"),
                List.of("Sun", "Ember", "Flame", "Sol", "Ashen", "Cinder", "Pyre", "Dawn", "Vael", "Light", "Flare", "Kindle", "Radiant", "Holy"),
                List.of("borne", "fall", "crest", "ward", "light", "hall", "holt", "veil", "wood", "hollow", "thorn", "moor", "fen", "wyn"),
                List.of()));

        PROFILES.put(NameCulture.HOLLOW, new NameCultureProfile(NameCulture.HOLLOW, NameRecord.Order.GIVEN_FIRST, "Casa",
                List.of("Morrach", "Velnyr", "Drenoth", "Nyrion", "Vyrandel", "Duskael", "Morwic", "Velnoth", "Dreneth", "Nyrach", "Vyrell",
                        "Corvach", "Valess", "Mornath", "Sylas", "Wrathnor", "Pallenor", "Grimor", "Vasrel", "Ondrach", "Selwyn", "Thanel",
                        "Fenwrath", "Ashnoth"),
                List.of("Morwyn", "Velessa", "Nyra", "Vyranne", "Duskara", "Sylvane", "Corvessa", "Palenne", "Vasrielle", "Ondrelle", "Selwyth",
                        "Thanessa", "Morvara", "Velisse", "Nyssara", "Grimessa", "Wrathenne", "Duskelle", "Vaelessa", "Fennora", "Ashnyra",
                        "Palessa", "Corvyth", "Nyrelle"),
                List.of("Hollow", "Wraith", "Gloam", "Murk", "Faded", "Umbra", "Shade", "Wither", "Pale", "Ashless"),
                List.of("Hollowmere", "Duskgrave", "Nightfallen", "Morncrest", "Vyrewood", "Palewarden", "Graveshade", "Wraithmoor",
                        "Sylvangrave", "Corvain", "Dimhollow", "Fadeward", "Ashenveil", "Grimfallen", "Duskrend", "Velnorwood", "Hollowfen",
                        "Nightgrave", "Palemourn", "Wraithborne", "Shadewick", "Umbracrest", "Direhollow", "Vyrenmoor", "Corvenshade",
                        "Grimwither", "Duskveil", "Ashfallen", "Palecrest", "Hollowrend", "Fadewick", "Nyrenmoor"),
                List.of("Hollow", "Dusk", "Night", "Pale", "Grave", "Wraith", "Shade", "Umbra", "Dire", "Grim", "Fade", "Ashen", "Vyren", "Corven"),
                List.of("mere", "grave", "fallen", "crest", "wood", "warden", "moor", "veil", "wick", "rend", "hollow", "mourn", "borne", "shade"),
                List.of()));
    }

    public static NameCultureProfile of(NameCulture id) { return PROFILES.get(id); }

    public static java.util.Collection<NameCultureProfile> all() { return PROFILES.values(); }

    /**
     * Which culture a new family founded in a place carries, deterministic and stable (the same place always rolls the same
     * culture): a weighted pick over {@code weightLines} ({@code "yamato:35","ashen:20",...}; an unknown or blank list falls
     * back to a broad, Yamato-leaning spread so Deiliora's existing calendar and festivals still feel at home most of the
     * time). {@code key} is usually the founding region's id.
     */
    public static NameCulture weightedPick(Dice dice, String key, java.util.List<String> weightLines) {
        NameCulture[] cultures = NameCulture.values();
        double[] weights = new double[cultures.length];
        boolean any = false;
        for (String line : weightLines == null ? List.<String>of() : weightLines) {
            int c = line.indexOf(':');
            if (c <= 0) continue;
            NameCulture culture = NameCulture.parse(line.substring(0, c).trim()).orElse(null);
            if (culture == null) continue;
            try { weights[culture.ordinal()] = Double.parseDouble(line.substring(c + 1).trim()); any = true; } catch (NumberFormatException ignored) { }
        }
        if (!any) { weights = new double[]{35, 20, 20, 15, 10}; }
        int pick = dice.weighted("name-culture:" + key, 0, weights);
        return pick < 0 ? NameCulture.YAMATO : cultures[pick];
    }

    public static String key(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }
}
