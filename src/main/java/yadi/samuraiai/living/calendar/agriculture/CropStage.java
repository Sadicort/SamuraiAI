package yadi.samuraiai.living.calendar.agriculture;

/** Where a crop is in its year. NONE means the resource is not a seasonal crop (it can be produced any time). */
public enum CropStage {
    PLANTING(0.15D), GROWTH(0.05D), HARVEST(1.0D), REST(0.0D), NONE(1.0D);

    private final double output;
    CropStage(double output) { this.output = output; }

    /** Share of a full harvest a farmer gets from a day's work in this stage. */
    public double output() { return output; }
}
