package yadi.samuraiai.ai.perception.prediction;

import yadi.samuraiai.ai.perception.engine.PerceptionSettings;
import yadi.samuraiai.ai.perception.engine.Perceiver;
import yadi.samuraiai.ai.perception.vision.VisionCone;
import yadi.samuraiai.ai.perception.vision.VisualTrack;

/**
 * Forecasts where a tracked target will be and when it will leave sight, from its last observed position and velocity.
 * Damped constant-velocity: a target that was moving keeps moving, but the estimate is trusted less the further ahead it
 * looks. Prepares following, searching and (later) combat without any of them knowing how it is computed.
 */
public final class TrajectoryPredictor {
    private static final double DAMPING = 0.985D, CONFIDENCE_HALF_LIFE_TICKS = 60.0D;

    private TrajectoryPredictor() { }

    public static PredictedPosition predict(double x, double y, double z, double vx, double vz, long ticksAhead, double baseConfidence) {
        double travelled = 0.0D, factor = 1.0D;
        for (long t = 0; t < ticksAhead; t++) { travelled += factor; factor *= DAMPING; }
        double confidence = Math.max(0.0D, Math.min(1.0D, baseConfidence)) * Math.pow(0.5D, ticksAhead / CONFIDENCE_HALF_LIFE_TICKS);
        double speed = Math.hypot(vx, vz);
        double heading = speed < 0.01D ? Double.NaN : Math.toDegrees(Math.atan2(-vx, vz));
        return new PredictedPosition(x + vx * travelled, y, z + vz * travelled, confidence, speed, heading);
    }

    public static PredictedPosition predict(VisualTrack track, long now, long ticksAhead) {
        long elapsed = Math.max(0L, now - track.lastSeenTick);
        return predict(track.x, track.y, track.z, track.vx, track.vz, elapsed + ticksAhead, track.confidence);
    }

    /**
     * Ticks until the predicted target leaves the eye's range or field of view, assuming the observer stays put; -1 if it
     * stays in sight for the whole horizon. This is the estimate of "it is about to walk out of view".
     */
    public static int ticksUntilLostFromView(Perceiver eye, PerceptionSettings s, VisualTrack track, int horizonTicks) {
        for (int t = 1; t <= horizonTicks; t += 2) {
            PredictedPosition at = predict(track.x, track.y, track.z, track.vx, track.vz, t, track.confidence);
            var sample = VisionCone.evaluate(eye, at.x(), at.y() + 0.9D, at.z(), s);
            if (!sample.inRange()) return t;
        }
        return -1;
    }
}
