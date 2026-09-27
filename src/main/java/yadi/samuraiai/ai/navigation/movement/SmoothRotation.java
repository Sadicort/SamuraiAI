package yadi.samuraiai.ai.navigation.movement;

/** Yaw helpers that make turning progressive instead of snapping. Angles are Minecraft degrees (0 = +Z, 90 = -X). */
public final class SmoothRotation {
    private SmoothRotation() { }

    public static float wrap(float degrees) {
        float d = degrees % 360.0F;
        if (d >= 180.0F) d -= 360.0F;
        if (d < -180.0F) d += 360.0F;
        return d;
    }

    /** Signed shortest difference from {@code current} to {@code target}. */
    public static float difference(float current, float target) { return wrap(target - current); }

    /** Moves {@code current} toward {@code target} by at most {@code maxStep} degrees. */
    public static float approach(float current, float target, float maxStep) {
        float diff = difference(current, target);
        if (Math.abs(diff) <= maxStep) return wrap(target);
        return wrap(current + Math.signum(diff) * maxStep);
    }

    public static float yawTo(double fromX, double fromZ, double toX, double toZ) {
        return (float) Math.toDegrees(Math.atan2(-(toX - fromX), toZ - fromZ));
    }

    /**
     * Speed multiplier for a given heading error: straight ahead = 1, facing away = {@code 1 - slowdown}.
     * Never below 0.15 so a walker that must turn around still turns while moving instead of freezing.
     */
    public static double turnSpeedFactor(float headingErrorDegrees, double slowdown) {
        double error = Math.min(1.0D, Math.abs(headingErrorDegrees) / 90.0D);
        return Math.max(0.15D, 1.0D - slowdown * error);
    }
}
