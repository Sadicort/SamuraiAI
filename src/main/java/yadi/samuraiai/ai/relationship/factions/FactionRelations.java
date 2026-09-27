package yadi.samuraiai.ai.relationship.factions;

import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.ai.relationship.model.FactionStanding;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;

/** How an NPC stands with a faction (temples, villages, merchants, guards, bandits): allied, neutral or hostile, from the same axes as a person. */
public final class FactionRelations {
    public static double score(RelationshipRecord r) {
        return Math.max(0.0D, Math.min(100.0D, r.trust() * 0.5D + r.affinity() * 0.3D + r.loyalty() * 0.2D - r.fear() * 0.2D - r.rivalry() * 0.6D));
    }

    public static FactionStanding standing(RelationshipRecord r, RelationshipSettings s) {
        double score = score(r);
        if (score >= s.factionAllied()) return FactionStanding.ALLIED;
        if (score <= s.factionHostile()) return FactionStanding.HOSTILE;
        return FactionStanding.NEUTRAL;
    }
}
