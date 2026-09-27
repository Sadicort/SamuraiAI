package yadi.samuraiai.ai.navigation.obstacles;

import java.util.UUID;
import yadi.samuraiai.ai.navigation.graph.NavPos;

/** Something in the way. Static obstacles change the graph; temporary ones (entities) are waited out or walked around. */
public record Obstacle(ObstacleType type, NavPos pos, boolean temporary, UUID entityId, int pathIndex) {
    public static Obstacle block(ObstacleType type, NavPos pos, int pathIndex) {
        return new Obstacle(type, pos, false, null, pathIndex);
    }
    public static Obstacle entity(ObstacleType type, NavPos pos, UUID id, int pathIndex) {
        return new Obstacle(type, pos, true, id, pathIndex);
    }
}
