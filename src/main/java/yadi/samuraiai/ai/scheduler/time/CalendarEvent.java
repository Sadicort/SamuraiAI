package yadi.samuraiai.ai.scheduler.time;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;

/**
 * A recurring world event (a market day, a festival, a heightened watch). It only biases what NPCs feel like doing; nothing
 * about it is hardcoded, it is read from the calendar catalogue.
 */
public record CalendarEvent(String name, int everyDays, int offsetDays, Set<DayPeriod> periods, Map<RoutineType, Double> bias) {
    public CalendarEvent {
        everyDays = Math.max(1, everyDays);
        offsetDays = Math.floorMod(offsetDays, everyDays);
        periods = periods == null || periods.isEmpty() ? EnumSet.allOf(DayPeriod.class) : EnumSet.copyOf(periods);
        bias = bias == null ? Map.of() : Map.copyOf(bias);
    }

    public boolean activeOn(long dayNumber, DayPeriod period) {
        return Math.floorMod(dayNumber - offsetDays, (long) everyDays) == 0 && periods.contains(period);
    }
}
