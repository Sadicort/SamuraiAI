package yadi.samuraiai.ai.scheduler.group;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import yadi.samuraiai.ai.scheduler.engine.Intent;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.events.GroupLeaderChangedEvent;
import yadi.samuraiai.ai.scheduler.formation.FormationEngine;
import yadi.samuraiai.ai.scheduler.formation.FormationType;
import yadi.samuraiai.ai.scheduler.personality.Trait;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.zone.Place;
import yadi.samuraiai.event.EventSink;

/**
 * Group coordination. It forms groups from NPCs of the same kind that are near each other, elects and re-elects leaders by
 * character, gives members roles (scout, guard, reserve), turns the leader's routine into an order for each follower with its
 * formation slot, and carries alarms from one member to the rest. It works only from {@link MemberSnapshot}s and returns
 * orders; it never touches an NPC.
 */
public final class GroupCoordinator {
    private static final AtomicInteger AUTO = new AtomicInteger();

    private final SchedulerSettings settings;
    private final FormationEngine formations = new FormationEngine();
    private final Map<String, Group> groups = new LinkedHashMap<>();
    private final Map<UUID, String> membership = new HashMap<>();
    private final Map<UUID, GroupOrder> orders = new HashMap<>();
    private EventSink events;

    public GroupCoordinator(SchedulerSettings settings, EventSink events) { this.settings = settings; this.events = events; }

    public void useEventSink(EventSink sink) { this.events = sink; }
    public List<Group> groups() { return List.copyOf(groups.values()); }
    public Optional<Group> group(String id) { return Optional.ofNullable(groups.get(id)); }
    public Optional<Group> groupOf(UUID npc) { String id = membership.get(npc); return id == null ? Optional.empty() : Optional.ofNullable(groups.get(id)); }
    public Optional<GroupOrder> orderFor(UUID npc) { return Optional.ofNullable(orders.get(npc)); }
    public GroupRole roleOf(UUID npc) { return groupOf(npc).map(g -> g.members().get(npc)).map(m -> m.role).orElse(null); }

    /** Creates a pinned group that automatic grouping leaves alone. */
    public Group create(String id, GroupType type) {
        Group g = new Group(id, type, true);
        groups.put(id, g);
        return g;
    }

    public boolean join(String groupId, UUID npc, long now) {
        Group g = groups.get(groupId);
        if (g == null || g.size() >= settings.groupMaxSize()) return false;
        leave(npc);
        g.add(npc, now);
        membership.put(npc, groupId);
        return true;
    }

    public void leave(UUID npc) {
        String id = membership.remove(npc);
        orders.remove(npc);
        if (id == null) return;
        Group g = groups.get(id);
        if (g == null) return;
        g.members().remove(npc);
        if (npc.equals(g.leader())) g.setLeader(null, 0);
        if (g.size() == 0 || (!g.pinned() && g.size() < 2)) {
            for (UUID rest : new ArrayList<>(g.members().keySet())) { membership.remove(rest); orders.remove(rest); }
            groups.remove(id);
        }
    }

    public void disband(String id) {
        Group g = groups.remove(id);
        if (g == null) return;
        for (UUID member : g.members().keySet()) { membership.remove(member); orders.remove(member); }
    }

    public void clear() { groups.clear(); membership.clear(); orders.clear(); }

    // ------------------------------------------------------------------ automatic grouping

