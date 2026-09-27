package yadi.samuraiai.ai.navigation.terrain;

import java.util.Optional;
import yadi.samuraiai.ai.navigation.graph.NavPos;

/** Lets zones (village, custom areas) relabel a node's terrain without the classifier knowing about zones. */
@FunctionalInterface
public interface TerrainOverrides {
    TerrainOverrides NONE = (dimension, pos) -> Optional.empty();
    Optional<TerrainType> terrainAt(String dimension, NavPos pos);
}
