package yadi.samuraiai.living.family.metrics;

import java.util.concurrent.atomic.AtomicLong;

/** Volume and cost of the Family Engine: families, births, generations, mentorships, techniques, inheritances, branches, queries and their latency. */
public final class FamilyMetrics {
    public final AtomicLong familiesCreated = new AtomicLong(), extinct = new AtomicLong(), births = new AtomicLong(), adopted = new AtomicLong(), generations = new AtomicLong(),
            mentorships = new AtomicLong(), mentorshipsCompleted = new AtomicLong(), techniquesTaught = new AtomicLong(), techniquesLost = new AtomicLong(), inheritances = new AtomicLong(),
            branches = new AtomicLong(), successions = new AtomicLong(), traditions = new AtomicLong(), rejectedLinks = new AtomicLong(), queries = new AtomicLong(), simulations = new AtomicLong();
    public final AtomicLong queryNanos = new AtomicLong(), simulationNanos = new AtomicLong();

    public record Snapshot(long familiesCreated, long extinct, long births, long adopted, long generations, long mentorships, long mentorshipsCompleted, long techniquesTaught,
                           long techniquesLost, long inheritances, long branches, long successions, long traditions, long rejectedLinks, long queries, double queryMicros,
                           long simulations, double simulationMicros) { }

    public Snapshot snapshot() {
        return new Snapshot(familiesCreated.get(), extinct.get(), births.get(), adopted.get(), generations.get(), mentorships.get(), mentorshipsCompleted.get(), techniquesTaught.get(),
                techniquesLost.get(), inheritances.get(), branches.get(), successions.get(), traditions.get(), rejectedLinks.get(), queries.get(),
                queryNanos.get() / 1000.0 / Math.max(1, queries.get()), simulations.get(), simulationNanos.get() / 1000.0 / Math.max(1, simulations.get()));
    }

    public void reset() {
        for (AtomicLong l : new AtomicLong[] {familiesCreated, extinct, births, adopted, generations, mentorships, mentorshipsCompleted, techniquesTaught, techniquesLost, inheritances,
                branches, successions, traditions, rejectedLinks, queries, simulations, queryNanos, simulationNanos}) l.set(0);
    }
}
