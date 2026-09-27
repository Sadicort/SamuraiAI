package yadi.samuraiai.living.calendar.moon;

import yadi.samuraiai.living.core.MoonPhase;

/**
 * The moon: a fixed cycle of {@code cycleDays} days, shifted by {@code offsetDays}. With the default 30-day cycle and offset 1
 * the full moon falls on the 15th of every 30-day month, which is when the Moon Festival is held.
 */
public final class MoonEngine {
    private final int cycleDays, offsetDays;

    public MoonEngine(int cycleDays, int offsetDays) { this.cycleDays = Math.max(2, cycleDays); this.offsetDays = offsetDays; }

    public int cycleDays() { return cycleDays; }

    /** Position in the cycle for a day, 0 = new moon, 0.5 = full moon. */
    public double position(long dayIndex) { return Math.floorMod(dayIndex + offsetDays, (long) cycleDays) / (double) cycleDays; }

    public MoonPhase phase(long dayIndex) { return MoonPhase.at(position(dayIndex)); }

    /** Days until the next full moon (0 when today is full). */
    public int daysToFull(long dayIndex) {
        for (int d = 0; d < cycleDays; d++) if (phase(dayIndex + d) == MoonPhase.FULL) return d;
        return 0;
    }
}
