package yadi.samuraiai.ai.navigation.terrain;

import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.graph.NavWorldView;

/** Turns nearby blocks into a static 0..cap danger score: lava, fire, cactus, drops and void. */
public final class HazardAnalyzer {
    public static final int DROP_SCAN_LIMIT = 12;
    private static final int[][] CARDINAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private final NavWorldView view;
    private final double cap;

    public HazardAnalyzer(NavWorldView view, double cap) { this.view = view; this.cap = cap; }

    /** Blocks between the cell below {@code feet} and the first supporting block, capped at {@link #DROP_SCAN_LIMIT}. */
    public int dropBelow(int x, int feetY, int z) {
        int depth = 0;
        for (int y = feetY - 1; depth < DROP_SCAN_LIMIT; y--, depth++) {
            if (y < view.minY()) return DROP_SCAN_LIMIT;
            BlockProfile block = view.profile(x, y, z);
            if (!block.loaded()) return 0;
            if (block.canSupport() || block.isLava() || block.isWater()) return depth;
        }
        return depth;
    }

    public double staticDanger(NavPos pos) {
        double danger = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 1; dy++) {
            if (dx == 0 && dy == 0 && dz == 0) continue;
            BlockProfile block = view.profile(pos.x() + dx, pos.y() + dy, pos.z() + dz);
            boolean side = dy >= 0 && (dx == 0 || dz == 0);
            if (block.isLava()) danger += dy < 0 && dx == 0 && dz == 0 ? 70 : side ? 60 : 25;
            else if (block.material() == Material.FIRE) danger += side ? 40 : 20;
            else if (block.material() == Material.DAMAGING && dy >= 0) danger += side ? 25 : 10;
        }
        for (int[] d : CARDINAL) {
            int drop = dropBelow(pos.x() + d[0], pos.y(), pos.z() + d[1]);
            if (drop >= DROP_SCAN_LIMIT) danger += 45;
            else if (drop >= 6) danger += 25;
            else if (drop >= 3) danger += 10;
        }
        BlockProfile below = view.profile(pos.x(), pos.y() - 1, pos.z());
        if (below.isLava()) danger += 100;
        return Math.min(cap, danger);
    }
}
