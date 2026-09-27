package yadi.samuraiai.ai.memory.engine;

import java.util.List;
import java.util.Set;
import yadi.samuraiai.config.RecordSettingsBuilder;

/**
 * Every tunable memory value, immutable and published atomically like the other engines' settings. The Forge loader is
 * {@code world.MemoryConfig} ({@code samuraiai-memory.toml}); tests build snapshots with {@link #builder()}. The list settings
 * describe data rather than the engine; an empty list means "use the built-in default".
 */
public record MemorySettings(
        // evaluation: how much each factor contributes to the score of an experience, and where it is kept at all
        double retainThreshold, double weightBase, double weightDuration, double weightRarity, double weightEmotion, double weightGoal,
        double weightParticipants, double weightPlace, double weightConsequence, double weightRelationship, double weightPersonality, double weightDanger,
        int durationCapTicks, int rarityCap,
        // score cut points for each importance level
        double cutLow, double cutNormal, double cutHigh, double cutImportant, double cutCritical, double cutLegendary,
        // repetition and capacity
        int reinforceWindowTicks, double reinforceGain, double reinforceSimilarity, int maxMemories,
        // consolidation
        int consolidationDelayTicks, int consolidationBatch, int mergeSpanTicks, double mergeSimilarity, int sleepBatch, double sleepReinforce, double consolidationBoost,
        // forgetting
        double halfLifeTicks, double scaleTrivial, double scaleLow, double scaleNormal, double scaleHigh, double scaleImportant, double scaleCritical, double scaleLegendary,
        double emotionRetention, double useRetention, int useRetentionCap, double traumaRetention, double personalityRetention,
        double fadingThreshold, double forgetThreshold, double rehearsalGain, int maintenanceBatch, int maintenanceIntervalTicks, int protectFromImportance,
        // evolution
        double evolutionHalfLifeTicks, double traumaResidual, double confidenceDriftPerDay, double reinterpretShift, int reinterpretLookback,
        // compression
        int compressionIntervalTicks, int compressionTrigger, int compressGroupMin, int compressWindowTicks, int detailAgeTicks,
        // retrieval, cache and echo
        int retrievalLimit, double fuzzyMinMatch, int shortCacheTicks, int shortCacheSize, int hotSize, int warmSize, int hotAccessThreshold,
        double echoMinIntensity, int echoCooldownTicks, double echoScale,
        // spatial and procedural memory
        int cellSize, double nodeMergeRadius, double nodeConnectRadius, double practiceGain, int protectSkillUses,
        // record size limits
        int maxChaptersPerRecord, int maxEventsPerRecord, int maxConsequences,
        boolean debugLogging,
        // data lists (empty = built-in)
        List<String> categoryTraits, List<String> protectedTags, List<String> protectedKinds) {

    public static final List<String> DEFAULT_CATEGORY_TRAITS = List.of("SOCIAL:SOCIABILITY", "COMBAT:AGGRESSION", "TRAVEL:CURIOSITY", "DISCOVERY:CURIOSITY",
            "ROUTINE:DILIGENCE", "SPIRITUAL:SPIRITUALITY", "DANGER:CAUTION", "DUTY:DISCIPLINE", "CULTURAL:LOYALTY", "KNOWLEDGE:CURIOSITY");
    public static final List<String> DEFAULT_PROTECTED_TAGS = List.of("oath", "identity", "vow", "trauma");

    public MemorySettings {
        retainThreshold = c(retainThreshold, 0, 1); weightBase = c(weightBase, 0, 2); weightDuration = c(weightDuration, 0, 2); weightRarity = c(weightRarity, 0, 2);
        weightEmotion = c(weightEmotion, 0, 2); weightGoal = c(weightGoal, 0, 2); weightParticipants = c(weightParticipants, 0, 2); weightPlace = c(weightPlace, 0, 2);
        weightConsequence = c(weightConsequence, 0, 2); weightRelationship = c(weightRelationship, 0, 2); weightPersonality = c(weightPersonality, 0, 2);
        weightDanger = c(weightDanger, 0, 2);
        durationCapTicks = i(durationCapTicks, 20, 240000); rarityCap = i(rarityCap, 1, 1000);
        cutLow = c(cutLow, 0, 1); cutNormal = c(cutNormal, cutLow, 1); cutHigh = c(cutHigh, cutNormal, 1); cutImportant = c(cutImportant, cutHigh, 1);
        cutCritical = c(cutCritical, cutImportant, 1); cutLegendary = c(cutLegendary, cutCritical, 1.5);
        reinforceWindowTicks = i(reinforceWindowTicks, 0, 240000); reinforceGain = c(reinforceGain, 0, 1); reinforceSimilarity = c(reinforceSimilarity, 0, 1);
        maxMemories = i(maxMemories, 20, 100000);
        consolidationDelayTicks = i(consolidationDelayTicks, 0, 240000); consolidationBatch = i(consolidationBatch, 1, 10000);
        mergeSpanTicks = i(mergeSpanTicks, 0, 2400000); mergeSimilarity = c(mergeSimilarity, 0, 1); sleepBatch = i(sleepBatch, 1, 100000);
        sleepReinforce = c(sleepReinforce, 0, 1); consolidationBoost = c(consolidationBoost, 0, 1);
        halfLifeTicks = c(halfLifeTicks, 1000, 1.0e9);
        scaleTrivial = c(scaleTrivial, 0.01, 1000); scaleLow = c(scaleLow, 0.01, 1000); scaleNormal = c(scaleNormal, 0.01, 1000); scaleHigh = c(scaleHigh, 0.01, 1000);
        scaleImportant = c(scaleImportant, 0.01, 1000); scaleCritical = c(scaleCritical, 0.01, 1000); scaleLegendary = c(scaleLegendary, 0.01, 10000);
        emotionRetention = c(emotionRetention, 0, 20); useRetention = c(useRetention, 0, 5); useRetentionCap = i(useRetentionCap, 0, 1000);
        traumaRetention = c(traumaRetention, 1, 100); personalityRetention = c(personalityRetention, 0, 1);
        fadingThreshold = c(fadingThreshold, 0, 1); forgetThreshold = c(forgetThreshold, 0, fadingThreshold); rehearsalGain = c(rehearsalGain, 0, 1);
        maintenanceBatch = i(maintenanceBatch, 1, 100000); maintenanceIntervalTicks = i(maintenanceIntervalTicks, 1, 240000);
        protectFromImportance = i(protectFromImportance, 0, 7);
        evolutionHalfLifeTicks = c(evolutionHalfLifeTicks, 1000, 1.0e9); traumaResidual = c(traumaResidual, 0, 1);
        confidenceDriftPerDay = c(confidenceDriftPerDay, 0, 1); reinterpretShift = c(reinterpretShift, 0, 1); reinterpretLookback = i(reinterpretLookback, 0, 200);
        compressionIntervalTicks = i(compressionIntervalTicks, 20, 2400000); compressionTrigger = i(compressionTrigger, 10, 100000);
        compressGroupMin = i(compressGroupMin, 2, 1000); compressWindowTicks = i(compressWindowTicks, 100, 2400000); detailAgeTicks = i(detailAgeTicks, 0, 24000000);
        retrievalLimit = i(retrievalLimit, 1, 1000); fuzzyMinMatch = c(fuzzyMinMatch, 0, 1); shortCacheTicks = i(shortCacheTicks, 0, 24000);
        shortCacheSize = i(shortCacheSize, 1, 10000); hotSize = i(hotSize, 1, 10000); warmSize = i(warmSize, hotSize, 100000); hotAccessThreshold = i(hotAccessThreshold, 1, 1000);
        echoMinIntensity = c(echoMinIntensity, 0, 1); echoCooldownTicks = i(echoCooldownTicks, 0, 2400000); echoScale = c(echoScale, 0, 2);
        cellSize = i(cellSize, 4, 512); nodeMergeRadius = c(nodeMergeRadius, 1, 256); nodeConnectRadius = c(nodeConnectRadius, nodeMergeRadius, 1024);
        practiceGain = c(practiceGain, 0, 1); protectSkillUses = i(protectSkillUses, 1, 100000);
        maxChaptersPerRecord = i(maxChaptersPerRecord, 1, 100); maxEventsPerRecord = i(maxEventsPerRecord, 1, 100); maxConsequences = i(maxConsequences, 1, 100);
        categoryTraits = List.copyOf(categoryTraits == null ? List.of() : categoryTraits);
        protectedTags = List.copyOf(protectedTags == null ? List.of() : protectedTags);
        protectedKinds = List.copyOf(protectedKinds == null ? List.of() : protectedKinds);
    }

    public static MemorySettings defaults() {
        return new MemorySettings(0.10, 0.34, 0.05, 0.10, 0.16, 0.06, 0.04, 0.04, 0.08, 0.07, 0.03, 0.06, 6000, 8,
                0.12, 0.28, 0.45, 0.62, 0.78, 0.92,
                1200, 0.15, 0.8, 2000,
                600, 24, 48000, 0.75, 200, 0.12, 0.10,
                120000.0, 0.2, 0.5, 1.0, 2.5, 6.0, 20.0, 100.0,
                1.5, 0.25, 10, 6.0, 0.4,
                0.35, 0.08, 0.05, 64, 1200, 5,
                96000.0, 0.35, 0.01, 0.25, 12,
                6000, 300, 5, 72000, 120000,
                8, 0.5, 40, 32, 32, 128, 3,
                0.4, 2400, 0.5,
                32, 12.0, 64.0, 0.08, 25,
                8, 6, 6,
                false,
                List.of(), List.of(), List.of());
    }

    public List<String> effectiveCategoryTraits() { return categoryTraits.isEmpty() ? DEFAULT_CATEGORY_TRAITS : categoryTraits; }
    public Set<String> effectiveProtectedTags() { return Set.copyOf(protectedTags.isEmpty() ? DEFAULT_PROTECTED_TAGS : protectedTags); }
    public Set<String> effectiveProtectedKinds() { return Set.copyOf(protectedKinds); }

    private static volatile MemorySettings current = defaults();
    public static MemorySettings current() { return current; }
    public static void apply(MemorySettings next) { current = java.util.Objects.requireNonNull(next); }
    public static Builder builder() { return new Builder(defaults()); }

    public static final class Builder extends RecordSettingsBuilder<MemorySettings, Builder> {
        private Builder(MemorySettings base) { super(MemorySettings.class, base); }
        @Override protected Builder self() { return this; }
    }

    private static double c(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
    private static int i(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
