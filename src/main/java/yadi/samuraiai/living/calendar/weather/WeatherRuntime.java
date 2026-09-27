package yadi.samuraiai.living.calendar.weather;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;
import yadi.samuraiai.living.core.WeatherKind;

/**
 * The weather over one cell of the world (a region, or the whole world for the {@code world} cell): what it is doing now,
 * since when, when it will next change, how strong it is, what it did recently, and how many minutes of each weather the
 * current and the previous day had (agriculture reads the daily summary).
 */
public final class WeatherRuntime {
    public record Spell(WeatherKind kind, long from, double intensity) { }

    private static final int HISTORY = 32;

    private final String key;
    private String climate;
    private double altitude;
    private WeatherKind current = WeatherKind.SUNNY;
    private double intensity = 0.5D;
    private long since, nextChange;
    private long changes;
    private final Deque<Spell> history = new ArrayDeque<>();
    private final Map<WeatherKind, Long> today = new EnumMap<>(WeatherKind.class);
    private Map<WeatherKind, Long> yesterday = new EnumMap<>(WeatherKind.class);
    private long accountedUntil;

    public WeatherRuntime(String key, String climate, double altitude, long now) {
        this.key = key; this.climate = climate; this.altitude = altitude; this.since = now; this.nextChange = now; this.accountedUntil = now;
    }

    public String key() { return key; }
    public String climate() { return climate; }
    public void climate(String v) { climate = v; }
    public double altitude() { return altitude; }
    public void altitude(double v) { altitude = v; }
    public WeatherKind current() { return current; }
    public double intensity() { return intensity; }
    public long since() { return since; }
    public long nextChange() { return nextChange; }
    public long changes() { return changes; }
    public Deque<Spell> history() { return history; }

    void set(WeatherKind kind, double strength, long at, long next) {
        account(at);
        if (kind != current || history.isEmpty()) {
            history.addLast(new Spell(kind, at, strength));
            while (history.size() > HISTORY) history.removeFirst();
        }
        current = kind; intensity = strength; since = at; nextChange = next; changes++;
    }

    /** Adds the minutes since the last accounting to today's tally of the current weather. */
    void account(long now) {
        if (now > accountedUntil) { today.merge(current, now - accountedUntil, Long::sum); accountedUntil = now; }
    }

    /** Closes the day: today's tally becomes yesterday's summary. */
    void rollDay(long dayStart) {
        account(dayStart);
        yesterday = new EnumMap<>(WeatherKind.class);
        yesterday.putAll(today);
        today.clear();
    }

    public Map<WeatherKind, Long> todayMinutes() { return Map.copyOf(today); }
    public Map<WeatherKind, Long> yesterdayMinutes() { return Map.copyOf(yesterday); }

    /** The weather that lasted longest yesterday (SUNNY when nothing was recorded). */
    public WeatherKind yesterdayDominant() {
        WeatherKind best = WeatherKind.SUNNY;
        long most = -1;
        for (var e : yesterday.entrySet()) if (e.getValue() > most) { most = e.getValue(); best = e.getKey(); }
        return best;
    }

    // persistence
    public void restore(WeatherKind kind, double strength, long sinceMinute, long next, long changeCount, long accounted, Map<WeatherKind, Long> todayTally, Map<WeatherKind, Long> yesterdayTally,
                        Iterable<Spell> spells) {
        current = kind; intensity = strength; since = sinceMinute; nextChange = next; changes = changeCount; accountedUntil = accounted;
        today.clear(); today.putAll(todayTally);
        yesterday = new EnumMap<>(WeatherKind.class); yesterday.putAll(yesterdayTally);
        history.clear(); for (Spell s : spells) history.addLast(s);
    }

    public long accountedUntil() { return accountedUntil; }
}
