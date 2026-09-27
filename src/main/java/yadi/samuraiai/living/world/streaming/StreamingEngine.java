package yadi.samuraiai.living.world.streaming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import yadi.samuraiai.living.world.regions.Region;

/**
 * World streaming: decides each region's {@link SimulationLevel} from where players are, so the living world is simulated at
 * full fidelity only where someone can see it and degrades gracefully into abstraction elsewhere. A region nobody has come
 * near for {@code historicalAfterMinutes} drops to HISTORICAL. It only reports transitions; the world engine reacts
 * (catch-up simulation when a region wakes, events on the bus).
 */
public final class StreamingEngine {
    /** Where a player (or anything that should keep the world detailed) is. */
    public record Viewer(String dimension, double x, double z) { }

    public record Transition(Region region, SimulationLevel from, SimulationLevel to) {
        public boolean woke() { return to.ordinal() <= SimulationLevel.ACTIVE.ordinal() && from.ordinal() > SimulationLevel.ACTIVE.ordinal(); }
        public boolean slept() { return to.ordinal() > SimulationLevel.ACTIVE.ordinal() && from.ordinal() <= SimulationLevel.ACTIVE.ordinal(); }
    }

    private double fullRadius = 96, nearRadius = 320, settlementRadius = 1200;
    private long historicalAfterMinutes = 30L * 1440;
    private long updates, transitions;

    public void configure(double full, double near, double settlement, long historicalAfter) {
        fullRadius = Math.max(8, full); nearRadius = Math.max(fullRadius, near); settlementRadius = Math.max(nearRadius, settlement); historicalAfterMinutes = Math.max(1440, historicalAfter);
    }

    public SimulationLevel classify(Region region, List<Viewer> viewers, long now) {
        double best = Double.MAX_VALUE;
        for (Viewer v : viewers) if (v.dimension().equals(region.dimension())) best = Math.min(best, region.distanceTo(v.x(), v.z()));
        if (best <= settlementRadius) region.lastPlayerSeen(now);
        if (best <= fullRadius) return SimulationLevel.FULL;
        if (best <= nearRadius) return SimulationLevel.ACTIVE;
        if (best <= settlementRadius) return SimulationLevel.SETTLEMENT;
        return now - region.lastPlayerSeen() > historicalAfterMinutes ? SimulationLevel.HISTORICAL : SimulationLevel.ABSTRACT;
    }

    public List<Transition> update(Collection<Region> regions, List<Viewer> viewers, long now) {
        updates++;
        List<Transition> out = new ArrayList<>();
        for (Region r : regions) {
            SimulationLevel next = classify(r, viewers, now);
            if (next != r.level()) { out.add(new Transition(r, r.level(), next)); r.level(next); transitions++; }
        }
        return out;
    }

    public long updates() { return updates; }
    public long transitions() { return transitions; }
    public double fullRadius() { return fullRadius; }
    public double nearRadius() { return nearRadius; }
    public double settlementRadius() { return settlementRadius; }
}
