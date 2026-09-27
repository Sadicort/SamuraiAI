package yadi.samuraiai.ai.navigation.recovery;

/**
 * Notices a walker that is asked to move but is not getting anywhere. Progress is measured against an anchor
 * position: if the walker has not left a small radius around it for the configured window, it is stuck.
 */
public final class StuckDetector {
    private final int stuckTicks;
    private final double minProgress;
    private double anchorX, anchorY, anchorZ;
    private long anchorTick = -1;

    public StuckDetector(int stuckTicks, double minProgress) { this.stuckTicks = stuckTicks; this.minProgress = minProgress; }

    /** @return ticks stuck when the window expired without progress, else 0 */
    public int update(double x, double y, double z, long tick, boolean tryingToMove) {
        if (!tryingToMove || anchorTick < 0) { anchor(x, y, z, tick); return 0; }
        double moved = Math.sqrt((x - anchorX) * (x - anchorX) + (z - anchorZ) * (z - anchorZ)) + Math.abs(y - anchorY) * 0.5D;
        if (moved >= minProgress) { anchor(x, y, z, tick); return 0; }
        long waited = tick - anchorTick;
        if (waited >= stuckTicks) { anchor(x, y, z, tick); return (int) waited; }
        return 0;
    }

    public void reset() { anchorTick = -1; }
    private void anchor(double x, double y, double z, long tick) { anchorX = x; anchorY = y; anchorZ = z; anchorTick = tick; }
}
