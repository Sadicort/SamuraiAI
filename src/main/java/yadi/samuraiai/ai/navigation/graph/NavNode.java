package yadi.samuraiai.ai.navigation.graph;

import yadi.samuraiai.ai.navigation.terrain.Material;
import yadi.samuraiai.ai.navigation.terrain.TerrainType;

/**
 * Cached facts about one standing position. Independent of who is walking: capability-dependent
 * questions (iron doors, swimming) are answered from these facts by the graph.
 *
 * @param standable   a body fits here and has a floor (or ladder / shallow water floor)
 * @param deepWater   feet are in water with no solid floor: only swimmers may stand here
 * @param staticDanger hazard score 0..100 from blocks around this position
 */
public record NavNode(NavPos pos, boolean standable, boolean deepWater, boolean door, boolean ironDoor,
                      boolean climbable, boolean water, TerrainType terrain, Material floor, double staticDanger) {
    public static NavNode unstandable(NavPos pos) {
        return new NavNode(pos, false, false, false, false, false, false, TerrainType.NORMAL, Material.AIR, 0);
    }
}
