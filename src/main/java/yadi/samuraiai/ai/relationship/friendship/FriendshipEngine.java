package yadi.samuraiai.ai.relationship.friendship;

import yadi.samuraiai.ai.cognition.model.Stamp;
import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.ai.relationship.model.FriendshipStage;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;

/**
 * Friendship is earned over time from trust, affinity, respect and the share of good moments. Each stage needs a score and a
 * minimum acquaintance time, so nobody becomes a best friend in an afternoon; stages change with hysteresis so a score hovering
 * at a boundary does not flicker. The last stage (brothers in arms) is prepared but disabled by default.
 */
public final class FriendshipEngine {
    public double score(RelationshipRecord r) {
        double positiveShare = r.interactions() == 0 ? 0.0D : (double) r.positive() / Math.max(1, r.positive() + r.negative());
        double score = 0.35D * r.trust() + 0.25D * r.affinity() + 0.20D * r.respect() + 0.20D * (positiveShare * 100.0D);
        return Math.max(0.0D, score - r.rivalry() * 0.25D);
    }

    public FriendshipStage evaluate(RelationshipRecord r, long now, RelationshipSettings s) {
        double score = score(r);
        FriendshipStage current = r.stage();
        FriendshipStage best = FriendshipStage.STRANGER;
        FriendshipStage[] stages = FriendshipStage.values();
        for (int i = 1; i < stages.length; i++) {
            FriendshipStage stage = stages[i];
            if (stage == FriendshipStage.BROTHER_IN_ARMS && (!s.brotherEnabled() || r.sharedDanger() < s.brotherSharedDanger())) continue;
            double needed = threshold(stage, s);
            // Hysteresis: an already-held stage is kept until the score falls a margin below it.
            if (stage.ordinal() <= current.ordinal()) needed -= s.friendshipHysteresis();
            long minAge = (long) s.friendshipTicksPerStage() * (i - 1);
            if (score >= needed && now - r.created() >= minAge && r.interactions() >= i) best = stage;
        }
        return best;
    }

    /** Upward movement is limited to one stage per evaluation: friendship is a path, not a jump. */
    public FriendshipStage step(RelationshipRecord r, long now, RelationshipSettings s) {
        FriendshipStage target = evaluate(r, now, s);
        FriendshipStage current = r.stage();
        if (target.ordinal() > current.ordinal()) return FriendshipStage.values()[current.ordinal() + 1];
        return target;
    }

    public static double threshold(FriendshipStage stage, RelationshipSettings s) {
        return switch (stage) {
            case STRANGER -> 0; case ACQUAINTANCE -> s.friendshipAcquaintance(); case COMPANION -> s.friendshipCompanion(); case FRIEND -> s.friendshipFriend();
            case CLOSE_FRIEND -> s.friendshipClose(); case BEST_FRIEND -> s.friendshipBest(); case BROTHER_IN_ARMS -> s.friendshipBrother();
        };
    }

    public static long ticksPerDay() { return Stamp.TICKS_PER_DAY; }
}
