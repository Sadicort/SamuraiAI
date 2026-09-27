package yadi.samuraiai.living.calendar.clock;

/**
 * The official clock: an absolute count of Deiliora minutes that only moves forward.
 *
 * <p>It is fed a source of ticks every server tick. When the sun is moving and {@link #alignToSun} is on, the source is the
 * level's day time, so a night slept through or a {@code /time add} advances the calendar exactly as the sky shows; when the
 * daylight cycle is frozen the source is the game time, so the calendar still runs. A source that jumps backwards (a
 * {@code /time set} to an earlier hour, a restored backup) never rewinds the clock: with the sun aligned the clock moves
 * forward to the next time the sky shows that hour, otherwise it holds until the source catches up. Either case is counted.
 */
public final class DeiliClock {
    public enum Source { NONE, DAY_TIME, GAME_TIME }

    /** How far ahead of the sky the calendar may be at start-up before it is treated as a different day. */
    public static final int SUN_TOLERANCE_MINUTES = 30;

    private long minute;
    private double fraction;
    private long lastSource = Long.MIN_VALUE;
    private Source sourceKind = Source.NONE;
    private long rewindAttempts, forwardJumps;

    private int ticksPerDay = 24000;
    private int sunOffsetMinutes = 360;
    private boolean alignToSun = true;

    public DeiliClock(long startMinute) { this.minute = startMinute; }

    public void configure(int ticksPerDay, int sunOffsetMinutes, boolean alignToSun) {
        this.ticksPerDay = Math.max(20, ticksPerDay);
        this.sunOffsetMinutes = Math.floorMod(sunOffsetMinutes, CalendarSpec.MINUTES_PER_DAY);
        this.alignToSun = alignToSun;
    }

    public long now() { return minute; }
    public long rewindAttempts() { return rewindAttempts; }
    public long forwardJumps() { return forwardJumps; }
    public Source sourceKind() { return sourceKind; }
    public long lastSource() { return lastSource; }
    public double minutesPerTick() { return CalendarSpec.MINUTES_PER_DAY / (double) ticksPerDay; }

    /** The minute of the day the sky shows for a level day time. */
    public int sunMinuteOfDay(long dayTime) {
        return (int) Math.floorMod((long) Math.floor(Math.floorMod(dayTime, (long) ticksPerDay) * minutesPerTick()) + sunOffsetMinutes, (long) CalendarSpec.MINUTES_PER_DAY);
    }

    /** Moves forward to the next moment the day shows {@code minuteOfDay} (possibly now). Never backwards. Returns minutes moved. */
    public long alignTo(int minuteOfDay) {
        long current = Math.floorMod(minute, (long) CalendarSpec.MINUTES_PER_DAY);
        long forward = Math.floorMod(minuteOfDay - current, (long) CalendarSpec.MINUTES_PER_DAY);
        minute += forward;
        fraction = 0;
        return forward;
    }

    /**
     * Advances from the world's clocks. Returns the minutes gained this call (zero or positive).
     *
     * @param gameTime  the level's game time (monotonic in a healthy world)
     * @param dayTime   the level's day time (moves with the sun, can be set by commands)
     * @param sunMoves  whether the daylight cycle is running
     */
    public long advance(long gameTime, long dayTime, boolean sunMoves) {
        Source kind = alignToSun && sunMoves ? Source.DAY_TIME : Source.GAME_TIME;
        long source = kind == Source.DAY_TIME ? dayTime : gameTime;
        if (kind != sourceKind || lastSource == Long.MIN_VALUE) {
            // First tick, or the source changed (daylight cycle toggled): start counting from here without a jump.
            boolean first = sourceKind == Source.NONE;
            sourceKind = kind;
            lastSource = source;
            if (first && kind == Source.DAY_TIME) {
                int sun = sunMinuteOfDay(dayTime);
                long ahead = Math.floorMod(Math.floorMod(minute, (long) CalendarSpec.MINUTES_PER_DAY) - sun, (long) CalendarSpec.MINUTES_PER_DAY);
                // A calendar a few minutes ahead of the sky (rounding across a restart) keeps its time rather than skipping a whole day.
                if (ahead > 0 && ahead <= SUN_TOLERANCE_MINUTES) return 0;
                long moved = alignTo(sun);
                if (moved > 0) forwardJumps++;
                return moved;
            }
            return 0;
        }
        long delta = source - lastSource;
        lastSource = source;
        if (delta < 0) {
            rewindAttempts++;
            if (kind == Source.DAY_TIME) { forwardJumps++; return alignTo(sunMinuteOfDay(dayTime)); }
            return 0;
        }
        return addTicks(delta);
    }

    /** Adds ticks worth of time (fractions of a minute are kept). */
    public long addTicks(long ticks) {
        if (ticks <= 0) return 0;
        fraction += ticks * minutesPerTick();
        long whole = (long) Math.floor(fraction);
        fraction -= whole;
        minute += whole;
        return whole;
    }

    /** Adds whole minutes (commands, catch-up, tests). Negative values are refused. */
    public long addMinutes(long minutes) {
        if (minutes <= 0) { if (minutes < 0) rewindAttempts++; return 0; }
        minute += minutes;
        return minutes;
    }

    /** Restores persisted state. The source is re-read from the world on the next tick. */
    public void restore(long savedMinute, double savedFraction, long rewinds, long jumps) {
        minute = savedMinute;
        fraction = Math.max(0.0D, Math.min(0.999999D, savedFraction));
        rewindAttempts = rewinds;
        forwardJumps = jumps;
        lastSource = Long.MIN_VALUE;
        sourceKind = Source.NONE;
    }

    /** After a restore the first tick must not re-align a fresh world: it only resumes counting. */
    public void resumeFrom(long gameTime, long dayTime, boolean sunMoves) {
        sourceKind = alignToSun && sunMoves ? Source.DAY_TIME : Source.GAME_TIME;
        lastSource = sourceKind == Source.DAY_TIME ? dayTime : gameTime;
    }

    public double fraction() { return fraction; }
}
