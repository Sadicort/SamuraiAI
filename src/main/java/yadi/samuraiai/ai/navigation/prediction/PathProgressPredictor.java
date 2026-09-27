package yadi.samuraiai.ai.navigation.prediction;

import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.pathfinding.NavigationPath;

/**
 * Forecasts a walker along its path: time to arrival and where it will be in N ticks. Other systems (crowd
 * management, escorts, perception's tracking) use the forecast without knowing anything about the walker's body.
 */
public final class PathProgressPredictor {
    private PathProgressPredictor() { }

    /** Remaining path length in blocks from a position to the end, following the path polyline. */
    public static double remainingLength(NavigationPath path, int index, double px, double pz) {
        double total = 0, cx = px, cz = pz;
        for (int i = Math.max(0, index); i < path.size(); i++) {
            NavPos node = path.get(i).pos();
            total += Math.hypot(node.centerX() - cx, node.centerZ() - cz);
            cx = node.centerX(); cz = node.centerZ();
        }
        return total;
    }

    public static long estimateTicks(NavigationPath path, int index, double px, double pz, double blocksPerTick) {
        if (blocksPerTick <= 0) return Long.MAX_VALUE;
        return (long) Math.ceil(remainingLength(path, index, px, pz) / blocksPerTick);
    }

    /** Horizontal position after walking {@code ticks} along the remaining path; the goal once it would be reached. */
    public static double[] positionAfter(NavigationPath path, int index, double px, double pz, long ticks, double blocksPerTick) {
        double remaining = Math.max(0, ticks * blocksPerTick), cx = px, cz = pz;
        for (int i = Math.max(0, index); i < path.size(); i++) {
            NavPos node = path.get(i).pos();
            double tx = node.centerX(), tz = node.centerZ(), length = Math.hypot(tx - cx, tz - cz);
            if (remaining <= length && length > 1.0E-9D) return new double[]{cx + (tx - cx) * remaining / length, cz + (tz - cz) * remaining / length};
            remaining -= length; cx = tx; cz = tz;
        }
        return new double[]{cx, cz};
    }

    /** Will the walker pass within {@code radius} of a moving point in the next {@code horizonTicks}? Used to decide if a mover is a real obstacle. */
    public static boolean willCross(NavigationPath path, int index, double px, double pz, double blocksPerTick,
                                    double ox, double oz, double ovx, double ovz, double radius, int horizonTicks) {
        for (int t = 0; t <= horizonTicks; t += 2) {
            double[] walker = positionAfter(path, index, px, pz, t, blocksPerTick);
            if (Math.hypot(walker[0] - (ox + ovx * t), walker[1] - (oz + ovz * t)) <= radius) return true;
        }
        return false;
    }
}
