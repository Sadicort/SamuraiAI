package yadi.samuraiai.ai.perception.vision;

/**
 * Shared per-tick allowance of raycasts. Rays are the costly part of perception, so every consumer asks first; when the
 * budget is spent the caller degrades (keeps its last result) instead of spiking the tick.
 */
public final class RayBudget {
    private int remaining;
    private int consumed;
    private int refused;

    public RayBudget(int rays) { this.remaining = Math.max(0, rays); }

    public boolean tryConsume(int rays) {
        if (rays > remaining) { refused++; return false; }
        remaining -= rays; consumed += rays;
        return true;
    }
    public int remaining() { return remaining; }
    public int consumed() { return consumed; }
    public int refused() { return refused; }
}
