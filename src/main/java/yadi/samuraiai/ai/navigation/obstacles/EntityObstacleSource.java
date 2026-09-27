package yadi.samuraiai.ai.navigation.obstacles;

import java.util.List;
import java.util.UUID;

/** Contract for finding entities near a point; the Minecraft implementation lives in the world adapter package. */
@FunctionalInterface
public interface EntityObstacleSource {
    EntityObstacleSource NONE = (dimension, x, y, z, radius, exclude) -> List.of();
    List<EntityObstacleInfo> near(String dimension, double x, double y, double z, double radius, UUID exclude);
}
