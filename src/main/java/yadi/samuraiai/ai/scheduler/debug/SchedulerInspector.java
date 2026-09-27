package yadi.samuraiai.ai.scheduler.debug;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.ai.scheduler.engine.BehaviorScheduler;
import yadi.samuraiai.ai.scheduler.engine.Candidate;
import yadi.samuraiai.ai.scheduler.engine.NpcSchedule;
import yadi.samuraiai.ai.scheduler.engine.SchedulerAdvice;
import yadi.samuraiai.ai.scheduler.group.Group;
import yadi.samuraiai.ai.scheduler.interrupt.InterruptFrame;
import yadi.samuraiai.ai.scheduler.metrics.SchedulerMetrics;
import yadi.samuraiai.ai.scheduler.routine.RoutineInstance;

/** Explains the scheduler in plain lines: the world clock, one NPC's day and why, groups, zones and the scheduler's own cost. */
public final class SchedulerInspector {
    private SchedulerInspector() { }

    public static List<String> summary(SchedulerMetrics.Snapshot m, BehaviorScheduler s) {
        List<String> lines = new ArrayList<>();
        lines.add("period=" + s.currentPeriod() + " events=" + s.currentEvents().stream().map(e -> e.name()).toList() + " tick=" + s.currentTick());
        lines.add("npcs=" + m.npcs() + " groups=" + m.groups() + " zones=" + m.zones() + " crowdScale=" + String.format("%.2f", m.crowdScale()) + " buckets=" + m.buckets());
        lines.add(String.format("evaluations=%d avg=%.1fus max=%.1fus | tick avg=%.1fus max=%.1fus | deferred=%d failures=%d", m.evaluations(), m.averageEvaluationMicros(),
                m.maxEvaluationMicros(), m.averageTickMicros(), m.maxTickMicros(), m.deferred(), m.failures()));
        if (m.failures() > 0) lines.add("last failure: " + s.metrics().lastFailure());
        lines.add("routines started=" + m.routinesStarted() + " completed=" + m.routinesCompleted() + " interrupted=" + m.interruptions() + " resumed=" + m.resumes()
                + " lapsed=" + m.lapsed() + " conflicts=" + m.conflicts() + " emergencies=" + m.emergencies());
        lines.add("timelineChanges=" + m.timelineChanges() + " moodChanges=" + m.moodChanges() + " leaderChanges=" + m.leaderChanges() + " alarms=" + m.alarms()
                + " personalityChanges=" + m.personalityChanges());
        return lines;
    }

    public static List<String> inspect(NpcSchedule st, long now) {
        List<String> lines = new ArrayList<>();
        SchedulerAdvice a = st.advice();
        lines.add("lifestyle=" + st.lifestyle().id() + " shift=" + st.shift() + " bucket=" + st.bucket() + " mood=" + st.mood().mood() + " (" + (int) st.mood().strength() + ")");
        lines.add("traits " + st.traits());
        lines.add("condition " + st.energy());
        RoutineInstance cur = st.current();
        if (cur == null) lines.add("routine=none" + (a == null ? "" : " (" + a.reason() + ")"));
        else lines.add("routine=" + cur.intent().label() + " " + cur.state() + " layer=" + cur.layer() + " progress=" + (int) (cur.progress() * 100) + "% place=" + (cur.place() == null ? "-" : cur.place().describe())
                + " because " + cur.reason());
        if (a != null) lines.add("advice period=" + a.period() + " background=" + a.background() + (a.groupId() == null ? "" : " group=" + a.groupId() + " role=" + a.role() + " formation=" + a.formation()));
        for (InterruptFrame f : st.stack().frames())
            lines.add("  stacked " + f.instance().intent().label() + " (" + f.policy() + ", by " + f.by() + ", " + (now - f.at()) + "t ago" + (f.expiresAt() == Long.MAX_VALUE ? "" : ", expires in " + Math.max(0, f.expiresAt() - now) + "t") + ")");
        for (Candidate c : st.lastCandidates().stream().sorted((x, y) -> Double.compare(y.score(), x.score())).limit(6).toList())
            lines.add(String.format("  candidate %-16s %-11s %-8s %5.1f  %s", c.key(), c.layer(), c.source(), c.score(), c.reason()));
        var cd = st.cooldowns().snapshot(now);
        if (!cd.isEmpty()) lines.add("cooldowns " + cd);
        return lines;
    }

    public static List<String> groups(BehaviorScheduler s) {
        List<String> lines = new ArrayList<>();
        for (Group g : s.groupCoordinator().groups()) {
            lines.add(g.id() + " " + g.type() + " size=" + g.size() + " leader=" + (g.leader() == null ? "-" : g.leader().toString().substring(0, 8)) + " formation=" + g.formation()
                    + (g.pinned() ? " pinned" : "") + (g.alarm() == null ? "" : " ALARM level " + g.alarm().level()));
        }
        if (lines.isEmpty()) lines.add("no groups");
        return lines;
    }

    public static List<String> zones(BehaviorScheduler s, long now) {
        List<String> lines = new ArrayList<>();
        for (var z : s.zoneRegistry().all()) {
            int occ = s.zoneOccupancy(z.id());
            lines.add(z.id() + " " + z.kind() + " r=" + z.radius() + " at " + Math.round(z.x()) + "," + Math.round(z.y()) + "," + Math.round(z.z()) + " " + occ + "/" + z.capacity()
                    + (z.owner() == null ? "" : " owner=" + z.owner()) + (s.zoneAlerted(z.id(), now) ? " ALERT" : ""));
        }
        if (lines.isEmpty()) lines.add("no zones (routines fall back to a ring around each NPC's home)");
        return lines;
    }
}
