package yadi.samuraiai.living.family.clan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.family.reputation.CauseLedger;

/**
 * A clan: a social layer above families the specification asks for — several families under one banner, sharing traditions,
 * allies and rivals, without becoming a second Family or Lineage Engine. A family need not belong to one; nothing creates a
 * clan on its own, an operator or a family head does (a clan is a deliberate, remembered choice, not clutter). Its member
 * families keep their own reputation, honour, heirlooms and lineages; the clan only tracks who belongs, who leads and how the
 * clan as a whole is regarded.
 */
public final class ClanRecord {
    public enum Status { FORMING, ACTIVE, DECLINING, DISPERSED, EXTINCT, HISTORICAL }
    /** Relations beyond ally/rival are hooks for a future politics layer (vassalage, overlordship); nothing acts on them yet. */
    public enum Relation { ALLY, NEUTRAL, RIVAL, HOSTILE, VASSAL_FUTURE, OVERLORD_FUTURE }

    private final UUID id;
    private String name;
    private final UUID founder;
    private final long founded;
    private UUID region;
    private UUID leaderFamily;
    private Status status = Status.FORMING;
    private final Set<UUID> memberFamilies = new LinkedHashSet<>();
    private final Set<String> traditions = new LinkedHashSet<>();
    private final List<String> history = new ArrayList<>();
    private final CauseLedger reputation = new CauseLedger(-1, 1), honor = new CauseLedger(-100, 100);
    private final Map<UUID, Relation> relations = new LinkedHashMap<>();
    private boolean dirty = true;

    public ClanRecord(UUID id, String name, UUID founder, long founded, UUID region) {
        this.id = id; this.name = name; this.founder = founder; this.founded = founded; this.region = region;
    }

    public UUID id() { return id; }
    public String scope() { return "clan:" + id; }
    public String name() { return name; }
    public void name(String v) { name = v; dirty = true; }
    public UUID founder() { return founder; }
    public long founded() { return founded; }
    public UUID region() { return region; }
    public void region(UUID v) { region = v; dirty = true; }
    public UUID leaderFamily() { return leaderFamily; }
    public void leaderFamily(UUID v) { leaderFamily = v; dirty = true; }
    public Status status() { return status; }
    public void status(Status v) { status = v; dirty = true; }
    public Set<UUID> memberFamilies() { return memberFamilies; }
    public Set<String> traditions() { return traditions; }
    public List<String> history() { return history; }
    public void note(String line) { history.add(line); while (history.size() > 100) history.remove(0); dirty = true; }
    public CauseLedger reputation() { return reputation; }
    public CauseLedger honor() { return honor; }
    public Map<UUID, Relation> relations() { return relations; }
    public boolean dirty() { return dirty; }
    public void clean() { dirty = false; }
    public void markDirty() { dirty = true; }
}
