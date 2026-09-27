package yadi.samuraiai.ai.knowledge.engine;

import java.util.List;
import yadi.samuraiai.config.RecordSettingsBuilder;

/** Every tunable knowledge and society value ({@code samuraiai-knowledge.toml}). The {@code cultures} list is world data; empty means the built-in cultures. */
public record KnowledgeSettings(
        // starting confidence by way of learning
        double confObservation, double confExperience, double confConversation, double confTeaching, double confRumor, double confDeduction, double confCulture, double confPublic, double confAdmin,
        // validation
        double likelyAt, double verifiedAt, double falseAt, int independentForLikely, int independentForVerified, double trustedSource, double contradictionPenalty, double confirmationBoost,
        // forgetting
        double halfLifeTicks, double rumorHalfLifeTicks, double verifiedDecayScale, double forgetBelow, int decayIntervalTicks, int decayBatch, int maxKnowledge, int maxRevisions,
        // learning, teaching, discovery
        double learnCuriosity, double learnDiscipline, double attentionFloor, double teachRespect, double teachTrust, double teachKnowledge, double teachAttention,
        double teachMinQuality, double teachConfidenceScale, double studentTrustsAt, double discoveryNovelty, double discoveryMinImportance,
        // rumours
        double rumorHopDecay, double distortionPerHop, double distortionMax, double rumorMinCredibility, int maxRumorsPerNpc, int maxRumors, double rumorTellThreshold, int maxHops,
        // propagation and gossip
        int propagationBudget, int queueMax, int baseDelayTicks, double delayPerBlock, double communitySizeDelay, double gossipRadius, int gossipCooldownTicks, int gossipMaxItems,
        // society
        int maxCommunities, int historyMax, int worldTimelineMax, double collectiveFraction, int collectiveMin, double memberJoinRadius, boolean autoVillage,
        double publicSignificance, double traditionReinforce, double traditionDecayPerDay, int societyIntervalTicks,
        // graph
        int pathMaxDepth, int queryLimit,
        boolean debugLogging,
        // "id|name|traditions|norms" (see CultureCatalog). Empty = built-in cultures.
        List<String> cultures) {

    public KnowledgeSettings {
        confObservation = c(confObservation, 0, 1); confExperience = c(confExperience, 0, 1); confConversation = c(confConversation, 0, 1); confTeaching = c(confTeaching, 0, 1);
        confRumor = c(confRumor, 0, 1); confDeduction = c(confDeduction, 0, 1); confCulture = c(confCulture, 0, 1); confPublic = c(confPublic, 0, 1); confAdmin = c(confAdmin, 0, 1);
        falseAt = c(falseAt, 0, 1); likelyAt = c(likelyAt, falseAt, 1); verifiedAt = c(verifiedAt, likelyAt, 1);
        independentForLikely = i(independentForLikely, 1, 100); independentForVerified = i(independentForVerified, independentForLikely, 100);
        trustedSource = c(trustedSource, 0, 1); contradictionPenalty = c(contradictionPenalty, 0, 1); confirmationBoost = c(confirmationBoost, 0, 1);
        halfLifeTicks = c(halfLifeTicks, 1000, 1.0e10); rumorHalfLifeTicks = c(rumorHalfLifeTicks, 1000, 1.0e10); verifiedDecayScale = c(verifiedDecayScale, 1, 1000);
        forgetBelow = c(forgetBelow, 0, 1); decayIntervalTicks = i(decayIntervalTicks, 1, 2400000); decayBatch = i(decayBatch, 1, 100000);
        maxKnowledge = i(maxKnowledge, 10, 1000000); maxRevisions = i(maxRevisions, 1, 100);
        learnCuriosity = c(learnCuriosity, 0, 2); learnDiscipline = c(learnDiscipline, 0, 2); attentionFloor = c(attentionFloor, 0, 1);
        teachRespect = c(teachRespect, 0, 1); teachTrust = c(teachTrust, 0, 1); teachKnowledge = c(teachKnowledge, 0, 1); teachAttention = c(teachAttention, 0, 1);
        teachMinQuality = c(teachMinQuality, 0, 1); teachConfidenceScale = c(teachConfidenceScale, 0, 1); studentTrustsAt = c(studentTrustsAt, 0, 100);
        discoveryNovelty = c(discoveryNovelty, 0, 1); discoveryMinImportance = c(discoveryMinImportance, 0, 1);
        rumorHopDecay = c(rumorHopDecay, 0, 1); distortionPerHop = c(distortionPerHop, 0, 1); distortionMax = c(distortionMax, 0, 1); rumorMinCredibility = c(rumorMinCredibility, 0, 1);
        maxRumorsPerNpc = i(maxRumorsPerNpc, 1, 100000); maxRumors = i(maxRumors, 1, 1000000); rumorTellThreshold = c(rumorTellThreshold, 0, 1); maxHops = i(maxHops, 1, 1000);
        propagationBudget = i(propagationBudget, 1, 10000); queueMax = i(queueMax, 1, 1000000); baseDelayTicks = i(baseDelayTicks, 0, 2400000);
        delayPerBlock = c(delayPerBlock, 0, 1000); communitySizeDelay = c(communitySizeDelay, 0, 100); gossipRadius = c(gossipRadius, 1, 256);
        gossipCooldownTicks = i(gossipCooldownTicks, 0, 2400000); gossipMaxItems = i(gossipMaxItems, 1, 100);
        maxCommunities = i(maxCommunities, 1, 100000); historyMax = i(historyMax, 1, 1000000); worldTimelineMax = i(worldTimelineMax, 1, 1000000);
        collectiveFraction = c(collectiveFraction, 0, 1); collectiveMin = i(collectiveMin, 1, 1000); memberJoinRadius = c(memberJoinRadius, 1, 4096);
        publicSignificance = c(publicSignificance, 0, 1); traditionReinforce = c(traditionReinforce, 0, 1); traditionDecayPerDay = c(traditionDecayPerDay, 0, 1);
        societyIntervalTicks = i(societyIntervalTicks, 1, 2400000);
        pathMaxDepth = i(pathMaxDepth, 1, 12); queryLimit = i(queryLimit, 1, 100000);
        cultures = List.copyOf(cultures == null ? List.of() : cultures);
    }

    public static KnowledgeSettings defaults() {
        return new KnowledgeSettings(0.85, 0.9, 0.6, 0.75, 0.3, 0.5, 0.8, 0.8, 1.0,
                0.55, 0.85, 0.15, 2, 3, 0.8, 0.4, 0.3,
                2400000.0, 240000.0, 4.0, 0.08, 6000, 64, 3000, 6,
                0.5, 0.3, 0.3, 0.3, 0.25, 0.3, 0.15, 0.35, 0.9, 45.0, 0.4, 0.3,
                0.85, 0.08, 0.5, 0.2, 100, 1000, 0.3, 8,
                4, 500, 200, 4.0, 0.05, 10.0, 1200, 2,
                64, 500, 400, 0.5, 2, 64.0, true, 0.5, 0.05, 0.01, 1200,
                4, 200,
                false, List.of());
    }

    private static volatile KnowledgeSettings current = defaults();
    public static KnowledgeSettings current() { return current; }
    public static void apply(KnowledgeSettings next) { current = java.util.Objects.requireNonNull(next); }
    public static Builder builder() { return new Builder(defaults()); }

    public static final class Builder extends RecordSettingsBuilder<KnowledgeSettings, Builder> {
        private Builder(KnowledgeSettings base) { super(KnowledgeSettings.class, base); }
        @Override protected Builder self() { return this; }
    }

    public double confidenceFor(yadi.samuraiai.ai.knowledge.model.LearnMethod method) {
        return switch (method) {
            case OBSERVATION -> confObservation; case EXPERIENCE -> confExperience; case CONVERSATION -> confConversation; case TEACHING -> confTeaching;
            case RUMOR -> confRumor; case DEDUCTION -> confDeduction; case CULTURE -> confCulture; case PUBLIC_EVENT -> confPublic; case ADMIN -> confAdmin;
        };
    }

    private static double c(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
    private static int i(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
