package yadi.samuraiai.ai.navigation.obstacles;

import java.util.UUID;

/** Minecraft-agnostic description of a living thing standing near a path. */
public record EntityObstacleInfo(UUID id, EntityKind kind, double x, double y, double z, double width) { }
