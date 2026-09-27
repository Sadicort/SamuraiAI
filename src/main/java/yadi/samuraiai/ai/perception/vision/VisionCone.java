package yadi.samuraiai.ai.perception.vision;

import yadi.samuraiai.ai.perception.engine.PerceptionSettings;
import yadi.samuraiai.ai.perception.engine.Perceiver;

/**
 * The visual field: horizontal and vertical angles, near/far distance and peripheral vision, split into zones with
 * their own sensitivity. Pure geometry, no world access.
 */
public final class VisionCone {
    /** Result of testing one point against the field. {@code sensitivity} already includes the near-range floor. */
    public record Sample(VisionZone zone, double sensitivity, double effectiveRange, double distance, double horizontalDegrees,
                         double verticalDegrees, boolean inRange) { }

    private VisionCone() { }

    /** Minecraft yaw of the direction from (fx, fz) to (tx, tz): 0 = +Z, 90 = -X. */
    public static float yawTo(double fx, double fz, double tx, double tz) {
        return (float) Math.toDegrees(Math.atan2(-(tx - fx), tz - fz));
    }

    public static double wrapDegrees(double d) {
        double v = d % 360.0D;
        if (v >= 180.0D) v -= 360.0D;
        if (v < -180.0D) v += 360.0D;
        return v;
    }

    public static Sample evaluate(Perceiver eye, double tx, double ty, double tz, PerceptionSettings s) {
        double dx = tx - eye.x(), dy = ty - eye.eyeY(), dz = tz - eye.z();
        double horizontal = Math.hypot(dx, dz), distance = Math.sqrt(horizontal * horizontal + dy * dy);
        double far = s.visionFar() * eye.senses().visionRange();
        double yawDiff = horizontal < 1.0E-6D ? 0.0D : Math.abs(wrapDegrees(yawTo(eye.x(), eye.z(), tx, tz) - eye.yaw()));
        // MC pitch is positive when looking down; elevation of the target is positive when above the eyes.
        double elevation = Math.toDegrees(Math.atan2(dy, Math.max(horizontal, 1.0E-6D)));
        double pitchDiff = Math.abs(elevation + eye.pitch());
        double halfH = s.visionHorizontalFov() / 2.0D, halfV = s.visionVerticalFov() / 2.0D;
        VisionZone zone;
        if (pitchDiff > halfV + 20.0D) zone = VisionZone.OUT_OF_VIEW;
        else if (yawDiff <= halfH * 0.5D) zone = VisionZone.CENTER;
        else if (yawDiff <= halfH) zone = VisionZone.MAIN;
        else if (yawDiff <= halfH + s.visionPeripheralDegrees()) zone = VisionZone.PERIPHERAL;
        else zone = VisionZone.REAR;
        double sensitivity = zone == VisionZone.REAR ? s.visionRearSensitivity() : zone.sensitivity();
        double range = far * (zone == VisionZone.REAR ? 0.15D : zone.rangeFactor());
        if (pitchDiff > halfV && zone != VisionZone.OUT_OF_VIEW) sensitivity *= 0.5D;
        // Things right next to the NPC are noticed even outside the cone: presence, not sight.
        boolean near = distance <= s.visionNear();
        if (near) { sensitivity = Math.max(sensitivity, 0.5D); range = Math.max(range, s.visionNear()); }
        if (zone == VisionZone.OUT_OF_VIEW && !near) return new Sample(zone, 0.0D, 0.0D, distance, yawDiff, pitchDiff, false);
        return new Sample(zone, sensitivity, range, distance, yawDiff, pitchDiff, distance <= range);
    }
}
