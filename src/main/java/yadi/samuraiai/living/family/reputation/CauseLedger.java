package yadi.samuraiai.living.family.reputation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A value with its causes: family reputation, family honour, a lineage's standing. Every change is kept with why, when and
 * who caused it, so "the family is respected" can always be explained ("defended the village in year 102").
 */
public final class CauseLedger {
    public record Cause(long minute, double delta, String cause, UUID person) { }

    private double value;
    private final double min, max;
    private final List<Cause> causes = new ArrayList<>();

    public CauseLedger(double min, double max) { this.min = min; this.max = max; }

    public double value() { return value; }
    public List<Cause> causes() { return List.copyOf(causes); }

    public double add(double delta, String cause, UUID person, long minute) {
        double before = value;
        value = Math.max(min, Math.min(max, value + delta));
        causes.add(new Cause(minute, value - before, cause, person));
        while (causes.size() > 60) causes.remove(0);
        return value - before;
    }

    public void restore(double v, List<Cause> saved) { value = v; causes.clear(); causes.addAll(saved); }
}