    /** Adds NPCs that have no group to a nearby group of their kind, or pairs them into a new one; drops members that wandered far off. */
    public void autoGroup(List<MemberSnapshot> npcs, long now) {
        if (!settings.groupsEnabled()) return;
        Map<UUID, MemberSnapshot> byId = new HashMap<>();
        for (MemberSnapshot s : npcs) byId.put(s.id(), s);
        for (Group g : new ArrayList<>(groups.values())) {
            if (g.pinned()) continue;
            for (UUID member : new ArrayList<>(g.members().keySet())) {
                MemberSnapshot me = byId.get(member), lead = g.leader() == null ? null : byId.get(g.leader());
                if (me == null) { leave(member); continue; }
                if (lead != null && !lead.id().equals(me.id()) && (!lead.dimension().equals(me.dimension()) || distance(lead, me) > settings.autoGroupRadius() * 2.5D)) leave(member);
            }
        }
        List<MemberSnapshot> loose = new ArrayList<>();
        for (MemberSnapshot s : npcs) if (!membership.containsKey(s.id()) && GroupType.parse(s.groupType()).isPresent()) loose.add(s);
        loose.sort(Comparator.comparing(MemberSnapshot::id));
        for (MemberSnapshot s : loose) {
            if (membership.containsKey(s.id())) continue;
            GroupType type = GroupType.parse(s.groupType()).get();
            Group target = null;
            for (Group g : groups.values()) {
                if (g.pinned() || g.type() != type || g.size() >= settings.groupMaxSize()) continue;
                for (UUID member : g.members().keySet()) {
                    MemberSnapshot near = byId.get(member);
                    if (near != null && near.dimension().equals(s.dimension()) && distance(near, s) <= settings.autoGroupRadius()) { target = g; break; }
                }
                if (target != null) break;
            }
            if (target == null) {
                for (MemberSnapshot other : loose) {
                    if (other.id().equals(s.id()) || membership.containsKey(other.id()) || !other.groupType().equalsIgnoreCase(s.groupType())) continue;
                    if (!other.dimension().equals(s.dimension()) || distance(other, s) > settings.autoGroupRadius()) continue;
                    target = new Group(type.name().toLowerCase() + "-" + AUTO.incrementAndGet(), type, false);
                    groups.put(target.id(), target);
                    target.add(other.id(), now);
                    membership.put(other.id(), target.id());
                    break;
                }
            }
            if (target != null) { target.add(s.id(), now); membership.put(s.id(), target.id()); }
        }
    }

    private static double distance(MemberSnapshot a, MemberSnapshot b) { return Math.hypot(a.x() - b.x(), a.z() - b.z()); }

    // ------------------------------------------------------------------ synchronisation: leader, roles, orders

    /** Re-elects leaders, assigns roles and builds an order for every follower. Called every {@code groupSyncTicks}. */
    public void sync(List<MemberSnapshot> npcs, long now) {
        Map<UUID, MemberSnapshot> byId = new HashMap<>();
        for (MemberSnapshot s : npcs) byId.put(s.id(), s);
        orders.clear();
        for (Group g : new ArrayList<>(groups.values())) {
            for (UUID id : new ArrayList<>(g.members().keySet())) if (!byId.containsKey(id)) leave(id);
            if (!groups.containsKey(g.id())) continue;
            electLeader(g, byId, now);
            assignRoles(g, byId);
            if (g.alarm != null && now >= g.alarmUntil) g.alarm = null;
            buildOrders(g, byId, now);
        }
    }

    private double leadership(Group g, UUID id, MemberSnapshot s, long now) {
        double character = (s.traits().get(Trait.LOYALTY) + s.traits().get(Trait.DISCIPLINE) + s.traits().get(Trait.COURAGE)
                + s.traits().get(Trait.PATIENCE) + s.traits().get(Trait.DILIGENCE)) / 5.0D;
        double seniority = Math.min(10.0D, (now - g.members().get(id).joinedTick) / 12000.0D);
        return character + seniority;
    }

    private void electLeader(Group g, Map<UUID, MemberSnapshot> byId, long now) {
        UUID best = null;
        double bestScore = -1;
        for (UUID id : g.members().keySet()) {
            double score = leadership(g, id, byId.get(id), now);
            if (score > bestScore || (score == bestScore && best != null && id.compareTo(best) < 0)) { best = id; bestScore = score; }
        }
        if (best == null) return;
        UUID current = g.leader();
        boolean vacant = current == null || !g.members().containsKey(current);
        boolean due = !vacant && now - g.leaderSince() >= settings.leaderReelectTicks();
        if (vacant || (due && !best.equals(current) && bestScore > leadership(g, current, byId.get(current), now) + 8.0D)) {
            UUID previous = vacant ? null : current;
            g.setLeader(best, now);
            events.publish(new GroupLeaderChangedEvent(g.id(), previous, best, g.size(), bestScore));
        }
    }

