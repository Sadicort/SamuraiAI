package yadi.samuraiai.ai.navigation.terrain;

import yadi.samuraiai.ai.navigation.graph.EdgeType;
import yadi.samuraiai.ai.navigation.pathfinding.NavigationPath;

/** Describes how much a path climbs and drops, so behaviors and debug tools can judge effort and risk. */
public final class HeightAnalyzer {
    public record HeightProfile(int totalAscent, int totalDescent, int maxRise, int maxDrop, int jumps, int steps, int ladders, int drops) {
        public boolean flat() { return totalAscent == 0 && totalDescent == 0; }
    }

    private HeightAnalyzer() { }

    public static HeightProfile analyze(NavigationPath path) {
        int ascent = 0, descent = 0, maxRise = 0, maxDrop = 0, jumps = 0, steps = 0, ladders = 0, drops = 0;
        for (int i = 1; i < path.size(); i++) {
            int delta = path.get(i).pos().y() - path.get(i - 1).pos().y();
            if (delta > 0) { ascent += delta; maxRise = Math.max(maxRise, delta); }
            if (delta < 0) { descent -= delta; maxDrop = Math.max(maxDrop, -delta); }
            EdgeType via = path.get(i).via();
            if (via == EdgeType.JUMP) jumps++;
            if (via == EdgeType.STEP) steps++;
            if (via == EdgeType.CLIMB) ladders++;
            if (via == EdgeType.DESCEND) drops++;
        }
        return new HeightProfile(ascent, descent, maxRise, maxDrop, jumps, steps, ladders, drops);
    }
}
