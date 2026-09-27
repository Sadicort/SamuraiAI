package yadi.samuraiai.living.calendar.metrics;

import java.util.concurrent.atomic.AtomicLong;

/** Volume and cost of the Living Calendar: days, seasons, weather changes, festivals, anniversaries, rewinds refused, time per tick and per day. */
public final class CalendarMetrics {
    public final AtomicLong ticks = new AtomicLong(), days = new AtomicLong(), skippedDays = new AtomicLong(), months = new AtomicLong(), seasons = new AtomicLong(), years = new AtomicLong(),
            weatherChanges = new AtomicLong(), festivals = new AtomicLong(), holidays = new AtomicLong(), anniversaries = new AtomicLong(), harvests = new AtomicLong(),
            timelineEntries = new AtomicLong(), rewindsRefused = new AtomicLong(), offlineMinutes = new AtomicLong(), manualMinutes = new AtomicLong();
    public final AtomicLong tickNanos = new AtomicLong(), dayNanos = new AtomicLong(), maxDayNanos = new AtomicLong();

    public record Snapshot(long ticks, long days, long skippedDays, long months, long seasons, long years, long weatherChanges, long festivals, long holidays, long anniversaries,
                           long harvests, long timelineEntries, long rewindsRefused, long offlineMinutes, long manualMinutes, double tickMicros, double dayMillis, double maxDayMillis) { }

    public void day(long nanos) { days.incrementAndGet(); dayNanos.addAndGet(nanos); maxDayNanos.accumulateAndGet(nanos, Math::max); }

    public Snapshot snapshot() {
        return new Snapshot(ticks.get(), days.get(), skippedDays.get(), months.get(), seasons.get(), years.get(), weatherChanges.get(), festivals.get(), holidays.get(), anniversaries.get(),
                harvests.get(), timelineEntries.get(), rewindsRefused.get(), offlineMinutes.get(), manualMinutes.get(),
                tickNanos.get() / 1000.0 / Math.max(1, ticks.get()), dayNanos.get() / 1e6 / Math.max(1, days.get()), maxDayNanos.get() / 1e6);
    }

    public void reset() {
        for (AtomicLong l : new AtomicLong[] {ticks, days, skippedDays, months, seasons, years, weatherChanges, festivals, holidays, anniversaries, harvests, timelineEntries, rewindsRefused,
                offlineMinutes, manualMinutes, tickNanos, dayNanos, maxDayNanos}) l.set(0);
    }
}
