package yadi.samuraiai.living.calendar.astronomy;

/**
 * Day length through the year: {@code 12 ± amplitude} hours of daylight, longest on {@code longestDay} (day of year) and
 * shortest half a year later, with solar noon at 12:00. Village schedules use sunrise to move the start of the day (people
 * rise later in winter).
 */
public final class SunModel {
    private final int daysPerYear, longestDay;
    private final double amplitudeHours;

    public SunModel(int daysPerYear, int longestDay, double amplitudeHours) {
        this.daysPerYear = Math.max(1, daysPerYear);
        this.longestDay = longestDay;
        this.amplitudeHours = Math.max(0.0D, Math.min(10.0D, amplitudeHours));
    }

    public double daylightHours(int dayOfYear) { return 12.0D + amplitudeHours * Math.cos(2.0D * Math.PI * (dayOfYear - longestDay) / daysPerYear); }

    /** Minute of the day the sun rises. */
    public int sunrise(int dayOfYear) { return (int) Math.round((12.0D - daylightHours(dayOfYear) / 2.0D) * 60.0D); }

    public int sunset(int dayOfYear) { return (int) Math.round((12.0D + daylightHours(dayOfYear) / 2.0D) * 60.0D); }

    /** How many minutes later than the year's average the sun rises today (negative in summer). */
    public int sunriseShift(int dayOfYear) { return sunrise(dayOfYear) - 360; }
}
