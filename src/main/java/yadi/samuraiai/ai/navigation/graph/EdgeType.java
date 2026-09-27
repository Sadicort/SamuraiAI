package yadi.samuraiai.ai.navigation.graph;

/** How an NPC gets from one node to the next. Base cost is per edge, before terrain and danger multipliers. */
public enum EdgeType {
    WALK(1.0D), DIAGONAL(1.4142D), STEP(1.5D), JUMP(2.0D), DESCEND(1.5D), CLIMB(1.6D), DOOR(2.5D), BRIDGE(1.1D), WADE(1.3D), SWIM(1.8D);

    private final double baseCost;
    EdgeType(double baseCost) { this.baseCost = baseCost; }
    public double baseCost() { return baseCost; }
    /** Edges that need the body to leave the ground or use a special control. */
    public boolean needsJump() { return this == JUMP; }
    public boolean isVertical() { return this == STEP || this == JUMP || this == DESCEND || this == CLIMB; }
}
