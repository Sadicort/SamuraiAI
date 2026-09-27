package yadi.samuraiai.ai.emotion.engine;

import java.util.List;
import yadi.samuraiai.config.RecordSettingsBuilder;

/** Every tunable emotion value ({@code samuraiai-emotion.toml}). */
public record EmotionSettings(
        // records
        int maxActive, double minIntensityKeep, double fusionGain, int historySize, int maxCauses, int maxRelated,
        // decay
        double linearPerTick, double exponentialHalfLife, double traumaHalfLife, double hopeHalfLife, double positiveDecayScale, double negativeDecayScale,
        double traumaFloorFraction,
        // techniques (decay multipliers) and recovery
        double breatheBoost, double meditateBoost, double sleepBoost, double talkBoost, double isolateBoost, double restBoost, double passiveRegulation,
        int regulationInterval, double regulationThreshold,
        // mood
        double moodInertia, double moodSwitchMargin, int moodMinTicks, int moodUpdateTicks, double moodBaseline,
        // blending
        double blendMinIntensity, double blendRatio,
        // trauma and recovery
        double traumaThreshold, double traumaMinWeight, int maxTraumas, double recoveryPerDay, double recoverySleep, double recoveryMeditation,
        double recoveryFriendship, double recoveryPositive, double recoveryConversation, double traumaRecoveredAt, double traumaEchoIntensity, int traumaEchoCooldown,
        // resilience
        double resilienceDiscipline, double resilienceCourage, double resilienceExperience, double resilienceHope, double resiliencePenalty,
        // contagion
        double contagionRadius, double contagionMinIntensity, double contagionFactor, int contagionCooldown, int contagionMaxTargets,
        // body and speech
        double expressionThreshold, double fearSpeedBoost, double sadnessSpeedDrop, int reactionDelayMax,
        boolean debugLogging,
        // "TRAIT|EMOTION|G/D|factor": G bends how strongly an emotion is triggered, D how fast it decays ({@code 1 + factor * lean}). Empty = built-in.
        List<String> personalityRules) {

    public static final List<String> DEFAULT_RULES = List.of(
            "CURIOSITY|CURIOSITY|G|0.6", "CURIOSITY|SURPRISE|G|0.4", "COURAGE|FEAR|G|-0.5", "COURAGE|ANXIETY|G|-0.3", "PATIENCE|ANGER|D|0.6",
            "PRIDE|SHAME|D|-0.5", "PRIDE|SHAME|G|0.3", "EMPATHY|COMPASSION|G|0.5", "SPIRITUALITY|CALM|G|0.3", "DISCIPLINE|DETERMINATION|G|0.4",
            "AGGRESSION|ANGER|G|0.4", "CAUTION|ANXIETY|G|0.4", "SOCIABILITY|LONELINESS|G|0.5", "SPIRITUALITY|SADNESS|D|0.3");

    public EmotionSettings {
        maxActive = i(maxActive, 2, 200); minIntensityKeep = c(minIntensityKeep, 0, 50); fusionGain = c(fusionGain, 0, 1); historySize = i(historySize, 1, 1000);
        maxCauses = i(maxCauses, 1, 100); maxRelated = i(maxRelated, 1, 100);
        linearPerTick = c(linearPerTick, 0, 10); exponentialHalfLife = c(exponentialHalfLife, 20, 1.0e9); traumaHalfLife = c(traumaHalfLife, 20, 1.0e10);
        hopeHalfLife = c(hopeHalfLife, 20, 1.0e9); positiveDecayScale = c(positiveDecayScale, 0.05, 20); negativeDecayScale = c(negativeDecayScale, 0.05, 20);
        traumaFloorFraction = c(traumaFloorFraction, 0, 1);
        breatheBoost = c(breatheBoost, 1, 50); meditateBoost = c(meditateBoost, 1, 50); sleepBoost = c(sleepBoost, 1, 50); talkBoost = c(talkBoost, 1, 50);
        isolateBoost = c(isolateBoost, 1, 50); restBoost = c(restBoost, 1, 50); passiveRegulation = c(passiveRegulation, 0, 5);
        regulationInterval = i(regulationInterval, 1, 24000); regulationThreshold = c(regulationThreshold, 0, 500);
        moodInertia = c(moodInertia, 0, 0.999); moodSwitchMargin = c(moodSwitchMargin, 0, 1); moodMinTicks = i(moodMinTicks, 0, 2400000);
        moodUpdateTicks = i(moodUpdateTicks, 1, 24000); moodBaseline = c(moodBaseline, 0, 1);
        blendMinIntensity = c(blendMinIntensity, 0, 100); blendRatio = c(blendRatio, 0, 1);
        traumaThreshold = c(traumaThreshold, 0, 100); traumaMinWeight = c(traumaMinWeight, 0, 1); maxTraumas = i(maxTraumas, 1, 500);
        recoveryPerDay = c(recoveryPerDay, 0, 5); recoverySleep = c(recoverySleep, 0, 1); recoveryMeditation = c(recoveryMeditation, 0, 1);
        recoveryFriendship = c(recoveryFriendship, 0, 1); recoveryPositive = c(recoveryPositive, 0, 1); recoveryConversation = c(recoveryConversation, 0, 1);
        traumaRecoveredAt = c(traumaRecoveredAt, 0.1, 1); traumaEchoIntensity = c(traumaEchoIntensity, 0, 1); traumaEchoCooldown = i(traumaEchoCooldown, 0, 2400000);
        resilienceDiscipline = c(resilienceDiscipline, 0, 1); resilienceCourage = c(resilienceCourage, 0, 1); resilienceExperience = c(resilienceExperience, 0, 1);
        resilienceHope = c(resilienceHope, 0, 1); resiliencePenalty = c(resiliencePenalty, 0, 1);
        contagionRadius = c(contagionRadius, 1, 128); contagionMinIntensity = c(contagionMinIntensity, 0, 100); contagionFactor = c(contagionFactor, 0, 1);
        contagionCooldown = i(contagionCooldown, 0, 240000); contagionMaxTargets = i(contagionMaxTargets, 1, 100);
        expressionThreshold = c(expressionThreshold, 0, 100); fearSpeedBoost = c(fearSpeedBoost, 0, 2); sadnessSpeedDrop = c(sadnessSpeedDrop, 0, 1); reactionDelayMax = i(reactionDelayMax, 0, 200);
        personalityRules = List.copyOf(personalityRules == null ? List.of() : personalityRules);
    }

    public static EmotionSettings defaults() {
        return new EmotionSettings(12, 3.0, 0.5, 40, 10, 8,
                0.01, 2400.0, 240000.0, 24000.0, 1.5, 1.0, 0.25,
                2.0, 2.5, 3.0, 1.8, 1.3, 1.5, 0.15, 200, 45.0,
                0.9, 0.15, 1200, 100, 0.15,
                20.0, 0.5,
                80.0, 0.7, 20, 0.05, 0.04, 0.03, 0.02, 0.02, 0.015, 0.98, 0.5, 2400,
                0.3, 0.3, 0.2, 0.2, 0.1,
                12.0, 35.0, 0.35, 200, 6,
                30.0, 0.25, 0.2, 12,
                false, List.of());
    }

    public List<String> effectiveRules() { return personalityRules.isEmpty() ? DEFAULT_RULES : personalityRules; }

    private static volatile EmotionSettings current = defaults();
    public static EmotionSettings current() { return current; }
    public static void apply(EmotionSettings next) { current = java.util.Objects.requireNonNull(next); }
    public static Builder builder() { return new Builder(defaults()); }

    public static final class Builder extends RecordSettingsBuilder<EmotionSettings, Builder> {
        private Builder(EmotionSettings base) { super(EmotionSettings.class, base); }
        @Override protected Builder self() { return this; }
    }

    private static double c(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
    private static int i(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
