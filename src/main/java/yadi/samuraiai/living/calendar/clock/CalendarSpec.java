package yadi.samuraiai.living.calendar.clock;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import yadi.samuraiai.living.core.CalendarDate;
import yadi.samuraiai.living.core.DayPhase;
import yadi.samuraiai.living.core.Season;

/**
 * The shape of the Deiliora calendar: months (name, days, season), weekday names, the epoch year and where each phase of the
 * day begins. It converts absolute minutes to dates and back; it holds no state. A day always has 24 hours of 60 minutes.
 *
 * <p>Month lines look like {@code Uzuki:30:SPRING}; phase lines like {@code DAWN:05:00}. The default is 12 months of 30 days
 * named after the old Japanese months, three per season starting with spring, 360 days a year and 90 days per season.
 */
public final class CalendarSpec {
    public static final int MINUTES_PER_HOUR = 60, HOURS_PER_DAY = 24, MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY;

    public static final List<String> DEFAULT_MONTHS = List.of(
            "Mutsuki:30:SPRING", "Kisaragi:30:SPRING", "Yayoi:30:SPRING",
            "Uzuki:30:SUMMER", "Satsuki:30:SUMMER", "Minazuki:30:SUMMER",
            "Fumizuki:30:AUTUMN", "Hazuki:30:AUTUMN", "Nagatsuki:30:AUTUMN",
            "Kannazuki:30:WINTER", "Shimotsuki:30:WINTER", "Shiwasu:30:WINTER");
    public static final List<String> DEFAULT_WEEKDAYS = List.of("Nichiyō", "Getsuyō", "Kayō", "Suiyō", "Mokuyō", "Kinyō", "Doyō");
    public static final List<String> DEFAULT_PHASES = List.of(
            "MIDNIGHT:00:00", "LATE_NIGHT:02:00", "DAWN:05:00", "MORNING:07:00", "NOON:11:30", "AFTERNOON:13:30", "SUNSET:17:30", "NIGHT:19:30");

    private final List<MonthDef> months;
    private final List<String> weekdays;
    private final int epochYear, daysPerYear;
    private final int[] monthStart;        // day-of-year (0-based) where each month begins
    private final int[] phaseStart;        // minute of day where each phase begins, sorted
    private final DayPhase[] phaseOrder;
    private final Map<Season, Integer> seasonDays = new EnumMap<>(Season.class);
    private final List<String> problems = new ArrayList<>();

    public CalendarSpec(List<String> monthLines, List<String> weekdayNames, List<String> phaseLines, int epochYear) {
        List<MonthDef> parsed = new ArrayList<>();
        for (String line : monthLines == null || monthLines.isEmpty() ? DEFAULT_MONTHS : monthLines) {
            String[] p = line.split(":");
            if (p.length < 3) { problems.add("month '" + line + "' is not name:days:season"); continue; }
            try {
                Season season = Season.parse(p[2]).orElseThrow(() -> new IllegalArgumentException("unknown season " + p[2]));
                parsed.add(new MonthDef(p[0], Integer.parseInt(p[1].trim()), season));
            } catch (RuntimeException e) { problems.add("month '" + line + "': " + e.getMessage()); }
        }
        if (parsed.isEmpty()) for (String line : DEFAULT_MONTHS) { String[] p = line.split(":"); parsed.add(new MonthDef(p[0], Integer.parseInt(p[1]), Season.valueOf(p[2]))); }
        this.months = List.copyOf(parsed);
        this.weekdays = List.copyOf(weekdayNames == null || weekdayNames.isEmpty() ? DEFAULT_WEEKDAYS : weekdayNames);
        this.epochYear = epochYear;
        this.monthStart = new int[months.size()];
        int total = 0;
        for (int i = 0; i < months.size(); i++) { monthStart[i] = total; total += months.get(i).days(); seasonDays.merge(months.get(i).season(), months.get(i).days(), Integer::sum); }
        this.daysPerYear = total;

        Map<DayPhase, Integer> starts = new EnumMap<>(DayPhase.class);
        for (String line : phaseLines == null || phaseLines.isEmpty() ? DEFAULT_PHASES : phaseLines) {
            String[] p = line.split(":");
            if (p.length < 3) { problems.add("phase '" + line + "' is not PHASE:HH:MM"); continue; }
            var phase = DayPhase.parse(p[0]);
            if (phase.isEmpty()) { problems.add("unknown phase " + p[0]); continue; }
            try { starts.put(phase.get(), Math.floorMod(Integer.parseInt(p[1].trim()) * 60 + Integer.parseInt(p[2].trim()), MINUTES_PER_DAY)); }
            catch (NumberFormatException e) { problems.add("phase '" + line + "' has a bad time"); }
        }
        if (starts.isEmpty()) for (String line : DEFAULT_PHASES) { String[] p = line.split(":"); starts.put(DayPhase.valueOf(p[0]), Integer.parseInt(p[1]) * 60 + Integer.parseInt(p[2])); }
        List<Map.Entry<DayPhase, Integer>> sorted = new ArrayList<>(starts.entrySet());
        sorted.sort(Map.Entry.comparingByValue());
        this.phaseStart = new int[sorted.size()];
        this.phaseOrder = new DayPhase[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) { phaseStart[i] = sorted.get(i).getValue(); phaseOrder[i] = sorted.get(i).getKey(); }
    }

