package yadi.samuraiai.ai.relationship.rivalry;

import yadi.samuraiai.ai.relationship.engine.DimensionEngine;
import yadi.samuraiai.ai.relationship.engine.Modulation;
import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.ai.relationship.model.Dimension;
import yadi.samuraiai.ai.relationship.model.FriendshipStage;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;
import yadi.samuraiai.ai.relationship.model.RivalryLevel;

/** Rivalry comes from competition, conflict and wounded honor; a close friendship blunts it. */
public final class RivalryEngine extends DimensionEngine {
    public RivalryEngine() { super(Dimension.RIVALRY); }

    @Override protected double adjust(double delta, boolean up, RelationshipRecord r, Modulation m, RelationshipSettings s) {
        if (up && r.stage().ordinal() >= FriendshipStage.CLOSE_FRIEND.ordinal()) return delta * s.rivalryFriendBlock();
        return delta;
    }

    public static RivalryLevel level(RelationshipRecord r, RelationshipSettings s) { return level(r.rivalry(), s); }

    public static RivalryLevel level(double rivalry, RelationshipSettings s) {
        if (rivalry >= s.rivalryNemesis()) return RivalryLevel.NEMESIS;
        if (rivalry >= s.rivalryMajor()) return RivalryLevel.MAJOR;
        if (rivalry >= s.rivalryGrowing()) return RivalryLevel.GROWING;
        if (rivalry >= s.rivalryMinor()) return RivalryLevel.MINOR;
        return RivalryLevel.NONE;
    }
}
