package yadi.samuraiai.living.calendar.festivals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import yadi.samuraiai.living.core.MoonPhase;

/**
 * A recurring festival. It starts on {@code month}/{@code day} and lasts {@code days}; when {@code moon} is set the start moves
 * to the first day within {@code moonWindow} days whose moon is of that family (the Moon Festival waits for the full moon).
 * {@code routineBias} (routine name → points) changes what villagers want to do, {@code demand} (resource id → multiplier)
 * what they buy, {@code social} how lively the village is. {@code cultures} limits it to those cultures (empty = everywhere).
 */
public record FestivalDef(String id, String name, int month, int day, int days, MoonPhase.Family moon, int moonWindow, Map<String, Double> routineBias,
                          Map<String, Double> demand, double social, Set<String> cultures, List<String> tags) {
    public FestivalDef {
        days = Math.max(1, days);
        moonWindow = Math.max(1, moonWindow);
        routineBias = routineBias == null ? Map.of() : Map.copyOf(routineBias);
        demand = demand == null ? Map.of() : Map.copyOf(demand);
        cultures = cultures == null ? Set.of() : Set.copyOf(cultures);
        tags = tags == null ? List.of() : List.copyOf(tags);
        social = Double.isFinite(social) ? Math.max(0.0D, social) : 1.0D;
    }

    public boolean celebratedBy(String culture) { return cultures.isEmpty() || culture != null && cultures.contains(culture); }
}
