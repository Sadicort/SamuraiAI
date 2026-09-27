package yadi.samuraiai.ai.perception.vision;

/** Region of the visual field. Each has its own sensitivity and a fraction of the full sight range. */
public enum VisionZone {
    CENTER(1.00D, 1.00D), MAIN(0.85D, 0.90D), PERIPHERAL(0.45D, 0.50D), REAR(0.10D, 0.15D), OUT_OF_VIEW(0.0D, 0.0D);

    private final double sensitivity;
    private final double rangeFactor;
    VisionZone(double sensitivity, double rangeFactor) { this.sensitivity = sensitivity; this.rangeFactor = rangeFactor; }
    public double sensitivity() { return sensitivity; }
    public double rangeFactor() { return rangeFactor; }
}
