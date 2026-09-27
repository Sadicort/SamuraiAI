package yadi.samuraiai.ai.scheduler.time;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.util.Segments;

/**
 * The world event calendar. Lines look like {@code market;every=5;offset=0;periods=MORNING,AFTERNOON;bias=MERCHANT:40,SOCIAL:30}.
 * With no configured lines the built-in catalogue (a market day and a festival) applies; that catalogue is also written as
 * lines, so it is plain data an operator can copy and change.
 */
public final class WorldCalendar {
    public static final List<String> DEFAULT_LINES = List.of(
            "market_day;every=5;offset=0;periods=MORNING,AFTERNOON;bias=MERCHANT:40,SOCIAL:25,WORK:-10",
            "festival;every=20;offset=10;periods=EVENING,NIGHT;bias=SOCIAL:45,PRAYER:20,PATROL:-10,TRAINING:-20");

    private final List<CalendarEvent> events;
    private final List<String> problems;

    private WorldCalendar(List<CalendarEvent> events, List<String> problems) { this.events = List.copyOf(events); this.problems = List.copyOf(problems); }

    public static WorldCalendar parse(List<String> lines) {
        List<String> source = lines == null || lines.isEmpty() ? DEFAULT_LINES : lines;
        List<CalendarEvent> events = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (String line : source) {
            var parsed = Segments.parse(line);
            if (parsed.isEmpty()) { problems.add("calendar line '" + line + "' has no name"); continue; }
            Segments seg = parsed.get();
            List<DayPeriod> periods = new ArrayList<>();
            for (String name : seg.list("periods")) {
                try { periods.add(DayPeriod.valueOf(name.toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { problems.add(seg.id() + ": unknown period " + name); }
            }
            Map<RoutineType, Double> bias = new EnumMap<>(RoutineType.class);
            seg.weights("bias").forEach((name, value) -> {
                try { bias.put(RoutineType.valueOf(name), value); } catch (IllegalArgumentException e) { problems.add(seg.id() + ": unknown routine " + name); }
            });
            events.add(new CalendarEvent(seg.id(), seg.integer("every", 1), seg.integer("offset", 0), periods.isEmpty() ? null : java.util.EnumSet.copyOf(periods), bias));
            problems.addAll(seg.problems().stream().map(p -> seg.id() + ": " + p).toList());
        }
        return new WorldCalendar(events, problems);
    }

    public List<CalendarEvent> events() { return events; }
    public List<String> problems() { return problems; }

    public List<CalendarEvent> activeAt(long dayNumber, DayPeriod period) {
        List<CalendarEvent> active = new ArrayList<>();
        for (CalendarEvent event : events) if (event.activeOn(dayNumber, period)) active.add(event);
        return active;
    }

    /** Summed bias of every event active now for one routine (may be negative). */
    public double bias(List<CalendarEvent> active, RoutineType routine) {
        double total = 0;
        for (CalendarEvent event : active) total += event.bias().getOrDefault(routine, 0.0D);
        return total;
    }
}
