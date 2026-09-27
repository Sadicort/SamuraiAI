package yadi.samuraiai.ai.scheduler.optimize;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;

/**
 * Crowd management: spreads the evaluations that are due over ticks so that a large population never all thinks at once.
 * NPCs are staggered by a per-NPC phase; among those due, the most overdue go first, up to the per-tick quota; the rest
 * wait for the next tick (deferred) and, having waited, are the most overdue then.
 */
public final class CrowdManager {
    /** An NPC that may need evaluating; a newly seen NPC gets {@code lastEvaluated = now - interval + phase} so first evaluations are staggered. */
    public record Candidate(UUID id, int interval, long lastEvaluated, boolean urgent) { }

    /** The outcome for one tick. */
    public record Plan(List<UUID> evaluate, int due, int deferred) { }

    private final SchedulerSettings settings;

    public CrowdManager(SchedulerSettings settings) { this.settings = settings; }

    public static int phaseOf(UUID id, int interval) { return Math.floorMod(id.hashCode(), Math.max(1, interval)); }

    public Plan plan(List<Candidate> candidates, long now) {
        record Due(Candidate candidate, long lateness) { }
        List<Due> due = new ArrayList<>();
        for (Candidate c : candidates) {
            long lateness = now - (c.lastEvaluated() + c.interval());
            if (c.urgent()) due.add(new Due(c, Long.MAX_VALUE / 2));
            else if (lateness >= 0) due.add(new Due(c, lateness));
        }
        due.sort(Comparator.<Due>comparingLong(d -> -d.lateness).thenComparing(d -> d.candidate.id()));
        int quota = settings.maxEvaluationsPerTick();
        List<UUID> chosen = new ArrayList<>();
        for (Due d : due) { if (chosen.size() >= quota && !d.candidate.urgent()) break; chosen.add(d.candidate.id()); }
        return new Plan(chosen, due.size(), due.size() - chosen.size());
    }
}
