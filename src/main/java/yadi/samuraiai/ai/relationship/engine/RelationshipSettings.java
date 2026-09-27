package yadi.samuraiai.ai.relationship.engine;

import java.util.List;
import yadi.samuraiai.config.RecordSettingsBuilder;

/** Every tunable relationship value ({@code samuraiai-relationship.toml}). See {@link yadi.samuraiai.ai.memory.engine.MemorySettings} for the conventions. */
public record RelationshipSettings(
        // starting point of a new relationship
        double initialTrust, double initialRespect, double initialAffinity, double initialFear, double initialLoyalty, double initialRivalry, double initialHonor,
        // how evidence moves the axes
        double effectScale, double observedScale, double headroomFloor, double trustLossBias, double trustGainScale, double moodInfluenceCap, double hearsayInfluence,
        // level thresholds
        double trustSuspicious, double trustNeutral, double trustTrusting, double trustClose, double trustAbsolute,
        double respectLow, double respectModerate, double respectHigh, double respectMaster, double respectLegendary,
        double honorHonorable, double honorQuestionable, double honorDishonorable, double honorLegendary,
        double affinityFriendly, double affinityComfortable, double affinityNeutral, double affinityAwkward,
        double rivalryMinor, double rivalryGrowing, double rivalryMajor, double rivalryNemesis,
        // friendship
        double friendshipAcquaintance, double friendshipCompanion, double friendshipFriend, double friendshipClose, double friendshipBest, double friendshipBrother,
        double friendshipHysteresis, int friendshipTicksPerStage, boolean brotherEnabled, int brotherSharedDanger,
        // loyalty and rivalry rules
        double loyaltyMinTrust, double loyaltyLowGainScale, double loyaltyBreakDelta, double rivalryFriendBlock, int knownAfterInteractions,
        // cooling
        int coolingAfterTicks, int dormantAfterTicks, double trustHalfLife, double affinityHalfLife, double respectHalfLife, double fearHalfLife,
        double loyaltyHalfLife, double rivalryHalfLife, int decayIntervalTicks, int decayBatch,
        // capacity and history
        int maxRelationships, int maxHistory, int maxMemoryLinks, int maxCauses, double eventMinDelta, int maxPromises, int promiseDefaultTicks,
        // promises
        double promiseKeptTrust, double promiseKeptRespect, double promiseKeptHonor, double promiseBrokenTrust, double promiseBrokenHonor,
        double promiseBrokenLoyalty, double promiseExpiredTrust,
        // reputation
        double reputationDecayPerHop, double reputationMinCredibility, double reputationIndependentBonus, double reputationHalfLife,
        double credTrust, double credRepute, double credEvidence, double credRelation,
        // factions
        double factionAllied, double factionHostile,
        boolean debugLogging,
        // "TRAIT|DIMENSION|G|L|B|factor": how a personality trait bends gains (G), losses (L) or both (B) on an axis. Empty = built-in rules.
        List<String> traitRules) {

    public static final List<String> DEFAULT_TRAIT_RULES = List.of(
            "CAUTION|TRUST|G|-0.35", "CAUTION|TRUST|L|0.35", "EMPATHY|AFFINITY|G|0.40", "SOCIABILITY|AFFINITY|G|0.40", "LOYALTY|LOYALTY|G|0.50",
            "COURAGE|FEAR|G|-0.60", "AGGRESSION|RIVALRY|G|0.50", "PRIDE|RIVALRY|G|0.30", "DISCIPLINE|RESPECT|G|0.25", "PATIENCE|RIVALRY|G|-0.30",
            "PRIDE|HONOR|L|0.30", "EMPATHY|TRUST|G|0.20");

    public RelationshipSettings {
        initialTrust = c(initialTrust, 0, 100); initialRespect = c(initialRespect, 0, 100); initialAffinity = c(initialAffinity, 0, 100);
        initialFear = c(initialFear, 0, 100); initialLoyalty = c(initialLoyalty, 0, 100); initialRivalry = c(initialRivalry, 0, 100); initialHonor = c(initialHonor, 0, 100);
        effectScale = c(effectScale, 0, 10); observedScale = c(observedScale, 0, 2); headroomFloor = c(headroomFloor, 0, 1); trustLossBias = c(trustLossBias, 0, 10);
        trustGainScale = c(trustGainScale, 0, 10); moodInfluenceCap = c(moodInfluenceCap, 0, 0.9); hearsayInfluence = c(hearsayInfluence, 0, 1);
        trustSuspicious = c(trustSuspicious, 0, 100); trustNeutral = c(trustNeutral, trustSuspicious, 100); trustTrusting = c(trustTrusting, trustNeutral, 100);
        trustClose = c(trustClose, trustTrusting, 100); trustAbsolute = c(trustAbsolute, trustClose, 100);
        respectLow = c(respectLow, 0, 100); respectModerate = c(respectModerate, respectLow, 100); respectHigh = c(respectHigh, respectModerate, 100);
        respectMaster = c(respectMaster, respectHigh, 100); respectLegendary = c(respectLegendary, respectMaster, 100);
        honorDishonorable = c(honorDishonorable, 0, 100); honorQuestionable = c(honorQuestionable, honorDishonorable, 100);
        honorHonorable = c(honorHonorable, honorQuestionable, 100); honorLegendary = c(honorLegendary, honorHonorable, 100);
        affinityAwkward = c(affinityAwkward, 0, 100); affinityNeutral = c(affinityNeutral, affinityAwkward, 100); affinityComfortable = c(affinityComfortable, affinityNeutral, 100);
        affinityFriendly = c(affinityFriendly, affinityComfortable, 100);
        rivalryMinor = c(rivalryMinor, 0, 100); rivalryGrowing = c(rivalryGrowing, rivalryMinor, 100); rivalryMajor = c(rivalryMajor, rivalryGrowing, 100); rivalryNemesis = c(rivalryNemesis, rivalryMajor, 100);
        friendshipAcquaintance = c(friendshipAcquaintance, 0, 100); friendshipCompanion = c(friendshipCompanion, friendshipAcquaintance, 100);
        friendshipFriend = c(friendshipFriend, friendshipCompanion, 100); friendshipClose = c(friendshipClose, friendshipFriend, 100);
        friendshipBest = c(friendshipBest, friendshipClose, 100); friendshipBrother = c(friendshipBrother, friendshipBest, 100);
        friendshipHysteresis = c(friendshipHysteresis, 0, 20); friendshipTicksPerStage = i(friendshipTicksPerStage, 0, 24000000); brotherSharedDanger = i(brotherSharedDanger, 0, 1000);
        loyaltyMinTrust = c(loyaltyMinTrust, 0, 100); loyaltyLowGainScale = c(loyaltyLowGainScale, 0, 1); loyaltyBreakDelta = c(loyaltyBreakDelta, 0, 100);
        rivalryFriendBlock = c(rivalryFriendBlock, 0, 1); knownAfterInteractions = i(knownAfterInteractions, 0, 100);
        coolingAfterTicks = i(coolingAfterTicks, 0, 240000000); dormantAfterTicks = i(dormantAfterTicks, coolingAfterTicks, 480000000);
        trustHalfLife = c(trustHalfLife, 1000, 1.0e10); affinityHalfLife = c(affinityHalfLife, 1000, 1.0e10); respectHalfLife = c(respectHalfLife, 1000, 1.0e10);
        fearHalfLife = c(fearHalfLife, 1000, 1.0e10); loyaltyHalfLife = c(loyaltyHalfLife, 1000, 1.0e10); rivalryHalfLife = c(rivalryHalfLife, 1000, 1.0e10);
        decayIntervalTicks = i(decayIntervalTicks, 1, 2400000); decayBatch = i(decayBatch, 1, 100000);
        maxRelationships = i(maxRelationships, 4, 100000); maxHistory = i(maxHistory, 1, 1000); maxMemoryLinks = i(maxMemoryLinks, 1, 1000); maxCauses = i(maxCauses, 1, 200);
        eventMinDelta = c(eventMinDelta, 0, 100); maxPromises = i(maxPromises, 1, 10000); promiseDefaultTicks = i(promiseDefaultTicks, 20, 240000000);
        promiseKeptTrust = c(promiseKeptTrust, 0, 100); promiseKeptRespect = c(promiseKeptRespect, 0, 100); promiseKeptHonor = c(promiseKeptHonor, 0, 100);
        promiseBrokenTrust = c(promiseBrokenTrust, 0, 100); promiseBrokenHonor = c(promiseBrokenHonor, 0, 100); promiseBrokenLoyalty = c(promiseBrokenLoyalty, 0, 100);
        promiseExpiredTrust = c(promiseExpiredTrust, 0, 100);
        reputationDecayPerHop = c(reputationDecayPerHop, 0, 1); reputationMinCredibility = c(reputationMinCredibility, 0, 1);
        reputationIndependentBonus = c(reputationIndependentBonus, 0, 1); reputationHalfLife = c(reputationHalfLife, 1000, 1.0e10);
        credTrust = c(credTrust, 0, 1); credRepute = c(credRepute, 0, 1); credEvidence = c(credEvidence, 0, 1); credRelation = c(credRelation, 0, 1);
        factionAllied = c(factionAllied, 0, 100); factionHostile = c(factionHostile, 0, factionAllied);
        traitRules = List.copyOf(traitRules == null ? List.of() : traitRules);
    }

    public static RelationshipSettings defaults() {
        return new RelationshipSettings(35, 20, 40, 0, 0, 0, 50,
                1.0, 0.5, 0.15, 1.4, 0.9, 0.25, 0.25,
                25, 45, 65, 82, 94, 15, 40, 65, 85, 96, 65, 40, 25, 92, 70, 50, 35, 20, 15, 35, 60, 85,
                18, 33, 48, 66, 82, 92, 3.0, 12000, false, 3,
                55, 0.2, 25, 0.3, 2,
                72000, 720000, 480000, 360000, 2400000, 96000, 1200000, 240000, 1200, 32,
                200, 24, 24, 12, 2.0, 50, 48000,
                12, 6, 8, 30, 25, 12, 8,
                0.7, 0.15, 0.15, 960000, 0.4, 0.25, 0.25, 0.1,
                55, 20, false, List.of());
    }

    public List<String> effectiveTraitRules() { return traitRules.isEmpty() ? DEFAULT_TRAIT_RULES : traitRules; }

    private static volatile RelationshipSettings current = defaults();
    public static RelationshipSettings current() { return current; }
    public static void apply(RelationshipSettings next) { current = java.util.Objects.requireNonNull(next); }
    public static Builder builder() { return new Builder(defaults()); }

    public static final class Builder extends RecordSettingsBuilder<RelationshipSettings, Builder> {
        private Builder(RelationshipSettings base) { super(RelationshipSettings.class, base); }
        @Override protected Builder self() { return this; }
    }

    private static double c(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
    private static int i(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
