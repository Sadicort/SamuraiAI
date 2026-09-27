package yadi.samuraiai.ai.scheduler.priority;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.ai.scheduler.conflict.ConflictResolver;
import yadi.samuraiai.ai.scheduler.conflict.Resolution;
import yadi.samuraiai.ai.scheduler.engine.Candidate;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;

/**
 * Priority Engine 2.0. Four layers (baseline, personal, situational, emergency); each has a threshold a candidate must reach
 * to act. An emergency candidate over its threshold overrides everything. Otherwise the highest layer with a qualifying
 * candidate decides, and inside a layer the {@link ConflictResolver} picks. What is already being done is kept unless a higher
 * layer speaks or a rival in the same layer beats it by the switch margin after the minimum dwell time, so schedules do not flap.
 */
public final class PriorityEngine {
    private final SchedulerSettings settings;
    private final ConflictResolver resolver;

    public PriorityEngine(SchedulerSettings settings) {
        this.settings = settings;
        this.resolver = new ConflictResolver(Math.max(1.0D, settings.switchMargin() / 2.0D));
    }

    public double threshold(PriorityLayer layer) {
        return switch (layer) {
            case BASELINE -> settings.baselineThreshold();
            case PERSONAL -> settings.personalThreshold();
            case SITUATIONAL -> settings.situationalThreshold();
            case EMERGENCY -> settings.emergencyThreshold();
        };
    }

    /**
     * @param candidates   everything the NPC could be doing
     * @param currentKey   key of what it is doing now, or null
     * @param currentLayer the layer that was current, when currentKey is set
     * @param dwellTicks   how long it has been doing it
     * @return the selection, or null when nothing qualifies
     */
    public Selection select(List<Candidate> candidates, String currentKey, PriorityLayer currentLayer, long dwellTicks) {
        if (candidates.isEmpty()) return null;
        List<Candidate> qualifying = new ArrayList<>();
        for (Candidate c : candidates) if (c.score() >= threshold(c.layer())) qualifying.add(c);
        Resolution overall = resolver.resolve(qualifying);
        if (overall == null) return null;
        boolean emergency = overall.winner().layer() == PriorityLayer.EMERGENCY;
        Candidate winner = overall.winner();
        if (currentKey == null || winner.key().equals(currentKey)) return new Selection(winner, winner.key().equals(currentKey), emergency, overall);

        Candidate current = null;
        for (Candidate c : candidates) {
            if (!c.key().equals(currentKey)) continue;
            if (current == null || c.layer().above(current.layer()) || (c.layer() == current.layer() && c.score() > current.score())) current = c;
        }
        if (emergency || winner.layer().above(currentLayer)) return new Selection(winner, false, emergency, overall);
        if (current == null || current.score() < threshold(current.layer())) return new Selection(winner, false, false, overall);
        // same layer (or the current is above the winner): keep it unless a rival clearly beats it after the minimum dwell
        boolean rivalWins = winner.layer() == currentLayer && dwellTicks >= settings.minRoutineTicks() && winner.score() >= current.score() + settings.switchMargin();
        return rivalWins ? new Selection(winner, false, false, overall) : new Selection(current, true, false, overall);
    }
}
