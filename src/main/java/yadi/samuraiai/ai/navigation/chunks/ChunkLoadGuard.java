package yadi.samuraiai.ai.navigation.chunks;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.graph.NavWorldView;
import yadi.samuraiai.ai.navigation.pathfinding.NavigationPath;

/** Answers "is this ground actually there?" so no NPC is sent into a chunk that does not exist. */
public final class ChunkLoadGuard {
    private final NavWorldView view;

    public ChunkLoadGuard(NavWorldView view) { this.view = view; }

    public boolean loaded(NavPos pos) { return view.isLoaded(pos.chunkX(), pos.chunkZ()); }

    /** Chunk keys of the next {@code count} nodes that are not loaded, in path order. */
    public List<Long> unloadedAhead(NavigationPath path, int fromIndex, int count) {
        List<Long> missing = new ArrayList<>();
        int last = Math.min(path.size() - 1, fromIndex + count);
        for (int i = Math.max(0, fromIndex); i <= last; i++) {
            NavPos pos = path.get(i).pos();
            if (!loaded(pos) && !missing.contains(pos.chunkKey())) missing.add(pos.chunkKey());
        }
        return missing;
    }
}
