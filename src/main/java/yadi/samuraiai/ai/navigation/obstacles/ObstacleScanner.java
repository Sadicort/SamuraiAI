package yadi.samuraiai.ai.navigation.obstacles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.graph.NavWorldView;
import yadi.samuraiai.ai.navigation.graph.NavigationGraph;
import yadi.samuraiai.ai.navigation.pathfinding.NavigationPath;
import yadi.samuraiai.ai.navigation.planner.PathPreferences;
import yadi.samuraiai.ai.navigation.terrain.BlockProfile;
import yadi.samuraiai.ai.navigation.terrain.Material;

/** Looks a few nodes ahead of the walker for anything that stops it: changed blocks and entities. */
public final class ObstacleScanner {
    private static final double ENTITY_RADIUS = 0.9D, SAMPLE_START = 0.8D, SAMPLE_STEP = 0.8D;
    private final NavigationGraph graph;
    private final EntityObstacleSource entities;

    public ObstacleScanner(NavigationGraph graph, EntityObstacleSource entities) {
        this.graph = graph;
        this.entities = entities == null ? EntityObstacleSource.NONE : entities;
    }

    /** Static obstacles on the next nodes: blocks that made a node stop being standable since the path was planned. */
    public List<Obstacle> staticAhead(NavigationPath path, int fromIndex, int count, PathPreferences prefs) {
        List<Obstacle> found = new ArrayList<>();
        int last = Math.min(path.size() - 1, fromIndex + count);
        for (int i = Math.max(0, fromIndex); i <= last; i++) {
            NavPos pos = path.get(i).pos();
            if (graph.standable(pos, prefs)) continue;
            found.add(Obstacle.block(classify(pos), pos, i));
        }
        return found;
    }

    /**
     * Entities standing on the walker's route within {@code distance} blocks, sampled along the path polyline
     * (not just at nodes, since smoothed paths have long straight segments).
     */
    public List<Obstacle> temporaryAhead(NavigationPath path, int fromIndex, double px, double pz, double distance, UUID self) {
        List<Obstacle> found = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        NavWorldView view = graph.view();
        double cx = px, cz = pz, remaining = distance, traveled = 0;
        for (int i = Math.max(0, fromIndex); i < path.size() && remaining > 0; i++) {
            NavPos node = path.get(i).pos();
            double tx = node.centerX(), tz = node.centerZ();
            double segment = Math.hypot(tx - cx, tz - cz);
            double consumed = Math.min(segment, remaining);
            for (double d = Math.max(0.0D, SAMPLE_START - traveled); d <= consumed && segment > 1.0E-6D; d += SAMPLE_STEP) {
                double sx = cx + (tx - cx) * d / segment, sz = cz + (tz - cz) * d / segment;
                for (EntityObstacleInfo info : entities.near(view.dimension(), sx, node.y() + 0.5D, sz, ENTITY_RADIUS, self)) {
                    if (!seen.add(info.id())) continue;
                    found.add(Obstacle.entity(typeOf(info.kind()), NavPos.ofBlock(sx, node.y(), sz), info.id(), i));
                }
            }
            traveled += consumed; remaining -= consumed;
            cx = tx; cz = tz;
        }
        return found;
    }

    private static ObstacleType typeOf(EntityKind kind) {
        return switch (kind) {
            case PLAYER -> ObstacleType.ENTITY_PLAYER;
            case NPC -> ObstacleType.ENTITY_NPC;
            case HOSTILE, OTHER -> ObstacleType.ENTITY_MOB;
            case ANIMAL -> ObstacleType.ENTITY_ANIMAL;
            case ITEM -> ObstacleType.ITEM;
        };
    }

    private ObstacleType classify(NavPos pos) {
        NavWorldView view = graph.view();
        BlockProfile feet = view.profile(pos), head = view.profile(pos.offset(0, 1, 0)), floor = view.profile(pos.offset(0, -1, 0));
        if (feet.isLava() || floor.isLava()) return ObstacleType.LAVA;
        if (feet.material() == Material.FIRE || head.material() == Material.FIRE) return ObstacleType.FIRE;
        if (feet.material() == Material.DAMAGING || head.material() == Material.DAMAGING) return ObstacleType.CACTUS;
        if (feet.isIronDoor() || head.isIronDoor() || feet.door()) return ObstacleType.DOOR;
        if (feet.isWater()) return ObstacleType.WATER;
        if (feet.isFullBlock() && head.isFullBlock()) return ObstacleType.WALL;
        return ObstacleType.BLOCK;
    }
}
