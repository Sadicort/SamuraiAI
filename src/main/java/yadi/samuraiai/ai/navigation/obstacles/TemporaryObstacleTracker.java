package yadi.samuraiai.ai.navigation.obstacles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers how long entities have been in the way. A player crossing the path is waited out; one that
 * stays put past the wait threshold triggers a local detour instead of a full replan.
 */
public final class TemporaryObstacleTracker {
    private final Map<UUID, Long> firstSeen = new HashMap<>();
    private long blockedSince = -1;

    public ObstacleDecision observe(List<Obstacle> obstacles, long tick, int waitTicks) {
        Map<UUID, Long> current = new HashMap<>();
        for (Obstacle obstacle : obstacles) {
            if (!obstacle.temporary() || obstacle.entityId() == null) continue;
            current.put(obstacle.entityId(), firstSeen.getOrDefault(obstacle.entityId(), tick));
        }
        firstSeen.clear();
        firstSeen.putAll(current);
        if (current.isEmpty()) { blockedSince = -1; return ObstacleDecision.NONE; }
        if (blockedSince < 0) blockedSince = tick;
        return tick - blockedSince >= waitTicks ? ObstacleDecision.DETOUR : ObstacleDecision.WAIT;
    }

    public long waitedTicks(long tick) { return blockedSince < 0 ? 0 : tick - blockedSince; }
    public void reset() { firstSeen.clear(); blockedSince = -1; }
}
