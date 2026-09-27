package yadi.samuraiai.ai.relationship.affinity;

import yadi.samuraiai.ai.relationship.engine.DimensionEngine;
import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.ai.relationship.model.AffinityLevel;
import yadi.samuraiai.ai.relationship.model.Dimension;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;

/** Affinity is social comfort: it grows with pleasant time together and shared routine and follows the NPC's own sociability. */
public final class AffinityEngine extends DimensionEngine {
    public AffinityEngine() { super(Dimension.AFFINITY); }

    public static AffinityLevel level(RelationshipRecord r, RelationshipSettings s) { return level(r.affinity(), s); }

    public static AffinityLevel level(double affinity, RelationshipSettings s) {
        if (affinity >= s.affinityFriendly()) return AffinityLevel.FRIENDLY;
        if (affinity >= s.affinityComfortable()) return AffinityLevel.COMFORTABLE;
        if (affinity >= s.affinityNeutral()) return AffinityLevel.NEUTRAL;
        if (affinity >= s.affinityAwkward()) return AffinityLevel.AWKWARD;
        return AffinityLevel.AVOIDANCE;
    }
}
