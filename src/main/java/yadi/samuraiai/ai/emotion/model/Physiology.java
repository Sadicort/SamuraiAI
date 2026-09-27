package yadi.samuraiai.ai.emotion.model;

/** How feelings colour the body: movement and turn speed scales, gaze steadiness, reaction delay and posture. Advice for the behavior and animation layers; emotion never moves an entity itself. */
public record Physiology(double speedScale, double turnScale, double gazeScale, int reactionDelayTicks, double posture) {
    public static final Physiology NEUTRAL = new Physiology(1.0D, 1.0D, 1.0D, 0, 0.0D);
}
