package yadi.samuraiai.ai.relationship.respect;

import yadi.samuraiai.ai.relationship.engine.DimensionEngine;
import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.ai.relationship.model.Dimension;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;
import yadi.samuraiai.ai.relationship.model.RespectLevel;

/** Respect is independent of liking: an enemy can be respected and a friend can lose respect. It follows what the NPC has seen of skill, courage and discipline. */
public final class RespectEngine extends DimensionEngine {
    public RespectEngine() { super(Dimension.RESPECT); }

    public static RespectLevel level(RelationshipRecord r, RelationshipSettings s) { return level(r.respect(), s); }

    public static RespectLevel level(double respect, RelationshipSettings s) {
        if (respect >= s.respectLegendary()) return RespectLevel.LEGENDARY;
        if (respect >= s.respectMaster()) return RespectLevel.MASTER;
        if (respect >= s.respectHigh()) return RespectLevel.HIGH;
        if (respect >= s.respectModerate()) return RespectLevel.MODERATE;
        if (respect >= s.respectLow()) return RespectLevel.LOW;
        return RespectLevel.NONE;
    }
}
