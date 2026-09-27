package yadi.samuraiai.living.family.naming;

/**
 * A person's name in parts: given name, family name, honorific, title, the lineage (school, craft) they carry, an earned
 * epithet, the naming culture it came from, and the order the culture displays it in. In Yamato ("Takeda Hiro") the family
 * name goes first; in the dark-fantasy cultures ("Aldren Ashborne") the given name does.
 */
public record NameRecord(String given, String family, String honorific, String title, String lineageName, String epithet, String cultureId, Order order) {
    /** How a culture writes a full name: family name first (Yamato) or given name first (the dark-fantasy cultures). */
    public enum Order { FAMILY_FIRST, GIVEN_FIRST }

    public NameRecord {
        given = given == null ? "" : given; family = family == null ? "" : family; honorific = honorific == null ? "" : honorific;
        title = title == null ? "" : title; lineageName = lineageName == null ? "" : lineageName; epithet = epithet == null ? "" : epithet;
        cultureId = cultureId == null ? "" : cultureId; order = order == null ? Order.FAMILY_FIRST : order;
    }

    /** The 5-part legacy shape, defaulting to Yamato order with no epithet or culture recorded. */
    public NameRecord(String given, String family, String honorific, String title, String lineageName) {
        this(given, family, honorific, title, lineageName, "", "", Order.FAMILY_FIRST);
    }

    public String full() {
        if (family.isEmpty()) return given;
        return order == Order.GIVEN_FIRST ? given + " " + family : family + " " + given;
    }

    public String formal() {
        StringBuilder sb = new StringBuilder();
        if (!title.isEmpty()) sb.append(title).append(' ');
        sb.append(full());
        if (!epithet.isEmpty()) sb.append(", ").append(epithet);
        if (!honorific.isEmpty()) sb.append('-').append(honorific);
        if (!lineageName.isEmpty()) sb.append(" (").append(lineageName).append(')');
        return sb.toString();
    }

    public NameRecord withFamily(String f) { return new NameRecord(given, f, honorific, title, lineageName, epithet, cultureId, order); }
    public NameRecord withTitle(String t) { return new NameRecord(given, family, honorific, t, lineageName, epithet, cultureId, order); }
    public NameRecord withHonorific(String h) { return new NameRecord(given, family, h, title, lineageName, epithet, cultureId, order); }
    public NameRecord withLineage(String l) { return new NameRecord(given, family, honorific, title, l, epithet, cultureId, order); }
    public NameRecord withEpithet(String e) { return new NameRecord(given, family, honorific, title, lineageName, e, cultureId, order); }
    public NameRecord withCulture(String c, Order o) { return new NameRecord(given, family, honorific, title, lineageName, epithet, c, o); }
}
