package yadi.samuraiai.ai.scheduler.group;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.scheduler.formation.FormationType;

/** A set of NPCs that act together: its type, members with roles, the current leader and the last known heading. */
public final class Group {
    /** One member's standing in the group. */
    public static final class Member {
        public GroupRole role = GroupRole.RESERVE;
        public final long joinedTick;
        public double x, z;
        Member(long joinedTick) { this.joinedTick = joinedTick; }
    }

    private final String id;
    private final GroupType type;
    private final boolean pinned;
    private final Map<UUID, Member> members = new LinkedHashMap<>();
    private UUID leader;
    private long leaderSince;
    private FormationType formation;
    private FormationType formationOverride;
    double headingX, headingZ = 1.0D;
    double leaderX, leaderZ;
    boolean leaderSeen;
    Alarm alarm;
    long alarmUntil;

    Group(String id, GroupType type, boolean pinned) { this.id = id; this.type = type; this.pinned = pinned; }

    public String id() { return id; }
    public GroupType type() { return type; }
    /** Pinned groups were created explicitly and are never reshuffled by automatic grouping. */
    public boolean pinned() { return pinned; }
    public UUID leader() { return leader; }
    public long leaderSince() { return leaderSince; }
    public Map<UUID, Member> members() { return members; }
    public int size() { return members.size(); }
    public FormationType formation() { return formationOverride != null ? formationOverride : formation; }
    public void forceFormation(FormationType type) { this.formationOverride = type; }
    public Alarm alarm() { return alarm; }
    public double headingX() { return headingX; }
    public double headingZ() { return headingZ; }

    void setLeader(UUID id, long now) { this.leader = id; this.leaderSince = now; }
    void setFormation(FormationType type) { this.formation = type; }
    Member add(UUID id, long now) { return members.computeIfAbsent(id, k -> new Member(now)); }
}
