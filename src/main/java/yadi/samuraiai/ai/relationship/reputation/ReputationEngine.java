package yadi.samuraiai.ai.relationship.reputation;

import java.util.UUID;
import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.ai.relationship.model.ReputationLabel;
import yadi.samuraiai.ai.relationship.model.ReputationRecord;
import yadi.samuraiai.ai.relationship.model.ReputationScope;

/**
 * Reputation is shared social information: what an NPC saw itself, was told, or knows of public events. How far a report is
 * believed depends on how much the NPC trusts the teller, the teller's own standing, how well evidenced it is, the bond with
 * the subject, and how many independent sources agree. A repeated report from the same source never counts twice.
 */
public final class ReputationEngine {
    public double credibility(double sourceTrust, double sourceRepute, double evidence, double relationBias, int independentConfirmations, RelationshipSettings s) {
        double c = s.credTrust() * clamp(sourceTrust) + s.credRepute() * clamp(sourceRepute) + s.credEvidence() * clamp(evidence) + s.credRelation() * clamp(relationBias);
        double weights = s.credTrust() + s.credRepute() + s.credEvidence() + s.credRelation();
        c = weights <= 0 ? 0.5D : c / weights;
        c += s.reputationIndependentBonus() * Math.min(3, Math.max(0, independentConfirmations));
        return clamp(c);
    }

    /** Folds one report into the record. Returns true when the record changed. */
    public boolean observe(ReputationRecord r, ReputationLabel label, double strength, double credibility, UUID source, boolean direct, long now, RelationshipSettings s) {
        if (label == ReputationLabel.UNKNOWN || (!direct && credibility < s.reputationMinCredibility())) return false;
        boolean independent = source != null && r.sources().add(source);
        double weight = strength * (direct ? 1.0D : credibility);
        if (!direct && !independent && source != null) weight *= 0.2D;
        double before = r.score(label);
        r.scores().put(label, before + weight * (1.0D - before));
        double conf = r.confidence();
        r.confidence(1.0D - (1.0D - conf) * (1.0D - (direct ? 0.7D : 0.5D * credibility)));
        if (direct) r.direct(r.direct() + 1); else r.hearsay(r.hearsay() + 1);
        r.updated(now);
        return true;
    }

    /** Reports fade: scores relax towards zero with a long half-life. */
    public void decay(ReputationRecord r, long now, RelationshipSettings s) {
        long elapsed = now - r.updated();
        if (elapsed <= 0) return;
        double keep = Math.pow(0.5D, elapsed / s.reputationHalfLife());
        r.scores().replaceAll((k, v) -> v * keep);
        r.updated(now);
    }

    /** How much of a report survives one more hop from mouth to mouth. */
    public double hop(double strength, int hops, RelationshipSettings s) { return strength * Math.pow(s.reputationDecayPerHop(), Math.max(0, hops)); }

    /** The scope a report about an event reaches, from how public it was. */
    public static ReputationScope scopeFor(boolean publicEvent, boolean sameCommunity) { return publicEvent ? (sameCommunity ? ReputationScope.VILLAGE : ReputationScope.GLOBAL) : ReputationScope.LOCAL; }

    private static double clamp(double v) { return Double.isFinite(v) ? Math.max(0.0D, Math.min(1.0D, v)) : 0.0D; }
}
