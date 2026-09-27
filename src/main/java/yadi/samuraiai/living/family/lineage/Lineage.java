package yadi.samuraiai.living.family.lineage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.family.reputation.CauseLedger;

/**
 * A lineage: a family line, a samurai school, a craft tradition, a religious succession, a merchant house or a school of
 * scholars. Biological and martial lineages are separate things: a samurai school is carried by masters and disciples, not by
 * blood. It keeps its founder, its leaders over time (each generation of the school), members, the knowledge it holds, its
 * traditions, reputation, honour, profession, school (a dojo or workshop), heirlooms, history and status.
 */
public final class Lineage {
    public enum Type { FAMILY, SAMURAI, CRAFT, RELIGIOUS, MERCHANT, SCHOLAR, CUSTOM }
    public enum Status { ACTIVE, DORMANT, EXTINCT }
    public record Leadership(UUID leader, long from, long until, String how) { }

    private final UUID id;
    private final String name;
    private final Type type;
    private final UUID founder;
    private final long founded;
    private UUID leader, school;
    private String profession, philosophy = "";
    private final List<Leadership> leaders = new ArrayList<>();
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<String> knowledge = new LinkedHashSet<>();
    private final Set<String> traditions = new LinkedHashSet<>();
    private final Set<UUID> heirlooms = new LinkedHashSet<>();
    private final List<String> history = new ArrayList<>();
    private final CauseLedger reputation = new CauseLedger(-1, 1), honor = new CauseLedger(-100, 100);
    private Status status = Status.ACTIVE;

    public Lineage(UUID id, String name, Type type, UUID founder, long founded, String profession) {
        this.id = id; this.name = name; this.type = type; this.founder = founder; this.founded = founded; this.profession = profession == null ? "" : profession;
        this.leader = founder;
        leaders.add(new Leadership(founder, founded, Long.MAX_VALUE, "fundador"));
        members.add(founder);
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public Type type() { return type; }
    public UUID founder() { return founder; }
    public long founded() { return founded; }
    public UUID leader() { return leader; }
    public UUID school() { return school; }
    public void school(UUID s) { school = s; }
    public String profession() { return profession; }
    public String philosophy() { return philosophy; }
    public void philosophy(String p) { philosophy = p == null ? "" : p; }
    public List<Leadership> leaders() { return leaders; }
    public int generations() { return leaders.size(); }
    public Set<UUID> members() { return members; }
    public Set<String> knowledge() { return knowledge; }
    public Set<String> traditions() { return traditions; }
    public Set<UUID> heirlooms() { return heirlooms; }
    public List<String> history() { return history; }
    public void note(String line) { history.add(line); while (history.size() > 100) history.remove(0); }
    public CauseLedger reputation() { return reputation; }
    public CauseLedger honor() { return honor; }
    public Status status() { return status; }
    public void status(Status s) { status = s; }

    public void succeed(UUID next, long at, String how) {
        if (!leaders.isEmpty()) { Leadership last = leaders.remove(leaders.size() - 1); leaders.add(new Leadership(last.leader(), last.from(), at, last.how())); }
        leaders.add(new Leadership(next, at, Long.MAX_VALUE, how));
        leader = next;
        members.add(next);
    }

    public void restoreLeaders(List<Leadership> saved, UUID current) { leaders.clear(); leaders.addAll(saved); leader = current; }
}
