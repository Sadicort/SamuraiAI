package yadi.samuraiai.ai.relationship.model;

/** The axes of a relationship. Each runs 0-100. HONOR is the honor this NPC has observed in the other (50 = neutral). */
public enum Dimension {
    TRUST(true), RESPECT(true), AFFINITY(true), FEAR(false), LOYALTY(true), RIVALRY(false), HONOR(true);

    private final boolean good;
    Dimension(boolean good) { this.good = good; }
    /** Whether a higher value is a better relationship (false for fear and rivalry). */
    public boolean higherIsBetter() { return good; }
}
