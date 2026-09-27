package yadi.samuraiai.living.village.life;

import java.util.Map;
import java.util.UUID;

/**
 * A village event: what, from when to when, where it came from ({@code source}: {@code world-event:<id>}, {@code festival:<id>},
 * {@code holiday:<id>}, {@code weather}, {@code schedule}) and the routine bias it applies to everyone and to guards.
 */
public record VillageEventRecord(UUID id, VillageEventKind kind, String title, long start, long end, String source, Map<String, Double> bias, Map<String, Double> guardBias) {
    public VillageEventRecord {
        bias = bias == null ? kind.bias() : Map.copyOf(bias);
        guardBias = guardBias == null ? kind.guardBias() : Map.copyOf(guardBias);
        source = source == null ? "" : source;
    }

    public boolean activeAt(long minute) { return minute >= start && minute < end; }
}
