package yadi.samuraiai.ai.navigation.movement;

import yadi.samuraiai.ai.navigation.graph.EdgeType;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.pathfinding.NavigationPath;
import yadi.samuraiai.ai.navigation.pathfinding.PathNode;

/**
 * Turns "follow this path" into one steering decision per call: which node is next, where to aim, whether
 * to jump or climb. Uses pure pursuit along straight runs so smoothed paths are followed as lines rather than
 * as a chain of stops, and aims straight at the block centre wherever the terrain demands precision.
 */
public final class PathFollower {
    public static final double LOOKAHEAD = 1.4D;

    public record Step(double aimX, double aimY, double aimZ, boolean jump, int climb, boolean arrived,
                       int targetIndex, boolean offPath, double distanceToTarget, boolean precise) { }

    private int index;

    public int index() { return index; }
    public void reset(int startIndex) { index = Math.max(0, startIndex); }
    public void restart(NavigationPath path, double px, double py, double pz) {
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < path.size(); i++) {
            NavPos n = path.get(i).pos();
            double d = Math.hypot(n.centerX() - px, n.centerZ() - pz) + Math.abs(n.y() - py) * 2;
            if (d < bestDistance) { bestDistance = d; best = i; }
        }
        index = Math.min(path.size() - 1, best + (bestDistance < 1.0D ? 1 : 0));
    }
    /** Steps back by {@code nodes} to retrace ground the walker already covered (recovery backtrack). */
    public void backtrack(int nodes) { index = Math.max(0, index - Math.max(1, nodes)); }

    public Step next(NavigationPath path, double px, double py, double pz, boolean onGround, double reachRadius, double finalRadius) {
        int last = path.size() - 1;
        boolean arrived = false;
        for (int guard = 0; guard <= path.size(); guard++) {
            index = Math.min(index, last);
            PathNode node = path.get(index);
            double radius = index == last ? finalRadius : reachRadius;
            double dx = node.pos().centerX() - px, dz = node.pos().centerZ() - pz;
            double dy = node.pos().y() - py;
            double verticalTolerance = switch (node.via()) {
                case CLIMB, DESCEND -> 0.9D;
                case STEP, JUMP -> 0.6D;
                default -> 1.2D;
            };
            boolean reached = Math.hypot(dx, dz) <= radius && Math.abs(dy) <= verticalTolerance;
            if (!reached && index < last && precise(path.get(index + 1)) == false && overshot(path, index, px, pz)) reached = true;
            if (!reached) break;
            if (index == last) { arrived = true; break; }
            index++;
        }
        PathNode target = path.get(index);
        NavPos pos = target.pos();
        double aimX = pos.centerX(), aimZ = pos.centerZ(), aimY = pos.y();
        boolean precise = precise(target) || index == 0;
        if (!precise && index > 0) {
            NavPos from = path.get(index - 1).pos();
            double sx = from.centerX(), sz = from.centerZ(), ex = pos.centerX(), ez = pos.centerZ();
            double length = Math.hypot(ex - sx, ez - sz);
            if (length > 1.0E-6D) {
                double t = ((px - sx) * (ex - sx) + (pz - sz) * (ez - sz)) / (length * length);
                t = Math.max(0.0D, Math.min(1.0D, t + LOOKAHEAD / length));
                aimX = sx + (ex - sx) * t;
                aimZ = sz + (ez - sz) * t;
            }
        }
        double distance = Math.hypot(pos.centerX() - px, pos.centerZ() - pz);
        boolean jump = false;
        int climb = 0;
        if (onGround && distance <= 1.8D) {
            if (target.via() == EdgeType.JUMP && py < pos.y() - 0.3D) jump = true;
            else if (pos.y() > py + 0.6D && target.via() != EdgeType.CLIMB && target.via() != EdgeType.STEP) jump = true;
        }
        if (target.via() == EdgeType.CLIMB) climb = pos.y() > py + 0.2D ? 1 : (pos.y() < py - 0.2D ? -1 : 0);
        return new Step(aimX, aimY, aimZ, jump, climb, arrived, index, offPath(path, px, pz), distance, precise);
    }

    /** Nodes where the walker must hit the block centre: steps, drops, doors, ladders, water, bridges. */
    private static boolean precise(PathNode node) {
        EdgeType via = node.via();
        return via != EdgeType.WALK && via != EdgeType.DIAGONAL;
    }

    /** True when the walker has already passed the current node along the segment toward the next one. */
    private boolean overshot(NavigationPath path, int i, double px, double pz) {
        NavPos a = path.get(i).pos(), b = path.get(i + 1).pos();
        double abx = b.centerX() - a.centerX(), abz = b.centerZ() - a.centerZ();
        double length = Math.hypot(abx, abz);
        if (length < 1.0E-6D) return false;
        double along = ((px - a.centerX()) * abx + (pz - a.centerZ()) * abz) / length;
        double across = Math.abs((px - a.centerX()) * abz - (pz - a.centerZ()) * abx) / length;
        return along > 0.0D && across < 1.5D && Math.hypot(px - a.centerX(), pz - a.centerZ()) < 2.5D;
    }

    private boolean offPath(NavigationPath path, double px, double pz) {
        NavPos to = path.get(index).pos();
        NavPos from = index == 0 ? to : path.get(index - 1).pos();
        double sx = from.centerX(), sz = from.centerZ(), ex = to.centerX(), ez = to.centerZ();
        double lengthSquared = (ex - sx) * (ex - sx) + (ez - sz) * (ez - sz);
        double t = lengthSquared < 1.0E-9D ? 0 : Math.max(0, Math.min(1, ((px - sx) * (ex - sx) + (pz - sz) * (ez - sz)) / lengthSquared));
        return Math.hypot(px - (sx + (ex - sx) * t), pz - (sz + (ez - sz) * t)) > 3.5D;
    }
}
