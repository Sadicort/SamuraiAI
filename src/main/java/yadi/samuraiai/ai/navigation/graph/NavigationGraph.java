package yadi.samuraiai.ai.navigation.graph;

import java.util.HashMap;
import java.util.Map;
import yadi.samuraiai.ai.navigation.planner.PathPreferences;
import yadi.samuraiai.ai.navigation.terrain.*;

/**
 * Lazy, dynamic navigation graph over a {@link NavWorldView}. Nodes are computed on demand and cached
 * per chunk; a world change invalidates the affected chunk (and its neighbours when the change touches
 * a border) so the next expansion sees the new world. Edges are generated from cached node facts.
 */
public final class NavigationGraph {
    private static final int[][] CARDINAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] DIAGONAL = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private final NavWorldView view;
    private final NodeAnalyzer analyzer;
    private final int nodeCacheMax;
    private final Map<Long, Map<NavPos, NavNode>> chunks = new HashMap<>();
    private int cached;
    private long version, hits, misses, invalidations;

    public NavigationGraph(NavWorldView view, double dangerCap, int nodeCacheMax, TerrainOverrides overrides) {
        this.view = view;
        this.analyzer = new NodeAnalyzer(view, new HazardAnalyzer(view, dangerCap), overrides == null ? TerrainOverrides.NONE : overrides);
        this.nodeCacheMax = nodeCacheMax;
    }

    public NavWorldView view() { return view; }
    public long version() { return version; }
    public int cachedNodes() { return cached; }
    public long cacheHits() { return hits; }
    public long cacheMisses() { return misses; }
    public long invalidations() { return invalidations; }

    public NavNode node(NavPos pos) {
        Map<NavPos, NavNode> chunk = chunks.get(pos.chunkKey());
        if (chunk != null) {
            NavNode known = chunk.get(pos);
            if (known != null) { hits++; return known; }
        }
        misses++;
        if (!view.isLoaded(pos.chunkX(), pos.chunkZ())) return NavNode.unstandable(pos);
        if (cached >= nodeCacheMax) clear();
        NavNode fresh = analyzer.analyze(pos);
        chunks.computeIfAbsent(pos.chunkKey(), key -> new HashMap<>()).put(pos, fresh);
        cached++;
        return fresh;
    }

    /** Whether this walker may stand on the node, given door and swimming capabilities. */
    public boolean standable(NavPos pos, PathPreferences prefs) {
        NavNode node = node(pos);
        if (!node.standable()) return false;
        if (node.deepWater() && !prefs.allowSwim()) return false;
        if (node.water() && !node.deepWater() && !prefs.allowWade() && !prefs.allowSwim()) return false;
        if (node.ironDoor()) return prefs.openIronDoors();
        if (node.door()) return prefs.openWoodDoors();
        return true;
    }

    /**
     * Standability checked against the live world for a cell a walker is about to cross. A cheap read decides
     * whether the cached node is stale; only then is the node recomputed.
     */
    public boolean liveStandable(NavPos pos, PathPreferences prefs) {
        boolean live = analyzer.quickStandable(pos);
        Map<NavPos, NavNode> chunk = chunks.get(pos.chunkKey());
        NavNode cachedNode = chunk == null ? null : chunk.get(pos);
        if (cachedNode != null && cachedNode.standable() != live) refresh(pos);
        else if (!live) return false;
        return standable(pos, prefs);
    }

    /**
     * Whether a walker centred at (x, z) on level y fits: all four cells under its footprint corners are
     * standable. This is what stops smoothed paths from clipping the corner of a wall.
     */
    public boolean footprintStandable(double x, double z, int y, PathPreferences prefs, boolean live) {
        for (int i = 0; i < 4; i++) {
            NavPos cell = new NavPos((int) Math.floor(x + ((i & 1) == 0 ? -prefs.bodyRadius() : prefs.bodyRadius())), y,
                    (int) Math.floor(z + ((i & 2) == 0 ? -prefs.bodyRadius() : prefs.bodyRadius())));
            if (!(live ? liveStandable(cell, prefs) : standable(cell, prefs))) return false;
        }
        return true;
    }

    /** Emits every legal move out of {@code from} for a walker with these preferences. */
    public void neighbors(NavPos from, PathPreferences prefs, EdgeSink sink) {
        NavNode source = node(from);
        if (!source.standable()) return;
        for (int[] d : CARDINAL) {
            NavPos flat = from.offset(d[0], 0, d[1]);
            if (standable(flat, prefs)) {
                sink.accept(flat, flatType(node(flat), false), flatType(node(flat), false).baseCost());
                continue;
            }
            if (!view.isLoaded(flat.chunkX(), flat.chunkZ())) continue;
            NavPos up = flat.offset(0, 1, 0);
            if (canJumpOver(from, flat) && standable(up, prefs)) {
                NavNode target = node(up);
                boolean stairs = target.floor() == Material.STAIRS || target.floor() == Material.SLAB || target.terrain() == TerrainType.STAIRS;
                EdgeType type = target.door() ? EdgeType.DOOR : stairs ? EdgeType.STEP : EdgeType.JUMP;
                sink.accept(up, type, type.baseCost());
                continue;
            }
            descend(from, flat, prefs, sink);
        }
        for (int[] d : DIAGONAL) {
            NavPos target = from.offset(d[0], 0, d[1]);
            if (!standable(target, prefs) || node(target).door()) continue;
            if (!columnClear(from.x() + d[0], from.y(), from.z()) || !columnClear(from.x(), from.y(), from.z() + d[1])) continue;
            sink.accept(target, EdgeType.DIAGONAL, EdgeType.DIAGONAL.baseCost());
        }
        if (source.climbable()) {
            NavPos up = from.offset(0, 1, 0), down = from.offset(0, -1, 0);
            if (standable(up, prefs) && node(up).climbable()) sink.accept(up, EdgeType.CLIMB, EdgeType.CLIMB.baseCost());
            if (standable(down, prefs) && (node(down).climbable() || !view.profile(down).blocksMovement()))
                sink.accept(down, EdgeType.CLIMB, EdgeType.CLIMB.baseCost());
        }
    }

    private EdgeType flatType(NavNode target, boolean diagonal) {
        if (target.door()) return EdgeType.DOOR;
        if (target.terrain() == TerrainType.BRIDGE) return EdgeType.BRIDGE;
        if (target.water()) return target.deepWater() ? EdgeType.SWIM : EdgeType.WADE;
        return diagonal ? EdgeType.DIAGONAL : EdgeType.WALK;
    }

    private boolean columnClear(int x, int y, int z) {
        return view.profile(x, y, z).isOccupiable() && view.profile(x, y + 1, z).isOccupiable();
    }

    /** A one-block rise is only possible over a full-height but not fence-tall block, with room to jump. */
    private boolean canJumpOver(NavPos from, NavPos flat) {
        BlockProfile ledge = view.profile(flat);
        if (!ledge.loaded() || ledge.material() == Material.FENCE || ledge.topHeight() > 1.01D || !ledge.blocksMovement()) return false;
        return view.profile(from.x(), from.y() + 2, from.z()).isOccupiable();
    }

    private void descend(NavPos from, NavPos flat, PathPreferences prefs, EdgeSink sink) {
        if (!view.profile(flat).isOccupiable() || !view.profile(flat.x(), flat.y() + 1, flat.z()).isOccupiable()) return;
        for (int k = 1; k <= prefs.maxDrop(); k++) {
            NavPos landing = flat.offset(0, -k, 0);
            if (standable(landing, prefs)) {
                sink.accept(landing, EdgeType.DESCEND, EdgeType.DESCEND.baseCost() + 0.5D * (k - 1) + (k >= 3 ? 1.0D : 0.0D));
                return;
            }
            if (!view.profile(landing).isOccupiable()) return;
        }
    }

    /**
     * Recomputes one node from the live world and replaces the cached copy. Used to double-check the few
     * nodes a walker is about to step on, catching changes no world event announced (pistons, doors).
     */
    public NavNode refresh(NavPos pos) {
        if (!view.isLoaded(pos.chunkX(), pos.chunkZ())) return NavNode.unstandable(pos);
        NavNode fresh = analyzer.analyze(pos);
        Map<NavPos, NavNode> chunk = chunks.computeIfAbsent(pos.chunkKey(), key -> new HashMap<>());
        if (chunk.put(pos, fresh) == null) cached++;
        return fresh;
    }

    /** Called when a block changed. Drops cached nodes that could depend on it. */
    public void invalidate(NavPos changed) {
        version++; invalidations++;
        drop(changed.chunkX(), changed.chunkZ());
        int lx = changed.x() & 15, lz = changed.z() & 15;
        if (lx <= 1) drop(changed.chunkX() - 1, changed.chunkZ());
        if (lx >= 14) drop(changed.chunkX() + 1, changed.chunkZ());
        if (lz <= 1) drop(changed.chunkX(), changed.chunkZ() - 1);
        if (lz >= 14) drop(changed.chunkX(), changed.chunkZ() + 1);
    }

    public void invalidateChunk(int chunkX, int chunkZ) { version++; invalidations++; drop(chunkX, chunkZ); }

    private void drop(int chunkX, int chunkZ) {
        Map<NavPos, NavNode> removed = chunks.remove(NavPos.chunkKey(chunkX, chunkZ));
        if (removed != null) cached -= removed.size();
    }

    public void clear() { chunks.clear(); cached = 0; version++; }
}
