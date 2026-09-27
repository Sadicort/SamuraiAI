package yadi.samuraiai.ai.navigation.terrain;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import yadi.samuraiai.ai.navigation.graph.NavNode;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.graph.NavigationGraph;

/**
 * Scans the ground around a point into a {@link TerrainSnapshot}, warming the graph's node cache as a side
 * effect. Results are kept briefly so several consumers asking about the same spot cost one scan.
 */
public final class TerrainScanner {
    private record Key(NavPos center, int radius) { }
    private final NavigationGraph graph;
    private final int ttlTicks;
    private final Map<Key, TerrainSnapshot> recent = new HashMap<>();

    public TerrainScanner(NavigationGraph graph, int ttlTicks) { this.graph = graph; this.ttlTicks = ttlTicks; }

    /** Scans a square of the given radius, looking two blocks up and down for the surface in each column. */
    public TerrainSnapshot scan(NavPos center, int radius, long tick) {
        Key key = new Key(center, radius);
        TerrainSnapshot known = recent.get(key);
        if (known != null && !known.expired(tick, ttlTicks)) return known;
        if (recent.size() > 64) recent.clear();
        EnumMap<TerrainType, Integer> counts = new EnumMap<>(TerrainType.class);
        int standable = 0;
        double total = 0, max = 0;
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (!graph.view().isLoaded((center.x() + dx) >> 4, (center.z() + dz) >> 4)) continue;
            for (int dy = 2; dy >= -2; dy--) {
                NavNode node = graph.node(center.offset(dx, dy, dz));
                if (!node.standable()) continue;
                standable++;
                counts.merge(node.terrain(), 1, Integer::sum);
                total += node.staticDanger();
                max = Math.max(max, node.staticDanger());
                break;
            }
        }
        TerrainSnapshot snapshot = new TerrainSnapshot(graph.view().dimension(), center, radius, counts, standable,
                standable == 0 ? 0 : total / standable, max, tick);
        recent.put(key, snapshot);
        return snapshot;
    }

    public void invalidate() { recent.clear(); }
}
