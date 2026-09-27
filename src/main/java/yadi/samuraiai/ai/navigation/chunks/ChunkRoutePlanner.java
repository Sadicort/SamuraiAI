package yadi.samuraiai.ai.navigation.chunks;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.graph.NavWorldView;

/**
 * Coarse chunk-level view of a journey. It does not route; it tells the engine whether the corridor to the
 * goal is loaded and where the ground stops existing, so the engine can wait, replan or fail deliberately.
 */
public final class ChunkRoutePlanner {
    private final NavWorldView view;

    public ChunkRoutePlanner(NavWorldView view) { this.view = view; }

    public ChunkRoute route(NavPos start, NavPos goal) {
        List<long[]> chunks = new ArrayList<>();
        int first = -1;
        double dx = goal.x() - start.x(), dz = goal.z() - start.z();
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dz)) / 8.0D));
        long lastKey = Long.MIN_VALUE;
        for (int i = 0; i <= steps; i++) {
            int x = (int) Math.floor(start.x() + dx * i / steps), z = (int) Math.floor(start.z() + dz * i / steps);
            long key = NavPos.chunkKey(x >> 4, z >> 4);
            if (key == lastKey) continue;
            lastKey = key;
            chunks.add(new long[]{x >> 4, z >> 4});
            if (first < 0 && !view.isLoaded(x >> 4, z >> 4)) first = chunks.size() - 1;
        }
        return new ChunkRoute(chunks, first);
    }
}
