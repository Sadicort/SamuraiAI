package yadi.samuraiai.ai.relationship.trust;

import yadi.samuraiai.ai.relationship.engine.DimensionEngine;
import yadi.samuraiai.ai.relationship.engine.Modulation;
import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.ai.relationship.model.Dimension;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;
import yadi.samuraiai.ai.relationship.model.TrustLevel;

/** Trust is slow to win and quick to lose: gains are scaled down and losses scaled up. Levels come from the configured thresholds. */
public final class TrustEngine extends DimensionEngine {
    public TrustEngine() { super(Dimension.TRUST); }

    @Override protected double adjust(double delta, boolean up, RelationshipRecord r, Modulation m, RelationshipSettings s) {
        return up ? delta * s.trustGainScale() : delta * s.trustLossBias();
    }

    public static TrustLevel level(RelationshipRecord r, RelationshipSettings s) { return level(r.trust(), r.interactions(), s); }

    public static TrustLevel level(double trust, int interactions, RelationshipSettings s) {
        // Too little contact to have an opinion, unless what little there was already moved it (a betrayal is not "unknown").
        if (interactions < s.knownAfterInteractions() && Math.abs(trust - s.initialTrust()) < 5.0D) return TrustLevel.UNKNOWN;
        if (trust < s.trustSuspicious()) return TrustLevel.SUSPICIOUS;
        if (trust < s.trustNeutral()) return TrustLevel.NEUTRAL;
        if (trust < s.trustTrusting()) return TrustLevel.TRUSTING;
        if (trust < s.trustClose()) return TrustLevel.CLOSE;
        return trust >= s.trustAbsolute() ? TrustLevel.ABSOLUTE_TRUST : TrustLevel.CLOSE;
    }
}
