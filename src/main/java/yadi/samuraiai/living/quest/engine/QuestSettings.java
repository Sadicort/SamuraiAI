package yadi.samuraiai.living.quest.engine;

import java.util.List;
import yadi.samuraiai.config.RecordSettingsBuilder;

/** Every tunable value of the Dynamic Quest Engine ({@code samuraiai-quest.toml}). */
public record QuestSettings(
        double minSeverity, int cooldownMinutes, int maxOpenQuests, int maxOffersPerSettlement, double twistThreshold, double offerRadius,
        double rewardScale, int historyPerPlayer, int historyWorld, boolean campaigns, boolean chains, boolean debugLogging,
        List<String> templates) {

    public QuestSettings {
        minSeverity = c(minSeverity, 0, 1); cooldownMinutes = i(cooldownMinutes, 0, 10000000); maxOpenQuests = i(maxOpenQuests, 1, 100000); maxOffersPerSettlement = i(maxOffersPerSettlement, 1, 1000);
        twistThreshold = c(twistThreshold, 0.01, 1); offerRadius = c(offerRadius, 8, 100000); rewardScale = c(rewardScale, 0, 100);
        historyPerPlayer = i(historyPerPlayer, 8, 100000); historyWorld = i(historyWorld, 16, 1000000);
        templates = List.copyOf(templates == null ? List.of() : templates);
    }

    public static QuestSettings defaults() { return new QuestSettings(0.2, 2 * 1440, 64, 3, 0.25, 160, 1.0, 200, 1000, true, true, false, List.of()); }

    private static volatile QuestSettings current = defaults();
    public static QuestSettings current() { return current; }
    public static void apply(QuestSettings next) { current = java.util.Objects.requireNonNull(next); }
    public static Builder builder() { return new Builder(defaults()); }

    public static final class Builder extends RecordSettingsBuilder<QuestSettings, Builder> {
        private Builder(QuestSettings base) { super(QuestSettings.class, base); }
        @Override protected Builder self() { return this; }
    }

    private static double c(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
    private static int i(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
