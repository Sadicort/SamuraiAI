package yadi.samuraiai.ai.perception.engine;

/**
 * Per-NPC scaling of the senses and of how strongly stimuli register. This is the hook through which personality
 * reaches perception (the scheduler's personality engine derives one per NPC): a curious NPC notices more, a fearful
 * one is easier to alarm. 1.0 everywhere is the neutral NPC.
 */
public record SenseProfile(double visionRange, double hearing, double curiosity, double suspicionGain, double fearfulness, double attentionSpan) {
    public static final SenseProfile NEUTRAL = new SenseProfile(1, 1, 1, 1, 1, 1);

    public SenseProfile {
        visionRange = clamp(visionRange); hearing = clamp(hearing); curiosity = clamp(curiosity);
        suspicionGain = clamp(suspicionGain); fearfulness = clamp(fearfulness); attentionSpan = clamp(attentionSpan);
    }
    private static double clamp(double v) { return Double.isFinite(v) ? Math.max(0.1D, Math.min(4.0D, v)) : 1.0D; }
}
