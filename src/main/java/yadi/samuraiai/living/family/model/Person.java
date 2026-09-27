package yadi.samuraiai.living.family.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.family.lifecycle.LifeState;
import yadi.samuraiai.living.family.naming.NameRecord;

/**
 * A person in the family records — the Family Member Record for the living and the Historical Person Record for ancestors, in
 * one lightweight object (never an entity). Kinship itself is not stored here: parents, children, siblings and partners are
 * edges of the {@link yadi.samuraiai.living.family.genealogy.GenealogyGraph}, the single source of truth, so the records can
 * never contradict each other.
 */
public final class Person {
    private final UUID id;
    private NameRecord name;
    private KinGender gender;
    private UUID birthFamily, family, household;
    private int generation;
    private long birth;
    private boolean birthEstimated;
    private UUID birthPlace;
    private long death = Long.MIN_VALUE;
    private LifeState state;
    private String role = "MEMBER";
    private int successionPosition;
    private String profession = "";
    private boolean embodied, agingEnabled = true;
    private final Set<String> legacyTags = new LinkedHashSet<>();
    private final List<String> importantMemories = new ArrayList<>();

    public Person(UUID id, NameRecord name, KinGender gender, UUID family, int generation, long birth, boolean estimated, LifeState state, boolean embodied) {
        this.id = id; this.name = name; this.gender = gender == null ? KinGender.UNSPECIFIED : gender; this.family = family; this.birthFamily = family;
        this.generation = generation; this.birth = birth; this.birthEstimated = estimated; this.state = state; this.embodied = embodied;
    }

    public UUID id() { return id; }
    public NameRecord name() { return name; }
    public void name(NameRecord n) { name = n; }
    public KinGender gender() { return gender; }
    public void gender(KinGender g) { gender = g; }
    public UUID birthFamily() { return birthFamily; }
    public void birthFamily(UUID f) { birthFamily = f; }
    public UUID family() { return family; }
    public void family(UUID f) { family = f; }
    public UUID household() { return household; }
    public void household(UUID h) { household = h; }
    public int generation() { return generation; }
    public void generation(int g) { generation = g; }
    public long birth() { return birth; }
    public void birth(long b, boolean estimated) { birth = b; birthEstimated = estimated; }
    public boolean birthEstimated() { return birthEstimated; }
    public UUID birthPlace() { return birthPlace; }
    public void birthPlace(UUID v) { birthPlace = v; }
    public long death() { return death; }
    public void death(long d) { death = d; }
    public LifeState state() { return state; }
    public void state(LifeState s) { state = s; }
    public String role() { return role; }
    public void role(String r) { role = r == null ? "MEMBER" : r; }
    public int successionPosition() { return successionPosition; }
    public void successionPosition(int p) { successionPosition = p; }
    public String profession() { return profession; }
    public void profession(String p) { profession = p == null ? "" : p; }
    public boolean embodied() { return embodied; }
    public void embodied(boolean e) { embodied = e; }
    public boolean agingEnabled() { return agingEnabled; }
    public void agingEnabled(boolean a) { agingEnabled = a; }
    public Set<String> legacyTags() { return legacyTags; }
    public List<String> importantMemories() { return importantMemories; }
    public void remember(String memory) { importantMemories.add(memory); while (importantMemories.size() > 12) importantMemories.remove(0); }
}
