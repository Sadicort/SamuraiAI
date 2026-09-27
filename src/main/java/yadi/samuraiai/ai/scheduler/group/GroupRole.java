package yadi.samuraiai.ai.scheduler.group;

/** A member's part in its group. The multipliers tilt how readily it answers a surprise: scouts investigate, guards fight, reserves flee. */
public enum GroupRole {
    LEADER(1.0D, 1.0D, 0.9D), GUARD(0.9D, 1.3D, 0.6D), SCOUT(1.3D, 0.9D, 1.0D), RESERVE(0.7D, 0.9D, 1.2D);

    private final double investigate, assist, flee;
    GroupRole(double investigate, double assist, double flee) { this.investigate = investigate; this.assist = assist; this.flee = flee; }
    public double investigate() { return investigate; }
    public double assist() { return assist; }
    public double flee() { return flee; }
}
