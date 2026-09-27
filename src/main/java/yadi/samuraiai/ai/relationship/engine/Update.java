package yadi.samuraiai.ai.relationship.engine;

import java.util.Map;
import yadi.samuraiai.ai.relationship.model.Dimension;
import yadi.samuraiai.ai.relationship.model.FriendshipStage;
import yadi.samuraiai.ai.relationship.model.HonorCategory;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;
import yadi.samuraiai.ai.relationship.model.TrustLevel;

/** What applying one piece of evidence did: the record, whether it is new, the net change per axis and the level transitions. */
public record Update(RelationshipRecord record, boolean created, Map<Dimension, Double> deltas, TrustLevel trustBefore, TrustLevel trustAfter,
                     HonorCategory honorAfter, FriendshipStage stageBefore, FriendshipStage stageAfter, boolean loyaltyBroken) {
    public Update { deltas = Map.copyOf(deltas); }
    public double delta(Dimension d) { return deltas.getOrDefault(d, 0.0D); }
    public boolean stageChanged() { return stageBefore != stageAfter; }
}
