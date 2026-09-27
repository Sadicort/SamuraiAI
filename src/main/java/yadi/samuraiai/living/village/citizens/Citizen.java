package yadi.samuraiai.living.village.citizens;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * What the Village Engine owns about one inhabitant: citizenship (village, status, since when), profession and the
 * experience gained in it, home, roles in the community, and the offset that makes this person's day not identical to the
 * neighbour's. Age and family belong to the Family Engine; relationships, emotions and memories to the cognitive layer; the
 * citizen view assembled by the hub brings them together.
 *
 * <p>{@code embodied} is false for citizens that exist only in the simulation (born or arrived while the region was
 * abstract); they live, work and consume like the others but have no entity until the adapter gives them one.
 */
public final class Citizen {
    public enum Status { RESIDENT, AWAY, VISITOR, MIGRATED, MISSING, DECEASED }
    public enum Role { ELDER, HEADMAN, GUARD_CAPTAIN, PRIEST, MERCHANT_BOSS, APPRENTICE }

    private final UUID id;
    private String name;
    private final String npcType;
    private UUID village;
    private Status status = Status.RESIDENT;
    private String profession = "";
    private double professionHours;
    private UUID home;
    private final Set<Role> roles = new LinkedHashSet<>();
    private final long joined;
    private int scheduleOffset;
    private boolean embodied;
    private double hoursToday;
    private long lastWorkDay = Long.MIN_VALUE;

    public Citizen(UUID id, String name, String npcType, UUID village, long joined, boolean embodied) {
        this.id = id; this.name = name == null ? "?" : name; this.npcType = npcType == null ? "villager" : npcType; this.village = village; this.joined = joined; this.embodied = embodied;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public void name(String v) { name = v; }
    public String npcType() { return npcType; }
    public UUID village() { return village; }
    public void village(UUID v) { village = v; }
    public Status status() { return status; }
    public void status(Status v) { status = v; }
    public boolean present() { return status == Status.RESIDENT || status == Status.VISITOR; }
    public String profession() { return profession; }
    public void profession(String v) { profession = v == null ? "" : v; }
    public double professionHours() { return professionHours; }
    public void professionHours(double v) { professionHours = Math.max(0, v); }
    public UUID home() { return home; }
    public void home(UUID v) { home = v; }
    public Set<Role> roles() { return roles; }
    public long joined() { return joined; }
    public int scheduleOffset() { return scheduleOffset; }
    public void scheduleOffset(int v) { scheduleOffset = v; }
    public boolean embodied() { return embodied; }
    public void embodied(boolean v) { embodied = v; }

    /** Records hours of work for the profession; resets the daily tally on a new day. */
    public void worked(double hours, long dayIndex) {
        if (dayIndex != lastWorkDay) { hoursToday = 0; lastWorkDay = dayIndex; }
        hoursToday += Math.max(0, hours);
        professionHours += Math.max(0, hours);
    }
    public double hoursToday(long dayIndex) { return dayIndex == lastWorkDay ? hoursToday : 0; }
    public void restoreWork(double today, long day) { hoursToday = today; lastWorkDay = day; }
    public long lastWorkDay() { return lastWorkDay; }
}
