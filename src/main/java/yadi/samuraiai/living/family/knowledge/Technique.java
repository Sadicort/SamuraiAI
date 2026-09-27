package yadi.samuraiai.living.family.knowledge;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A piece of knowledge that must be taught to survive: a sword technique, a forging secret, a sutra, a recipe, a family story.
 * It records who created it and when, the lineage or family that keeps it, how rare it is, who holds it now (living holders)
 * and every transmission (from whom to whom, when, how). When its last holder is gone without having taught it, it is lost —
 * but its evidence (a scroll, an heirloom, a story) remains.
 */
public final class Technique {
    public enum Via { PARENT, MASTER, ORAL, COMMUNITY, PROFESSION, BOOK_FUTURE }
    public record Transmission(UUID from, UUID to, long minute, Via via) { }

    private final String key;
    private final String name;
    private final UUID creator, lineage, family;
    private final long created;
    private final double rarity;
    private final Set<UUID> holders = new LinkedHashSet<>();
    private final List<Transmission> transmissions = new ArrayList<>();
    private long lostAt = Long.MIN_VALUE;
    private String evidence = "";

    public Technique(String key, String name, UUID creator, UUID lineage, UUID family, long created, double rarity) {
        this.key = key; this.name = name; this.creator = creator; this.lineage = lineage; this.family = family; this.created = created; this.rarity = Math.max(0, Math.min(1, rarity));
        if (creator != null) holders.add(creator);
    }

    public String key() { return key; }
    public String name() { return name; }
    public UUID creator() { return creator; }
    public UUID lineage() { return lineage; }
    public UUID family() { return family; }
    public long created() { return created; }
    public double rarity() { return rarity; }
    public Set<UUID> holders() { return holders; }
    public List<Transmission> transmissions() { return transmissions; }
    public boolean lost() { return lostAt != Long.MIN_VALUE; }
    public long lostAt() { return lostAt; }
    public String evidence() { return evidence; }
    public void evidence(String e) { evidence = e == null ? "" : e; }

    public boolean teach(UUID from, UUID to, long minute, Via via) {
        if (lost() || !holders.contains(from) || !holders.add(to)) return false;
        transmissions.add(new Transmission(from, to, minute, via));
        return true;
    }

    /** A holder is gone (missing, dead, historical). Returns true when this made the technique lost. */
    public boolean holderGone(UUID who, long minute) {
        if (!holders.remove(who) || !holders.isEmpty() || lost()) return false;
        lostAt = minute;
        return true;
    }

    public void restore(Set<UUID> h, List<Transmission> t, long lost, String ev) { holders.clear(); holders.addAll(h); transmissions.clear(); transmissions.addAll(t); lostAt = lost; evidence = ev; }
}
