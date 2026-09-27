package yadi.samuraiai.ai.navigation.obstacles;

public enum ObstacleType {
    BLOCK, WALL, DOOR, WATER, LAVA, CACTUS, FIRE, ENTITY_PLAYER, ENTITY_NPC, ENTITY_MOB, ENTITY_ANIMAL, ITEM;

    public boolean isEntity() { return name().startsWith("ENTITY_") || this == ITEM; }
}
