package yadi.samuraiai.ai.scheduler.engine;

/**
 * The scheduler's view of what an NPC's senses concluded, in plain numbers (threat 0-3, awareness 0-5), so the scheduler
 * needs no knowledge of the perception engine's types. The world adapter fills it from the perception snapshot.
 */
public record Perceived(int threatLevel, double threatScore, int awareness, double suspicion, boolean suspicious, Investigation investigation,
                        boolean damaged, double threatX, double threatY, double threatZ) {
    public static final Perceived CALM = new Perceived(0, 0, 0, 0, false, null, false, Double.NaN, Double.NaN, Double.NaN);
    public boolean hasThreatPosition() { return !Double.isNaN(threatX) && !Double.isNaN(threatZ); }
}