    public static CalendarSpec defaults() { return new CalendarSpec(DEFAULT_MONTHS, DEFAULT_WEEKDAYS, DEFAULT_PHASES, 100); }

    public List<MonthDef> months() { return months; }
    public List<String> weekdays() { return weekdays; }
    public int epochYear() { return epochYear; }
    public int daysPerYear() { return daysPerYear; }
    public int monthsPerYear() { return months.size(); }
    public List<String> problems() { return List.copyOf(problems); }
    public int daysInSeason(Season season) { return seasonDays.getOrDefault(season, 0); }
    public MonthDef month(int oneBased) { return months.get(Math.floorMod(oneBased - 1, months.size())); }

    /** The phase of the day at a minute of the day (0..1439). Before the first boundary the last phase of the previous day still runs. */
    public DayPhase phaseAt(int minuteOfDay) {
        int found = phaseStart.length - 1;
        for (int i = 0; i < phaseStart.length; i++) if (phaseStart[i] <= minuteOfDay) found = i;
        return phaseOrder[found];
    }

    public int phaseStartMinute(DayPhase phase) {
        for (int i = 0; i < phaseOrder.length; i++) if (phaseOrder[i] == phase) return phaseStart[i];
        return -1;
    }

    public CalendarDate date(long minute) {
        long dayIndex = Math.floorDiv(minute, (long) MINUTES_PER_DAY);
        int minuteOfDay = (int) Math.floorMod(minute, (long) MINUTES_PER_DAY);
        long yearOffset = Math.floorDiv(dayIndex, (long) daysPerYear);
        int dayOfYear0 = (int) Math.floorMod(dayIndex, (long) daysPerYear);
        int m = 0;
        while (m + 1 < months.size() && monthStart[m + 1] <= dayOfYear0) m++;
        MonthDef month = months.get(m);
        int dayOfMonth = dayOfYear0 - monthStart[m] + 1;
        int weekday = (int) Math.floorMod(dayIndex, (long) weekdays.size());
        Season season = month.season();
        int dayOfSeason = dayOfSeason(m, dayOfMonth);
        return new CalendarDate(minute, dayIndex, (int) (epochYear + yearOffset), m + 1, month.name(), dayOfMonth, dayOfYear0 + 1, weekday, weekdays.get(weekday),
                minuteOfDay / 60, minuteOfDay % 60, season, dayOfSeason, phaseAt(minuteOfDay));
    }

    /** 1-based day inside the current run of consecutive months with the same season (wraps round the year). */
    private int dayOfSeason(int monthIndex, int dayOfMonth) {
        Season season = months.get(monthIndex).season();
        int days = dayOfMonth;
        int i = monthIndex;
        for (int guard = 0; guard < months.size() - 1; guard++) {
            i = Math.floorMod(i - 1, months.size());
            if (months.get(i).season() != season) break;
            days += months.get(i).days();
        }
        return days;
    }

    /** The absolute minute of a calendar date (month and day 1-based; out-of-range values are clamped into the calendar). */
    public long minuteOf(int year, int month, int day, int hour, int minute) {
        int m = Math.max(1, Math.min(months.size(), month));
        int d = Math.max(1, Math.min(months.get(m - 1).days(), day));
        long dayIndex = (long) (year - epochYear) * daysPerYear + monthStart[m - 1] + (d - 1);
        return dayIndex * MINUTES_PER_DAY + Math.max(0, Math.min(23, hour)) * 60L + Math.max(0, Math.min(59, minute));
    }

    /** The absolute minute when the given day index begins. */
    public long startOfDay(long dayIndex) { return dayIndex * MINUTES_PER_DAY; }
}
