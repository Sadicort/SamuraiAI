package yadi.samuraiai.living.core;

/**
 * The moon, in eight phases. {@link #family()} groups them into the five the rituals talk about (new, waxing, quarter,
 * full, waning); {@link #light()} is how bright the night is, 0..1.
 */
public enum MoonPhase {
    NEW(0.0D), WAXING_CRESCENT(0.25D), FIRST_QUARTER(0.5D), WAXING_GIBBOUS(0.75D), FULL(1.0D), WANING_GIBBOUS(0.75D), LAST_QUARTER(0.5D), WANING_CRESCENT(0.25D);

    public enum Family { NEW, WAXING, QUARTER, FULL, WANING }

    private final double light;
    MoonPhase(double light) { this.light = light; }

    public double light() { return light; }

    /** The Spanish name players and prompts read. */
    public String label() {
        return switch (this) {
            case NEW -> "luna nueva"; case WAXING_CRESCENT -> "luna creciente"; case FIRST_QUARTER -> "cuarto creciente"; case WAXING_GIBBOUS -> "luna gibosa creciente";
            case FULL -> "luna llena"; case WANING_GIBBOUS -> "luna gibosa menguante"; case LAST_QUARTER -> "cuarto menguante"; case WANING_CRESCENT -> "luna menguante";
        };
    }

    public Family family() {
        return switch (this) {
            case NEW -> Family.NEW;
            case WAXING_CRESCENT, WAXING_GIBBOUS -> Family.WAXING;
            case FIRST_QUARTER, LAST_QUARTER -> Family.QUARTER;
            case FULL -> Family.FULL;
            case WANING_GIBBOUS, WANING_CRESCENT -> Family.WANING;
        };
    }

    /** The phase at a point of a cycle, {@code position} in [0,1). */
    public static MoonPhase at(double position) {
        double p = position - Math.floor(position);
        return values()[(int) Math.floor(p * values().length + 0.5D) % values().length];
    }
}
