package yadi.samuraiai.ai.relationship.honor;

import yadi.samuraiai.ai.relationship.engine.DimensionEngine;
import yadi.samuraiai.ai.relationship.engine.Modulation;
import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.ai.relationship.model.Dimension;
import yadi.samuraiai.ai.relationship.model.HonorCategory;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;

/**
 * The honor an NPC has observed in someone, by the NPC's own cultural reading (the evidence carries a culture scale that
 * strengthens or softens honor effects). It is not universal morality. Oath-breaking is remembered separately and keeps the
 * label "oath breaker" attached until the person has redeemed themselves.
 */
public final class HonorSystem extends DimensionEngine {
    public HonorSystem() { super(Dimension.HONOR); }

    @Override protected double adjust(double delta, boolean up, RelationshipRecord r, Modulation m, RelationshipSettings s) { return delta * m.evidence().honorScale(); }

    public static HonorCategory category(RelationshipRecord r, RelationshipSettings s) {
        double honor = r.honor();
        if (honor >= s.honorLegendary() && r.respect() >= s.respectMaster()) return HonorCategory.LEGENDARY_WARRIOR;
        if (r.oathsBroken() > r.oathsKept() && honor < s.honorHonorable()) return HonorCategory.OATH_BREAKER;
        if (honor >= s.honorHonorable()) return HonorCategory.HONORABLE;
        if (honor >= s.honorQuestionable()) return HonorCategory.NEUTRAL;
        if (honor >= s.honorDishonorable()) return HonorCategory.QUESTIONABLE;
        return HonorCategory.DISHONORABLE;
    }
}
