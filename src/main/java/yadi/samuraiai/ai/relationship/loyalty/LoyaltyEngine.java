package yadi.samuraiai.ai.relationship.loyalty;

import yadi.samuraiai.ai.relationship.engine.DimensionEngine;
import yadi.samuraiai.ai.relationship.engine.Modulation;
import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.ai.relationship.model.Dimension;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;

/** Loyalty grows slowly and only on a base of trust; a large enough blow breaks it, and it must be re-earned. */
public final class LoyaltyEngine extends DimensionEngine {
    public LoyaltyEngine() { super(Dimension.LOYALTY); }

    @Override protected double adjust(double delta, boolean up, RelationshipRecord r, Modulation m, RelationshipSettings s) {
        if (up && r.trust() < s.loyaltyMinTrust()) return delta * s.loyaltyLowGainScale();
        return delta * (up ? 0.6D : 1.0D);
    }

    /** Whether a loss of this size breaks the loyalty (when there was any to break). */
    public static boolean breaks(double before, double loss, RelationshipSettings s) { return before >= 20.0D && loss >= s.loyaltyBreakDelta(); }
}
