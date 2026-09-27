package yadi.samuraiai.living.calendar.engine;

import java.util.List;
import yadi.samuraiai.living.core.CalendarDate;
import yadi.samuraiai.living.core.MoonPhase;
import yadi.samuraiai.living.core.WeatherKind;

/**
 * The calendar at one instant for one place, as the specification's Calendar Runtime: date and hour, season, weather,
 * temperature, moon, and the festivals and holidays running. Immutable; built on demand.
 */
public record CalendarRuntime(CalendarDate date, String cell, WeatherKind weather, double weatherIntensity, double temperature, MoonPhase moon,
                              List<String> festivals, List<String> holidays, int sunrise, int sunset) {
    public CalendarRuntime {
        festivals = List.copyOf(festivals);
        holidays = List.copyOf(holidays);
    }
}