    private void assignRoles(Group g, Map<UUID, MemberSnapshot> byId) {
        List<UUID> followers = new ArrayList<>();
        for (UUID id : g.members().keySet()) if (!id.equals(g.leader())) followers.add(id);
        g.members().get(g.leader()).role = GroupRole.LEADER;
        int scouts = (int) Math.round(followers.size() * 0.25D), guards = (int) Math.round(followers.size() * 0.5D);
        List<UUID> byScout = new ArrayList<>(followers);
        byScout.sort(Comparator.<UUID>comparingDouble(id -> -(byId.get(id).traits().get(Trait.CURIOSITY) + byId.get(id).traits().get(Trait.COURAGE))).thenComparing(id -> id));
        List<UUID> remaining = new ArrayList<>(followers);
        for (int i = 0; i < scouts && i < byScout.size(); i++) { g.members().get(byScout.get(i)).role = GroupRole.SCOUT; remaining.remove(byScout.get(i)); }
        remaining.sort(Comparator.<UUID>comparingDouble(id -> -(byId.get(id).traits().get(Trait.COURAGE) + byId.get(id).traits().get(Trait.AGGRESSION))).thenComparing(id -> id));
        for (int i = 0; i < remaining.size(); i++) g.members().get(remaining.get(i)).role = i < guards ? GroupRole.GUARD : GroupRole.RESERVE;
    }

    private void buildOrders(Group g, Map<UUID, MemberSnapshot> byId, long now) {
        MemberSnapshot lead = byId.get(g.leader());
        if (lead == null) return;
        double dx = lead.x() - g.leaderX, dz = lead.z() - g.leaderZ;
        if (g.leaderSeen && Math.hypot(dx, dz) > 0.5D) { double n = Math.hypot(dx, dz); g.headingX = dx / n; g.headingZ = dz / n; }
        else if (lead.routinePlace() != null && lead.travelling()) {
            double tx = lead.routinePlace().x() - lead.x(), tz = lead.routinePlace().z() - lead.z(), n = Math.hypot(tx, tz);
            if (n > 0.5D) { g.headingX = tx / n; g.headingZ = tz / n; }
        }
        g.leaderX = lead.x(); g.leaderZ = lead.z(); g.leaderSeen = true;
        for (var e : g.members().entrySet()) { MemberSnapshot m = byId.get(e.getKey()); if (m != null) { e.getValue().x = m.x(); e.getValue().z = m.z(); } }
        RoutineType routine = lead.routine();
        if (routine == null || !g.type().shares(routine) || lead.emergency()) { g.setFormation(null); return; }
        FormationType shape = lead.travelling() ? g.type().moving() : g.type().standing();
        g.setFormation(shape);
        FormationType use = g.formation();
        List<FormationEngine.Member> members = new ArrayList<>();
        for (var e : g.members().entrySet()) members.add(new FormationEngine.Member(e.getKey(), e.getValue().role));
        var assignments = formations.assign(use, g.leader(), lead.x(), lead.z(), g.headingX, g.headingZ, members, settings.formationSpacing());
        for (var a : assignments) {
            if (a.member().equals(g.leader())) continue;
            Place at = new Place(lead.dimension(), a.x(), lead.y(), a.z(), Math.max(1.0D, settings.formationSpacing() * 0.4D), null);
            orders.put(a.member(), new GroupOrder(g.id(), g.leader(), Intent.of(routine, at), a.role(), a.slot(), use.name(), now));
        }
    }

    // ------------------------------------------------------------------ alarms

    /** A member calls for help; every other member of its group will see it until it lapses. */
    public void raiseAlarm(Alarm alarm, long now) {
        groupOf(alarm.source()).ifPresent(g -> {
            if (g.alarm == null || alarm.level() >= g.alarm.level()) g.alarm = alarm;
            g.alarmUntil = Math.max(g.alarmUntil, now + settings.groupSyncTicks() * 6L);
        });
    }

    /** The alarm this NPC should react to: its group's, unless it raised it itself or it is out of range. */
    public Optional<Alarm> alarmFor(UUID npc, String dimension, double x, double z, long now) {
        return groupOf(npc).map(g -> g.alarm).filter(a -> a != null && !a.source().equals(npc) && a.dimension().equals(dimension)
                && Math.hypot(a.x() - x, a.z() - z) <= settings.groupAlertRadius() * 2.0D && now - a.tick() < settings.groupSyncTicks() * 12L);
    }
}
