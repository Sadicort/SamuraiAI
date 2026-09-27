package yadi.samuraiai.living.calendar.holidays;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import yadi.samuraiai.ai.scheduler.util.Segments;
import yadi.samuraiai.living.core.CalendarDate;

/**
 * Fixed holidays, from lines such as {@code obon;name=Obon;month=7;day=13;days=3;kind=MEMORIAL;tags=ancestors,family}. New Year
 * and Obon (the days of the ancestors, a family hook) are built in. On each day it reports the holidays that begin.
 */
public final class HolidayEngine {
    public static final List<String> DEFAULT_LINES = List.of(
            "shogatsu;name=Año Nuevo (Shōgatsu);month=1;day=1;days=3;kind=NEW_YEAR;tags=new_year,family,temple",
            "obon;name=Obon (días de los ancestros);month=7;day=13;days=3;kind=MEMORIAL;tags=ancestors,family,temple");

    private final Map<String, HolidayDef> holidays = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();

    public HolidayEngine(List<String> lines) {
        for (String line : DEFAULT_LINES) parse(line, null);
        if (lines != null) for (String line : lines) parse(line, problems);
    }

    private void parse(String line, List<String> report) {
        var parsed = Segments.parse(line);
        if (parsed.isEmpty()) { if (report != null) report.add("holiday line '" + line + "' has no id"); return; }
        Segments s = parsed.get();
        String id = s.id().toLowerCase(Locale.ROOT);
        holidays.put(id, new HolidayDef(id, s.text("name", id), s.integer("month", 1), s.integer("day", 1), s.integer("days", 1), s.text("kind", "CELEBRATION").toUpperCase(Locale.ROOT), s.list("tags")));
        if (report != null) s.problems().forEach(p -> report.add(id + ": " + p));
    }

    public List<HolidayDef> all() { return List.copyOf(holidays.values()); }
    public List<String> problems() { return List.copyOf(problems); }

    /** Holidays running on an absolute day (a holiday that began at the end of the previous year still counts). */
    public List<HolidayDef> activeOn(long dayIndex, yadi.samuraiai.living.calendar.clock.CalendarSpec spec) {
        int year = spec.date(dayIndex * yadi.samuraiai.living.calendar.clock.CalendarSpec.MINUTES_PER_DAY).year();
        List<HolidayDef> out = new ArrayList<>();
        for (HolidayDef h : holidays.values()) {
            for (int y = year - 1; y <= year; y++) {
                long start = Math.floorDiv(spec.minuteOf(y, h.month(), h.day(), 0, 0), (long) yadi.samuraiai.living.calendar.clock.CalendarSpec.MINUTES_PER_DAY);
                if (dayIndex >= start && dayIndex < start + h.days()) { out.add(h); break; }
            }
        }
        return out;
    }

    /** Holidays whose first day is this date. */
    public List<HolidayDef> beginningOn(CalendarDate date) {
        List<HolidayDef> out = new ArrayList<>();
        for (HolidayDef h : holidays.values()) if (h.month() == date.month() && h.day() == date.day()) out.add(h);
        return out;
    }
}
