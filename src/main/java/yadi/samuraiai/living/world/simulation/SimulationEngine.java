package yadi.samuraiai.living.world.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import yadi.samuraiai.living.core.TickBudget;
import yadi.samuraiai.living.world.regions.Region;
import yadi.samuraiai.living.world.streaming.SimulationLevel;

/**
 * The world simulation scheduler. Each region is simulated in steps whose length follows its level of detail (an hour near a
 * player, a day far away, a week for forgotten lands); only regions whose step is due run, most overdue first, within a
 * per-tick count and time budget. When a region wakes up after a long sleep it is caught up with a <b>bounded</b> number of
 * coarse steps ({@code maxCatchUpSteps}), never tick by tick.
 */
public final class SimulationEngine {
    public record CatchUp(Region region, long elapsed, int steps, long stepLength) { }

    private final List<RegionSimulator> simulators = new ArrayList<>();
    private final Map<SimulationLevel, Long> stepMinutes = new EnumMap<>(SimulationLevel.class);
    private int maxRegionsPerTick = 4, maxCatchUpSteps = 48;
    private long steps, catchUps, catchUpSteps, failures, stepNanos;
    private String lastError = "";

    public SimulationEngine() {
        stepMinutes.put(SimulationLevel.FULL, 60L);
        stepMinutes.put(SimulationLevel.ACTIVE, 60L);
        stepMinutes.put(SimulationLevel.SETTLEMENT, 360L);
        stepMinutes.put(SimulationLevel.ABSTRACT, 1440L);
        stepMinutes.put(SimulationLevel.HISTORICAL, 7L * 1440);
    }

    public void configure(long active, long settlement, long abstractStep, long historical, int maxPerTick, int maxCatchUp) {
        stepMinutes.put(SimulationLevel.FULL, Math.max(1, active));
        stepMinutes.put(SimulationLevel.ACTIVE, Math.max(1, active));
        stepMinutes.put(SimulationLevel.SETTLEMENT, Math.max(1, settlement));
        stepMinutes.put(SimulationLevel.ABSTRACT, Math.max(1, abstractStep));
        stepMinutes.put(SimulationLevel.HISTORICAL, Math.max(1, historical));
        maxRegionsPerTick = Math.max(1, maxPerTick);
        maxCatchUpSteps = Math.max(1, maxCatchUp);
    }

    public void register(RegionSimulator simulator) { simulators.add(simulator); }
    public List<RegionSimulator> simulators() { return List.copyOf(simulators); }
    public long step(SimulationLevel level) { return stepMinutes.get(level); }

    /** Simulates the regions that are due, within the budget. Returns how many were simulated. */
    public int tick(Collection<Region> regions, long now, TickBudget budget) {
        List<Region> due = new ArrayList<>();
        for (Region r : regions) if (now - r.lastSimulated() >= step(r.level())) due.add(r);
        if (due.isEmpty()) return 0;
        due.sort((a, b) -> Long.compare((now - b.lastSimulated()) - step(b.level()), (now - a.lastSimulated()) - step(a.level())));
        int done = 0;
        for (Region r : due) {
            if (done >= maxRegionsPerTick || budget.exhausted()) break;
            long elapsed = now - r.lastSimulated();
            if (elapsed > step(r.level()) * (long) maxCatchUpSteps) catchUp(r, now);   // long overdue (the server was busy): bounded coarse steps
            else run(r, r.lastSimulated(), now, r.level(), false);
            budget.spent();
            done++;
        }
        return done;
    }

    /** Brings a region from its last simulated minute to {@code now} in at most {@code maxCatchUpSteps} steps. */
    public CatchUp catchUp(Region region, long now) {
        long elapsed = now - region.lastSimulated();
        if (elapsed <= 0) return new CatchUp(region, 0, 0, 0);
        long fine = step(SimulationLevel.ACTIVE);
        int count = (int) Math.max(1, Math.min(maxCatchUpSteps, (elapsed + fine - 1) / fine));
        long length = (elapsed + count - 1) / count;
        long t = region.lastSimulated();
        int done = 0;
        while (t < now) {
            long next = Math.min(now, t + length);
            run(region, t, next, region.level(), true);
            t = next;
            done++;
        }
        catchUps++;
        catchUpSteps += done;
        return new CatchUp(region, elapsed, done, length);
    }

    private void run(Region region, long from, long to, SimulationLevel level, boolean catchUp) {
        long started = System.nanoTime();
        for (RegionSimulator s : simulators) {
            try { s.simulate(region, from, to, level, catchUp); }
            catch (RuntimeException error) { failures++; lastError = s.name() + ": " + error; }
        }
        region.lastSimulated(to);
        steps++;
        stepNanos += System.nanoTime() - started;
    }

    public long steps() { return steps; }
    public long catchUps() { return catchUps; }
    public long catchUpSteps() { return catchUpSteps; }
    public long failures() { return failures; }
    public String lastError() { return lastError; }
    public double averageStepMicros() { return steps == 0 ? 0 : stepNanos / 1000.0D / steps; }
}
