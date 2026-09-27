package yadi.samuraiai.goal;

import java.util.Objects;

/**
 * What the NPC wants to achieve right now (protect the village, rest, ...).
 * The DecisionEngine picks one Goal out of several candidates; a Behavior
 * later turns the chosen Goal into concrete Tasks.
 */
public class Goal {

    private final GoalType type;
    private final int basePriority;
    private final String description;

    /** Uses the type's own base priority, which is the normal case. */
    public Goal(GoalType type) {
        this(type, type.basePriority(), type.name());
    }

    public Goal(GoalType type, int basePriority, String description) {
        this.type = Objects.requireNonNull(type, "type");
        this.basePriority = basePriority;
        this.description = description == null ? type.name() : description;
    }

    public GoalType getType() {
        return type;
    }

    public int getBasePriority() {
        return basePriority;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Equality is by type alone. The brain compares "is this the same goal I
     * was already pursuing?" to decide whether to replan, and a goal rebuilt
     * with a different description is still the same intent.
     */
    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        return other instanceof Goal goal && type == goal.type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return type.name();
    }
}
