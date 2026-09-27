package yadi.samuraiai.living.economy.metrics;

import java.util.concurrent.atomic.AtomicLong;

/** Volume and cost of the Economy & Trade Engine: steps, production, consumption, prices, caravans, shortages, trades, contracts. */
public final class EconomyMetrics {
    public final AtomicLong steps = new AtomicLong(), productionRuns = new AtomicLong(), priceChanges = new AtomicLong(), priceUpdates = new AtomicLong(), caravansCreated = new AtomicLong(),
            caravansArrived = new AtomicLong(), caravansLost = new AtomicLong(), ambushes = new AtomicLong(), shortages = new AtomicLong(), surpluses = new AtomicLong(), trades = new AtomicLong(),
            contracts = new AtomicLong(), contractsFulfilled = new AtomicLong(), tradePlans = new AtomicLong();
    public final AtomicLong stepNanos = new AtomicLong(), caravanNanos = new AtomicLong(), planNanos = new AtomicLong();
    private final java.util.concurrent.atomic.DoubleAdder produced = new java.util.concurrent.atomic.DoubleAdder(), consumed = new java.util.concurrent.atomic.DoubleAdder();

    public record Snapshot(long steps, long productionRuns, double produced, double consumed, long priceUpdates, long priceChanges, long caravansCreated, long caravansArrived,
                           long caravansLost, long ambushes, long shortages, long surpluses, long trades, long contracts, long contractsFulfilled, long tradePlans,
                           double stepMicros, double caravanMicros, double planMillis) { }

    public void produced(double q) { produced.add(q); }
    public void consumed(double q) { consumed.add(q); }

    public Snapshot snapshot() {
        return new Snapshot(steps.get(), productionRuns.get(), produced.sum(), consumed.sum(), priceUpdates.get(), priceChanges.get(), caravansCreated.get(), caravansArrived.get(),
                caravansLost.get(), ambushes.get(), shortages.get(), surpluses.get(), trades.get(), contracts.get(), contractsFulfilled.get(), tradePlans.get(),
                stepNanos.get() / 1000.0 / Math.max(1, steps.get()), caravanNanos.get() / 1000.0 / Math.max(1, steps.get()), planNanos.get() / 1e6 / Math.max(1, tradePlans.get()));
    }

    public void reset() {
        for (AtomicLong l : new AtomicLong[] {steps, productionRuns, priceChanges, priceUpdates, caravansCreated, caravansArrived, caravansLost, ambushes, shortages, surpluses, trades,
                contracts, contractsFulfilled, tradePlans, stepNanos, caravanNanos, planNanos}) l.set(0);
        produced.reset(); consumed.reset();
    }
}
