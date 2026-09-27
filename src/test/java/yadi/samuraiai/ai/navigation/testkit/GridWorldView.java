package yadi.samuraiai.ai.navigation.testkit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import yadi.samuraiai.ai.navigation.graph.NavEnvironment;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.graph.NavWorldView;
import yadi.samuraiai.ai.navigation.terrain.BlockProfile;

/** Sparse in-memory world for navigation tests: solid ground up to {@code floorY}, air above, edits on top. */
public final class GridWorldView implements NavWorldView {
    private final Map<NavPos, BlockProfile> edits = new HashMap<>();
    private final Set<Long> unloaded = new HashSet<>();
    private final int floorY;
    private final int radiusChunks;
    private long tick;
    private NavEnvironment environment = NavEnvironment.CLEAR_DAY;

    /** Standing nodes are at {@code floorY + 1}. */
    public GridWorldView(int floorY, int radiusChunks) { this.floorY = floorY; this.radiusChunks = radiusChunks; }
    public static GridWorldView flat() { return new GridWorldView(63, 6); }

    public int standY() { return floorY + 1; }
    public GridWorldView set(int x, int y, int z, BlockProfile profile) { edits.put(new NavPos(x, y, z), profile); return this; }
    public GridWorldView fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockProfile profile) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, profile);
        return this;
    }
    public GridWorldView wall(int x, int z1, int z2, int height) { return fill(x, standY(), z1, x, standY() + height - 1, z2, BlockProfile.SOLID); }
    public GridWorldView unload(int chunkX, int chunkZ) { unloaded.add(NavPos.chunkKey(chunkX, chunkZ)); return this; }
    public GridWorldView load(int chunkX, int chunkZ) { unloaded.remove(NavPos.chunkKey(chunkX, chunkZ)); return this; }
    public GridWorldView environment(NavEnvironment value) { environment = value; return this; }
    public void advance(long ticks) { tick += ticks; }

    @Override public String dimension() { return "test:world"; }
    @Override public int minY() { return 0; }
    @Override public int maxY() { return 256; }
    @Override public boolean isLoaded(int chunkX, int chunkZ) {
        return Math.abs(chunkX) <= radiusChunks && Math.abs(chunkZ) <= radiusChunks && !unloaded.contains(NavPos.chunkKey(chunkX, chunkZ));
    }
    @Override public BlockProfile profile(int x, int y, int z) {
        if (!isLoaded(x >> 4, z >> 4)) return BlockProfile.UNLOADED;
        BlockProfile edit = edits.get(new NavPos(x, y, z));
        if (edit != null) return edit;
        return y <= floorY ? BlockProfile.SOLID : BlockProfile.AIR;
    }
    @Override public NavEnvironment environment(NavPos at) { return environment; }
    @Override public long gameTick() { return tick; }
}
