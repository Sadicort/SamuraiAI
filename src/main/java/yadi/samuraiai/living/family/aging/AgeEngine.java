package yadi.samuraiai.living.family.aging;

/**
 * Age from the official calendar: {@code (current Deiliora date - birth date)}, never from entity ticks. A year is the
 * calendar's year ({@code minutesPerYear}); stage thresholds (in years) are configuration.
 */
public final class AgeEngine {
    public record AgeProfile(long birth, double years, LifeStage stage, double progress, boolean agingEnabled) { }

    private final int[] thresholds;   // years at which each stage after INFANT begins
    private final long minutesPerYear;

    /** {@code thresholds}: child, adolescent, young adult, adult, mature, elder (years). */
    public AgeEngine(long minutesPerYear, int child, int adolescent, int youngAdult, int adult, int mature, int elder) {
        this.minutesPerYear = Math.max(1, minutesPerYear);
        this.thresholds = new int[]{child, adolescent, youngAdult, adult, mature, elder};
    }

    public long minutesPerYear() { return minutesPerYear; }

    public double years(long birth, long now) { return (now - birth) / (double) minutesPerYear; }

    public LifeStage stage(double years) {
        LifeStage[] stages = LifeStage.values();
        LifeStage s = LifeStage.INFANT_FUTURE;
        for (int i = 0; i < thresholds.length; i++) if (years >= thresholds[i]) s = stages[i + 1];
        return s;
    }

    public AgeProfile profile(long birth, long now, boolean agingEnabled) {
        double y = years(birth, now);
        LifeStage st = stage(y);
        int i = st.ordinal();
        double start = i == 0 ? 0 : thresholds[i - 1], end = i < thresholds.length ? thresholds[i] : start + 30;
        return new AgeProfile(birth, y, st, Math.max(0, Math.min(1, (y - start) / Math.max(1e-9, end - start))), agingEnabled);
    }

    /** The birth minute for someone who is {@code years} old now. */
    public long birthFor(double years, long now) { return now - Math.round(years * minutesPerYear); }
}
