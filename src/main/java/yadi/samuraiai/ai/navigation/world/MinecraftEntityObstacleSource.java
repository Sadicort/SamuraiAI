package yadi.samuraiai.ai.navigation.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import yadi.samuraiai.ai.navigation.obstacles.EntityKind;
import yadi.samuraiai.ai.navigation.obstacles.EntityObstacleInfo;
import yadi.samuraiai.ai.navigation.obstacles.EntityObstacleSource;

/**
 * Finds living entities near a point. Queries are bucketed into 4-block cells and cached for the current
 * tick, because many walkers sample points close together and one world query serves them all.
 */
public final class MinecraftEntityObstacleSource implements EntityObstacleSource {
    private static final int CELL = 4;
    private final ServerLevel level;
    private final Supplier<Set<UUID>> npcEntities;
    private final Map<Long, List<EntityObstacleInfo>> cells = new HashMap<>();
    private long cachedTick = Long.MIN_VALUE;

    public MinecraftEntityObstacleSource(ServerLevel level, Supplier<Set<UUID>> npcEntities) {
        this.level = level; this.npcEntities = npcEntities;
    }

    @Override public List<EntityObstacleInfo> near(String dimension, double x, double y, double z, double radius, UUID exclude) {
        long tick = level.getGameTime();
        if (tick != cachedTick) { cells.clear(); cachedTick = tick; }
        int cx = Math.floorDiv((int) Math.floor(x), CELL), cz = Math.floorDiv((int) Math.floor(z), CELL);
        long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL) ^ ((long) Math.floorDiv((int) Math.floor(y), 8) << 48);
        List<EntityObstacleInfo> bucket = cells.computeIfAbsent(key, k -> query(cx, cz, y));
        List<EntityObstacleInfo> result = new ArrayList<>();
        for (EntityObstacleInfo info : bucket) {
            if (info.id().equals(exclude)) continue;
            if (Math.hypot(info.x() - x, info.z() - z) <= radius + info.width() / 2 && Math.abs(info.y() - y) < 2.0D) result.add(info);
        }
        return result;
    }

    private List<EntityObstacleInfo> query(int cellX, int cellZ, double y) {
        double minX = cellX * CELL - 1.5D, minZ = cellZ * CELL - 1.5D;
        AABB box = new AABB(minX, y - 2.0D, minZ, minX + CELL + 3.0D, y + 3.0D, minZ + CELL + 3.0D);
        Set<UUID> npcs = npcEntities.get();
        List<EntityObstacleInfo> found = new ArrayList<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            EntityKind kind = entity instanceof ServerPlayer ? EntityKind.PLAYER
                    : npcs.contains(entity.getUUID()) ? EntityKind.NPC
                    : entity instanceof Enemy ? EntityKind.HOSTILE
                    : entity instanceof Animal ? EntityKind.ANIMAL : EntityKind.OTHER;
            found.add(new EntityObstacleInfo(entity.getUUID(), kind, entity.getX(), entity.getY(), entity.getZ(), entity.getBbWidth()));
        }
        return found;
    }
}
