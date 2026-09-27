package yadi.samuraiai.ai.scheduler.time;

import java.util.Arrays;
import java.util.Comparator;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;

/**
 * Maps world time onto the six day periods. The boundaries come from the settings and are sorted, so a configuration that
 * lists them in any order (or lets a period wrap past midnight) still yields a consistent day.
 */
public final class Timeline {
    private record Boundary(int start, DayPeriod period) { }

    private final int dayLength;
    private final Boundary[] boundaries;

    public Timeline(SchedulerSettings s) {
        this.dayLength = s.dayLength();
        Boundary[] all = {
                new Boundary(s.morningStart(), DayPeriod.MORNING), new Boundary(s.afternoonStart(), DayPeriod.AFTERNOON),
                new Boundary(s.eveningStart(), DayPeriod.EVENING), new Boundary(s.nightStart(), DayPeriod.NIGHT),
                new Boundary(s.lateNightStart(), DayPeriod.LATE_NIGHT), new Boundary(s.dawnStart(), DayPeriod.DAWN)};
        Arrays.sort(all, Comparator.comparingInt(Boundary::start).thenComparing(b -> b.period().ordinal()));
        this.boundaries = all;
    }

    public int dayLength() { return dayLength; }
    public int dayTime(long worldTime) { return (int) Math.floorMod(worldTime, (long) dayLength); }
    public long dayNumber(long worldTime) { return Math.floorDiv(worldTime, (long) dayLength); }

    public DayPeriod periodAt(long worldTime) { return boundaries[indexAt(dayTime(worldTime))].period(); }

    private int indexAt(int dayTime) {
        int found = boundaries.length - 1; // before the first boundary the previous day's last period is still running
        for (int i = 0; i < boundaries.length; i++) if (boundaries[i].start() <= dayTime) found = i;
        return found;
    }

    /** Ticks until the next period begins. */
    public int ticksUntilChange(long worldTime) {
        int t = dayTime(worldTime);
        int next = (indexAt(t) + 1) % boundaries.length;
        int start = boundaries[next].start();
        int wait = start - t;
        return wait > 0 ? wait : wait + dayLength;
    }

    /** How far through the current period the day is, 0..1. */
    public double progress(long worldTime) {
        int t = dayTime(worldTime);
        int index = indexAt(t);
        int start = boundaries[index].start();
        int length = boundaries[(index + 1) % boundaries.length].start() - start;
        if (length <= 0) length += dayLength;
        int elapsed = t - start;
        if (elapsed < 0) elapsed += dayLength;
        return Math.min(1.0D, elapsed / (double) length);
    }

    public DayPeriod next(DayPeriod period) { return DayPeriod.values()[(period.ordinal() + 1) % DayPeriod.values().length]; }
}
