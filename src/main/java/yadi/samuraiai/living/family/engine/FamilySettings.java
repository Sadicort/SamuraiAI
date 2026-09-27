package yadi.samuraiai.living.family.engine;

import java.util.List;
import yadi.samuraiai.config.RecordSettingsBuilder;

/** Every tunable value of the Family, Lineage & Legacy Engine ({@code samuraiai-family.toml}). */
public record FamilySettings(
        // founding families of NPCs that arrive without one
        int ancestorDepth, int minAdultAge, int maxAdultAge, boolean groupHouseholds, double partnerAgeGap, double siblingAgeGap, double parentAgeGap, double minParentAge,
        // stages of life (years)
        int childAge, int adolescentAge, int youngAdultAge, int adultAge, int matureAge, int elderAge,
        // mentorship and knowledge
        double mentorDailyRate, double storySignificance,
        // succession weights (culture lines override)
        List<String> successionRules,
        // identity extension: which naming culture a new family's region carries ("yamato:35","ashen:20",...)
        List<String> nameCultureWeights,
        // when a family earns a house title ("Casa <apellido>"): historical importance 0..1 and a minimum of generations
        double houseImportanceThreshold, int houseMinGenerations,
        // when a person earns an epithet: cumulative |honour| attributed to them by name
        double epithetHonorThreshold,
        // when an heirloom earns an epithet of its own: its symbolic value (generations passed through + events it was part of)
        double artifactEpithetThreshold,
        // a clan below this many member families is still "forming", not yet a recognised clan
        int clanMinFamilies,
        boolean debugLogging) {

    public FamilySettings {
        ancestorDepth = i(ancestorDepth, 0, 6); minAdultAge = i(minAdultAge, 14, 100); maxAdultAge = i(maxAdultAge, minAdultAge, 120);
        partnerAgeGap = c(partnerAgeGap, 0, 60); siblingAgeGap = c(siblingAgeGap, 0, 60); parentAgeGap = c(parentAgeGap, 10, 80); minParentAge = c(minParentAge, 10, 60);
        childAge = i(childAge, 1, 20); adolescentAge = i(adolescentAge, childAge, 30); youngAdultAge = i(youngAdultAge, adolescentAge, 40); adultAge = i(adultAge, youngAdultAge, 60);
        matureAge = i(matureAge, adultAge, 90); elderAge = i(elderAge, matureAge, 120);
        mentorDailyRate = c(mentorDailyRate, 0, 1); storySignificance = c(storySignificance, 0, 1);
        successionRules = List.copyOf(successionRules == null ? List.of() : successionRules);
        nameCultureWeights = List.copyOf(nameCultureWeights == null ? List.of() : nameCultureWeights);
        houseImportanceThreshold = c(houseImportanceThreshold, 0, 1); houseMinGenerations = i(houseMinGenerations, 1, 20);
        epithetHonorThreshold = c(epithetHonorThreshold, 0.5, 100); artifactEpithetThreshold = c(artifactEpithetThreshold, 0.5, 100);
        clanMinFamilies = i(clanMinFamilies, 1, 20);
    }

    public static FamilySettings defaults() {
        return new FamilySettings(2, 18, 55, true, 15, 12, 18, 16,
                3, 12, 16, 25, 45, 60,
                0.01, 0.45,
                List.of(),
                List.of("yamato:35", "ashen:20", "western_march:20", "old_flame:15", "hollow:10"),
                0.5, 3,
                6.0,
                4.0,
                2,
                false);
    }

    private static volatile FamilySettings current = defaults();
    public static FamilySettings current() { return current; }
    public static void apply(FamilySettings next) { current = java.util.Objects.requireNonNull(next); }
    public static Builder builder() { return new Builder(defaults()); }

    public static final class Builder extends RecordSettingsBuilder<FamilySettings, Builder> {
        private Builder(FamilySettings base) { super(FamilySettings.class, base); }
        @Override protected Builder self() { return this; }
    }

    private static double c(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
    private static int i(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
