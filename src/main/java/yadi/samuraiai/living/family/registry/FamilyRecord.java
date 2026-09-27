package yadi.samuraiai.living.family.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.family.family_memory.FamilyMemoryEntry;
import yadi.samuraiai.living.family.reputation.CauseLedger;

/**
 * A family: the Family Record and the Family Runtime together. Identity (name, founders, when and where it began), status,
 * generations, branches, reputation and honour with their causes, historical importance, head and successor, households,
 * traditions, knowledge heritage (the techniques and stories it keeps), heirlooms, properties it holds, relations with other
 * families and its collective memory. Members are found through the family index, not stored twice.
 */
public final class FamilyRecord {
    public enum Status { ACTIVE, DECLINING, EXTINCT, DISPERSED, MIGRATED, HISTORICAL }
    public enum Relation { ALLY, FRIENDLY, NEUTRAL, RIVAL, HOSTILE, MASTER_LINEAGE, TRADE_PARTNER }

    private final UUID id;
    private String name;
    private final List<UUID> founders = new ArrayList<>();
    private final long created;
    private UUID originVillage, originRegion, village, parentFamily, head, successor, designatedHeir;
    private String branchReason = "";
    private Status status = Status.ACTIVE;
    private int generationCount;
    private final Set<UUID> branches = new LinkedHashSet<>();
    private final CauseLedger reputation = new CauseLedger(-1, 1), honor = new CauseLedger(-100, 100);
    private double historicalImportance;
    private String cultureId = "yamato";
    private UUID clan;
    private String houseTitle = "";
    private long houseGrantedAt = Long.MIN_VALUE;
    private final Set<String> tags = new LinkedHashSet<>();
    private final Set<UUID> households = new LinkedHashSet<>();
    private final Set<String> traditions = new LinkedHashSet<>();
    private final Set<String> knowledge = new LinkedHashSet<>();
    private final Set<UUID> heirlooms = new LinkedHashSet<>();
    private final Set<UUID> properties = new LinkedHashSet<>();
    private final Map<UUID, Relation> relations = new LinkedHashMap<>();
    private final List<FamilyMemoryEntry> memory = new ArrayList<>();
    private final Map<String, Integer> professionsByGeneration = new LinkedHashMap<>();
    private boolean dirty = true;

    public FamilyRecord(UUID id, String name, long created, UUID originVillage, UUID originRegion) {
        this.id = id; this.name = name; this.created = created; this.originVillage = originVillage; this.originRegion = originRegion; this.village = originVillage;
    }

    public UUID id() { return id; }
    public String scope() { return "family:" + id; }
    public String name() { return name; }
    public void name(String n) { name = n; dirty = true; }
    public List<UUID> founders() { return founders; }
    public long created() { return created; }
    public UUID originVillage() { return originVillage; }
    public UUID originRegion() { return originRegion; }
    public UUID village() { return village; }
    public void village(UUID v) { village = v; dirty = true; }
    public UUID parentFamily() { return parentFamily; }
    public String branchReason() { return branchReason; }
    public void branchOf(UUID parent, String reason) { parentFamily = parent; branchReason = reason == null ? "" : reason; dirty = true; }
    public UUID head() { return head; }
    public void head(UUID h) { head = h; dirty = true; }
    public UUID successor() { return successor; }
    public void successor(UUID s) { successor = s; dirty = true; }
    public UUID designatedHeir() { return designatedHeir; }
    public void designatedHeir(UUID h) { designatedHeir = h; dirty = true; }
    public Status status() { return status; }
    public void status(Status s) { status = s; dirty = true; }
    public int generationCount() { return generationCount; }
    public void generationCount(int g) { generationCount = Math.max(generationCount, g); dirty = true; }
    public Set<UUID> branches() { return branches; }
    public CauseLedger reputation() { return reputation; }
    public CauseLedger honor() { return honor; }
    public double historicalImportance() { return historicalImportance; }
    public void historicalImportance(double v) { historicalImportance = Math.max(0, Math.min(1, v)); dirty = true; }
    /** The naming culture this family's given names and surname were drawn from ({@link yadi.samuraiai.living.family.naming.cultures.NameCulture}, lower case). Set once, at founding. */
    public String cultureId() { return cultureId; }
    public void cultureId(String v) { cultureId = v == null || v.isBlank() ? "yamato" : v; dirty = true; }
    /** The clan this family belongs to, or {@code null} for one that stands alone. */
    public UUID clan() { return clan; }
    public void clan(UUID v) { clan = v; dirty = true; }
    /** "Casa &lt;apellido&gt;": earned once the family's historical importance and generations cross a threshold; empty until then. */
    public String houseTitle() { return houseTitle; }
    public long houseGrantedAt() { return houseGrantedAt; }
    public void grantHouse(String title, long minute) { houseTitle = title == null ? "" : title; houseGrantedAt = minute; dirty = true; }
    public boolean isHouse() { return !houseTitle.isEmpty(); }
    public Set<String> tags() { return tags; }
    public Set<UUID> households() { return households; }
    public Set<String> traditions() { return traditions; }
    public Set<String> knowledge() { return knowledge; }
    public Set<UUID> heirlooms() { return heirlooms; }
    public Set<UUID> properties() { return properties; }
    public Map<UUID, Relation> relations() { return relations; }
    public List<FamilyMemoryEntry> memory() { return memory; }
    public void remember(FamilyMemoryEntry e) { memory.add(e); while (memory.size() > 200) memory.remove(0); dirty = true; }
    /** How many members of each generation practised each profession ("2:blacksmith" → 1). Traditions come from it. */
    public Map<String, Integer> professionsByGeneration() { return professionsByGeneration; }
    public boolean active() { return status == Status.ACTIVE || status == Status.DECLINING || status == Status.MIGRATED; }
    public boolean dirty() { return dirty; }
    public void markDirty() { dirty = true; }
    public void clean() { dirty = false; }
}
