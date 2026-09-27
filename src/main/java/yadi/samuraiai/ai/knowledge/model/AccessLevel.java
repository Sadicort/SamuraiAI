package yadi.samuraiai.ai.knowledge.model;

/** Who may know something: everyone, community members, an inner circle, or only leaders. A member's rank is the highest level they may read. */
public enum AccessLevel { PUBLIC, MEMBERS, INNER_CIRCLE, LEADERS;
    public boolean visibleTo(AccessLevel rank) { return rank.ordinal() >= ordinal(); }
}
