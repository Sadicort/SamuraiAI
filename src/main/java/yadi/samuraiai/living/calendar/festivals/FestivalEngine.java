package yadi.samuraiai.living.calendar.festivals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;
import yadi.samuraiai.living.calendar.clock.CalendarSpec;
import yadi.samuraiai.living.core.MoonPhase;

/**
 * Which festivals are running on a given day, and which started or ended when the day changed. A festival's start day for a
 * year is computed once per (festival, year) and cached; a moon-bound festival starts on the first day of its window with the
 * right moon (or at the end of the window if the moon never comes).
 */
public final class FestivalEngine {
    public record Change(FestivalDef festival, boolean started, int year) { }

    private final FestivalCatalog catalog;
    private final CalendarSpec spec;
    private final LongFunction<MoonPhase> moonOfDay;
    private final Map<String, Long> startCache = new LinkedHashMap<>();
    private List<FestivalDef> activeToday = List.of();
    private long evaluatedDay = Long.MIN_VALUE;

    public FestivalEngine(FestivalCatalog catalog, CalendarSpec spec, LongFunction<MoonPhase> moonOfDay) {
        this.catalog = catalog; this.spec = spec; this.moonOfDay = moonOfDay;
    }

    public FestivalCatalog catalog() { return catalog; }

    /** The absolute day a festival begins in a calendar year. */
    public long startDay(FestivalDef f, int year) {
        String key = f.id() + "@" + year;
        Long cached = startCache.get(key);
        if (cached != null) return cached;
        long first = Math.floorDiv(spec.minuteOf(year, f.month(), f.day(), 0, 0), (long) CalendarSpec.MINUTES_PER_DAY);
        long start = first;
        if (f.moon() != null) {
            start = first + f.moonWindow() - 1;
            for (int d = 0; d < f.moonWindow(); d++) if (moonOfDay.apply(first + d).family() == f.moon()) { start = first + d; break; }
        }
        if (startCache.size() > 512) startCache.clear();
        startCache.put(key, start);
        return start;
    }

    /** Festivals running on an absolute day (a festival that began late in the previous year is still found). */
    public List<FestivalDef> activeOn(long dayIndex) {
        int year = spec.date(dayIndex * CalendarSpec.MINUTES_PER_DAY).year();
        List<FestivalDef> out = new ArrayList<>();
        for (FestivalDef f : catalog.all()) {
            for (int y = year - 1; y <= year; y++) {
                long start = startDay(f, y);
                if (dayIndex >= start && dayIndex < start + f.days()) { out.add(f); break; }
            }
        }
        return out;
    }

    /** Day changed: returns the festivals that ended and started, and remembers today's set. */
    public List<Change> onDay(long dayIndex) {
        List<FestivalDef> now = activeOn(dayIndex);
        List<Change> changes = new ArrayList<>();
        int year = spec.date(dayIndex * CalendarSpec.MINUTES_PER_DAY).year();
        if (evaluatedDay != Long.MIN_VALUE) {
            for (FestivalDef f : activeToday) if (!now.contains(f)) changes.add(new Change(f, false, year));
            for (FestivalDef f : now) if (!activeToday.contains(f)) changes.add(new Change(f, true, year));
        } else {
            for (FestivalDef f : now) changes.add(new Change(f, true, year));
        }
        activeToday = List.copyOf(now);
        evaluatedDay = dayIndex;
        return changes;
    }

    public List<FestivalDef> activeToday() { return activeToday; }

    /** Restores the set of festivals considered running (so a restart does not re-announce them). */
    public void restoreActive(long dayIndex) { activeToday = activeOn(dayIndex); evaluatedDay = dayIndex; }

    /** The next festival to begin after a day, with how many days away it is. */
    public java.util.Optional<Map.Entry<FestivalDef, Long>> next(long dayIndex) {
        int year = spec.date(dayIndex * CalendarSpec.MINUTES_PER_DAY).year();
        FestivalDef best = null;
        long bestStart = Long.MAX_VALUE;
        for (FestivalDef f : catalog.all()) for (int y = year; y <= year + 1; y++) {
            long s = startDay(f, y);
            if (s > dayIndex && s < bestStart) { bestStart = s; best = f; }
        }
        return best == null ? java.util.Optional.empty() : java.util.Optional.of(Map.entry(best, bestStart - dayIndex));
    }
}
