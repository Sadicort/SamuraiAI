package yadi.samuraiai.living.core;

/**
 * The World Clock API: the one question every living-world engine asks about time. The Calendar Engine implements it; no
 * engine reads the vanilla clock for anything historical. Time is an absolute count of Deiliora minutes that never goes
 * backwards.
 */
public interface WorldClock {
    /** The current absolute minute. Monotonic. */
    long now();

    /** The calendar breakdown of any absolute minute. */
    CalendarDate date(long minute);

    int minutesPerDay();

    default CalendarDate today() { return date(now()); }

    default long dayIndex(long minute) { return Math.floorDiv(minute, (long) minutesPerDay()); }

    default long currentDay() { return dayIndex(now()); }

    /** Whole days between two instants (negative when {@code to} is earlier). */
    default double daysBetween(long from, long to) { return (to - from) / (double) minutesPerDay(); }

    /** A clock frozen at one minute, for tests and for replaying history. */
    static WorldClock fixed(long minute, int minutesPerDay, java.util.function.LongFunction<CalendarDate> dates) {
        return new WorldClock() {
            @Override public long now() { return minute; }
            @Override public CalendarDate date(long m) { return dates.apply(m); }
            @Override public int minutesPerDay() { return minutesPerDay; }
        };
    }
}
