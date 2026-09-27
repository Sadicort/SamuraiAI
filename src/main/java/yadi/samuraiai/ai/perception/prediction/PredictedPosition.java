package yadi.samuraiai.ai.perception.prediction;

/** Where a target is expected to be, with how much trust (falls with the prediction horizon). */
public record PredictedPosition(double x, double y, double z, double confidence, double speed, double headingDegrees) { }
