package yadi.samuraiai.living.family.household;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The Household Runtime: the people who live under one roof. A household is not a family — a family may keep several homes,
 * and a home may shelter people of more than one family. It points at the Village's home building (the house) and at the
 * Economy's accounts; it knows its residents, its head, how many beds the house has, its visitors, a note of its schedule and
 * its status (normal, crowded, empty, abandoned, damaged, destroyed, relocating).
 */
public final class Household {
    public enum Status { NORMAL, CROWDED, EMPTY, ABANDONED, DAMAGED, DESTROYED, RELOCATING }

    private final UUID id;
    private UUID home, village, head, wealthRef;
    private final Set<UUID> residents = new LinkedHashSet<>();
    private final Set<UUID> families = new LinkedHashSet<>();
    private final Set<UUID> visitors = new LinkedHashSet<>();
    private int beds;
    private String schedule = "";
    private Status status = Status.NORMAL;
    private final long founded;

    public Household(UUID id, UUID home, UUID village, long founded) { this.id = id; this.home = home; this.village = village; this.founded = founded; }

    public UUID id() { return id; }
    public UUID home() { return home; }
    public void home(UUID h) { home = h; }
    public UUID village() { return village; }
    public void village(UUID v) { village = v; }
    public UUID head() { return head; }
    public void head(UUID h) { head = h; }
    public UUID wealthRef() { return wealthRef; }
    public void wealthRef(UUID w) { wealthRef = w; }
    public Set<UUID> residents() { return residents; }
    public Set<UUID> families() { return families; }
    public Set<UUID> visitors() { return visitors; }
    public int beds() { return beds; }
    public void beds(int b) { beds = Math.max(0, b); }
    public String schedule() { return schedule; }
    public void schedule(String s) { schedule = s == null ? "" : s; }
    public Status status() { return status; }
    public void status(Status s) { status = s; }
    public long founded() { return founded; }

    /** Recomputes the status from residents, beds and the state of the house. */
    public Status refresh(boolean houseDamaged, boolean houseDestroyed) {
        if (status == Status.RELOCATING && !residents.isEmpty()) return status;
        status = houseDestroyed ? Status.DESTROYED : houseDamaged ? Status.DAMAGED : residents.isEmpty() ? (status == Status.ABANDONED || status == Status.EMPTY ? status : Status.EMPTY)
                : beds > 0 && residents.size() > beds ? Status.CROWDED : Status.NORMAL;
        return status;
    }
}
