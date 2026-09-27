package yadi.samuraiai.living.quest.metrics;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import yadi.samuraiai.living.quest.branching.Path;

/** Volume of the Dynamic Quest Engine: conditions, quests generated, accepted, completed, failed, paths taken, twists, merges, campaigns, average duration. */
public final class QuestMetrics {
    public final AtomicLong conditions = new AtomicLong(), ignored = new AtomicLong(), generated = new AtomicLong(), accepted = new AtomicLong(), completed = new AtomicLong(),
            failed = new AtomicLong(), expired = new AtomicLong(), abandoned = new AtomicLong(), resolvedByWorld = new AtomicLong(), twists = new AtomicLong(), merges = new AtomicLong(),
            campaigns = new AtomicLong(), chains = new AtomicLong(), consequences = new AtomicLong(), rewards = new AtomicLong(), refusals = new AtomicLong();
    public final AtomicLong durationMinutes = new AtomicLong();
    private final Map<Path, AtomicLong> paths = new EnumMap<>(Path.class);

    public QuestMetrics() { for (Path p : Path.values()) paths.put(p, new AtomicLong()); }

    public void path(Path p) { if (p != null) paths.get(p).incrementAndGet(); }

    public record Snapshot(long conditions, long ignored, long generated, long accepted, long completed, long failed, long expired, long abandoned, long resolvedByWorld,
                           long twists, long merges, long campaigns, long chains, long consequences, long rewards, long refusals, double averageDays, Map<Path, Long> paths) { }

    public Snapshot snapshot() {
        Map<Path, Long> p = new EnumMap<>(Path.class);
        paths.forEach((k, v) -> p.put(k, v.get()));
        long ended = Math.max(1, completed.get() + failed.get());
        return new Snapshot(conditions.get(), ignored.get(), generated.get(), accepted.get(), completed.get(), failed.get(), expired.get(), abandoned.get(), resolvedByWorld.get(),
                twists.get(), merges.get(), campaigns.get(), chains.get(), consequences.get(), rewards.get(), refusals.get(), durationMinutes.get() / 1440.0 / ended, p);
    }

    public void reset() {
        for (AtomicLong l : new AtomicLong[] {conditions, ignored, generated, accepted, completed, failed, expired, abandoned, resolvedByWorld, twists, merges, campaigns, chains,
                consequences, rewards, refusals, durationMinutes}) l.set(0);
        paths.values().forEach(v -> v.set(0));
    }
}
