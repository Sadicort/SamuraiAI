package yadi.samuraiai.living.calendar.weather;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import yadi.samuraiai.living.calendar.seasons.SeasonProfile;
import yadi.samuraiai.living.core.Dice;
import yadi.samuraiai.living.core.WeatherKind;

/**
 * Persistent, regional weather. Each tracked cell (a region, or {@code world}) holds its own {@link WeatherRuntime}; a cell is
 * only looked at when its next change is due (a priority queue, not a scan), so thousands of cells cost nothing between
 * changes. The next weather is drawn from the season's weights scaled by the cell's microclimate, with a chance to persist;
 * rain falls as snow when it is cold and snow as rain when it is warm. Draws are deterministic ({@link Dice}), so a world
 * caught up after a restart has the same weather it would have had.
 *
 * <p>A cell far behind (a region asleep for weeks) is not stepped change by change: after {@code maxSteps} changes it jumps to
 * the present with one draw, which is statistically the same.
 */
public final class WeatherEngine {
    /** What the engine needs from the calendar to draw the weather at an instant. */
    public interface Conditions {
        SeasonProfile seasonAt(long minute);
        double temperatureAt(WeatherRuntime cell, long minute, WeatherKind weather);
    }

    public record Change(String key, WeatherKind from, WeatherKind to, double intensity, long minute) { }

    private final Map<String, WeatherRuntime> cells = new LinkedHashMap<>();
    private final PriorityQueue<WeatherRuntime> due = new PriorityQueue<>((a, b) -> Long.compare(a.nextChange(), b.nextChange()));
    private ClimateTable climates = ClimateTable.defaults();
    private Dice dice = new Dice(0L);
    private int minHours = 3, maxHours = 18, maxSteps = 200;
    private double persistence = 0.35D;
    private long fastForwards;

    public void configure(ClimateTable climates, Dice dice, int minHours, int maxHours, double persistence, int maxSteps) {
        this.climates = climates; this.dice = dice;
        this.minHours = Math.max(1, minHours); this.maxHours = Math.max(this.minHours, maxHours);
        this.persistence = Math.max(0.0D, Math.min(0.95D, persistence)); this.maxSteps = Math.max(1, maxSteps);
    }

    public ClimateTable climates() { return climates; }
    public long fastForwards() { return fastForwards; }

    /** Starts tracking a cell (or updates its climate and altitude). Returns the runtime. */
    public WeatherRuntime track(String key, String climate, double altitude, long now) {
        WeatherRuntime cell = cells.get(key);
        if (cell == null) {
            cell = new WeatherRuntime(key, climates.has(climate) ? climate : "temperate", altitude, now);
            cells.put(key, cell);
            due.add(cell);
        } else {
            cell.climate(climates.has(climate) ? climate : cell.climate());
            cell.altitude(altitude);
        }
        return cell;
    }

    public Optional<WeatherRuntime> cell(String key) { return Optional.ofNullable(cells.get(key)); }
    public Collection<WeatherRuntime> cells() { return List.copyOf(cells.values()); }
    public int size() { return cells.size(); }
    public void forget(String key) { WeatherRuntime c = cells.remove(key); if (c != null) due.remove(c); }

    /** Draws every change due up to {@code now}; returns the changes (the caller publishes them). */
    public List<Change> stepTo(long now, Conditions conditions) {
        List<Change> out = new ArrayList<>();
        while (!due.isEmpty() && due.peek().nextChange() <= now) {
            WeatherRuntime cell = due.poll();
            int steps = 0;
            WeatherKind before = cell.current();
            while (cell.nextChange() <= now) {
                if (steps >= maxSteps) {
                    // far behind: jump to the present with a single draw
                    fastForwards++;
                    draw(cell, now, conditions);
                    break;
                }
                draw(cell, cell.nextChange(), conditions);
                steps++;
            }
            cell.account(now);
            due.add(cell);
            if (cell.current() != before) out.add(new Change(cell.key(), before, cell.current(), cell.intensity(), now));
        }
        return out;
    }

    private void draw(WeatherRuntime cell, long at, Conditions conditions) {
        SeasonProfile season = conditions.seasonAt(at);
        ClimateProfile climate = climates.of(cell.climate());
        String key = "weather:" + cell.key();
        long step = cell.changes();
        WeatherKind next;
        if (cell.changes() > 0 && dice.chance(key + ":keep", step, persistence)) next = cell.current();
        else {
            WeatherKind[] kinds = WeatherKind.values();
            double[] weights = new double[kinds.length];
            for (int i = 0; i < kinds.length; i++) weights[i] = season.weatherWeight(kinds[i]) * climate.scale(kinds[i]);
            int pick = dice.weighted(key, step, weights);
            next = pick < 0 ? WeatherKind.SUNNY : kinds[pick];
        }
        double temperature = conditions.temperatureAt(cell, at, next);
        if (next == WeatherKind.RAIN && temperature < 1.0D) next = WeatherKind.SNOW;
        else if (next == WeatherKind.SNOW && temperature > 3.0D) next = WeatherKind.RAIN;
        double hours = dice.between(key + ":len", step, minHours, maxHours + 1);
        if (next == WeatherKind.STORM || next == WeatherKind.HAIL) hours *= 0.5D;
        long length = Math.max(30L, Math.round(hours * 60.0D));
        cell.set(next, dice.between(key + ":int", step, 0.3D, 1.0D), at, at + length);
    }

    /** Forces the weather of a cell (commands, world events such as a storm). It lasts {@code minutes}. */
    public Change force(String key, WeatherKind kind, double intensity, long now, long minutes) {
        WeatherRuntime cell = track(key, "temperate", 64, now);
        WeatherKind before = cell.current();
        due.remove(cell);
        cell.set(kind, Math.max(0.0D, Math.min(1.0D, intensity)), now, now + Math.max(1L, minutes));
        due.add(cell);
        return new Change(key, before, kind, cell.intensity(), now);
    }

    /** Day boundary: each cell closes its daily tally. */
    public void rollDay(long dayStart) { for (WeatherRuntime cell : cells.values()) cell.rollDay(dayStart); }

    /** Re-inserts a restored cell. */
    public void restore(WeatherRuntime cell) {
        WeatherRuntime old = cells.put(cell.key(), cell);
        if (old != null) due.remove(old);
        due.add(cell);
    }

    public void clear() { cells.clear(); due.clear(); fastForwards = 0; }
}
