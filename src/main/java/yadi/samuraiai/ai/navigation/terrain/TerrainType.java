package yadi.samuraiai.ai.navigation.terrain;

/** Classification of a standing node. Each value carries a configurable cost multiplier (see {@link TerrainCostTable}). */
public enum TerrainType {
    SAFE, NORMAL, ROUGH, WATER, LAVA, CLIFF, STAIRS, LADDER, BRIDGE, DOOR, FOREST, VILLAGE, CUSTOM
}
