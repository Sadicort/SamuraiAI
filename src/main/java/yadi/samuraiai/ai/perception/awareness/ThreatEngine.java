package yadi.samuraiai.ai.perception.awareness;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;
import yadi.samuraiai.ai.perception.engine.Perceiver;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;

/**
 * Turns dangerous stimuli into a threat picture: hostiles, damage taken, explosions, fire, lava, falls, aggressive
 * players. Each source has a score that fades unless it keeps being perceived; the overall level is the worst source with
 * a fraction of the others added. It reports threat, it never reacts to it: the Brain and the scheduler decide.
 */
public final class ThreatEngine {
    public record Update(ThreatLevel level, double score, List<ThreatSource> newSources, boolean escalated, ThreatLevel previous) { }

    private final Map<String, ThreatSource> sources = new HashMap<>();
    private ThreatLevel level = ThreatLevel.SAFE;
    private double score;

    public ThreatLevel level() { return level; }
    public double score() { return score; }
    public List<ThreatSource> sources() {
        List<ThreatSource> list = new ArrayList<>(sources.values());
        list.sort(Comparator.comparingDouble(ThreatSource::score).reversed());
        return list;
    }
    public void reset() { sources.clear(); level = ThreatLevel.SAFE; score = 0.0D; }

    public Update update(List<Stimulus> stimuli, Perceiver p, long elapsedTicks, long tick, PerceptionSettings s) {
        double decay = s.threatDecayPerTick() * Math.max(1L, elapsedTicks);
        for (var entry : new ArrayList<>(sources.entrySet())) {
            ThreatSource faded = entry.getValue().withScore(entry.getValue().score() - decay);
            if (faded.score() < 1.0D) sources.remove(entry.getKey()); else sources.put(entry.getKey(), faded);
        }
        List<ThreatSource> fresh = new ArrayList<>();
        for (Stimulus st : stimuli) {
            int weight = st.category().threatWeight();
            if (weight <= 0) continue;
            double distance = st.distanceTo(p.x(), p.y(), p.z());
            double proximity = st.category() == StimulusCategory.DAMAGE_TAKEN || st.category() == StimulusCategory.EXPLOSION ? 1.0D : Math.max(0.3D, Math.min(1.0D, 1.2D - distance / 40.0D));
            double contribution = Math.min(100.0D, weight * st.intensity() * proximity * (1.0D + p.fear() / 300.0D * p.senses().fearfulness()));
            if (st.category() == StimulusCategory.DAMAGE_TAKEN) contribution = Math.max(contribution, 90.0D);
            if (contribution < 1.0D) continue;
            String key = st.key();
            ThreatSource known = sources.get(key);
            boolean isNew = known == null;
            double next = isNew ? contribution : Math.max(known.score(), contribution);
            ThreatSource updated = new ThreatSource(key, st.source(), st.category(), st.label(), st.x(), st.y(), st.z(), next, tick);
            sources.put(key, updated);
            if (isNew) fresh.add(updated);
        }
        double max = 0.0D, sum = 0.0D;
        for (ThreatSource source : sources.values()) { max = Math.max(max, source.score()); sum += source.score(); }
        score = Math.min(100.0D, max + 0.25D * (sum - max));
        ThreatLevel previous = level;
        level = score >= s.threatCritical() ? ThreatLevel.CRITICAL : score >= s.threatDanger() ? ThreatLevel.DANGER
                : score >= s.threatWarning() ? ThreatLevel.WARNING : ThreatLevel.SAFE;
        return new Update(level, score, fresh, level.compareTo(previous) > 0, previous);
    }
}
